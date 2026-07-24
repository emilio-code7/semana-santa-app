import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as sqs from 'aws-cdk-lib/aws-sqs';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as cognito from 'aws-cdk-lib/aws-cognito';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as path from 'path';
import { Construct } from 'constructs';

export class RepertorioInfraStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const appName = 'repertorio';

    // ── VPC ──────────────────────────────────────────────────────
    const vpc = ec2.Vpc.fromLookup(this, 'Vpc', { isDefault: true });

    // ── Security Groups ──────────────────────────────────────────
    const sgEc2 = new ec2.SecurityGroup(this, 'SgEc2', {
      vpc,
      description: 'EC2: HTTP from anywhere, SSH from trusted IPs (set manually)',
      allowAllOutbound: true,
    });
    sgEc2.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(80), 'HTTP from anywhere');
    sgEc2.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(22), 'SSH from anywhere (dev only)');

    const sgRds = new ec2.SecurityGroup(this, 'SgRds', {
      vpc,
      description: 'RDS: PostgreSQL from EC2 only',
      allowAllOutbound: false,
    });
    sgRds.addIngressRule(sgEc2, ec2.Port.tcp(5432), 'PostgreSQL from EC2');

    // ── SQS Queues ────────────────────────────────────────────────
    const queues: { name: string; dlq: sqs.Queue; queue: sqs.Queue }[] = [];

    for (const name of ['hermandad-events', 'hermandad-member-events', 'procesion-events', 'marcha-events']) {
      const dlq = new sqs.Queue(this, `Dlq${name.replace(/[-]/g, '')}`, {
        queueName: `${name}-dlq`,
        retentionPeriod: cdk.Duration.days(14),
      });

      const queue = new sqs.Queue(this, `Queue${name.replace(/[-]/g, '')}`, {
        queueName: name,
        visibilityTimeout: cdk.Duration.seconds(60),
        deadLetterQueue: {
          queue: dlq,
          maxReceiveCount: 3,
        },
      });

      queues.push({ name, dlq, queue });
    }

    // ── ECR Repositories ──────────────────────────────────────────
    const services = ['hermandad-service', 'procesion-service', 'repertorio-service'];
    const repos: Record<string, ecr.IRepository> = {};

    for (const svc of services) {
      const repo = new ecr.Repository(this, `Repo${svc.replace(/[-]/g, '')}`, {
        repositoryName: `${appName}/${svc}`,
        imageScanOnPush: true,
        removalPolicy: cdk.RemovalPolicy.DESTROY,
        emptyOnDelete: true,
      });
      repos[svc] = repo;
    }

    // ── IAM Role for EC2 ──────────────────────────────────────────
    const roleEc2 = new iam.Role(this, 'RoleEc2', {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'),
      ],
    });

    // SQS permissions
    for (const { queue, dlq } of queues) {
      queue.grantSendMessages(roleEc2);
      queue.grantConsumeMessages(roleEc2);
      dlq.grantSendMessages(roleEc2);
    }

    // ECR permissions
    for (const svc of services) {
      repos[svc].grantPull(roleEc2);
    }

    // ── Cognito ──────────────────────────────────────────────────
    const preTokenFn = new lambda.Function(this, 'PreTokenFn', {
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'index.handler',
      code: lambda.Code.fromAsset(path.join(__dirname, '..', 'pre-token-lambda')),
      description: 'Inject hermandad_memberships claim from Cognito groups',
    });

    const userPool = new cognito.UserPool(this, 'UserPool', {
      userPoolName: `${appName}-users`,
      selfSignUpEnabled: false,
      accountRecovery: cognito.AccountRecovery.EMAIL_ONLY,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      lambdaTriggers: {
        preTokenGeneration: preTokenFn,
      },
    });

    // Escape hatch: declare V2_0 contract so Cognito sends claimsAndScopeOverrideDetails
    // The L2 lambdaTriggers.preTokenGeneration only emits PreTokenGeneration (V1 legacy),
    // but our Lambda (pre-token-lambda/index.js) returns the V2 contract format.
    const cfnUserPool = userPool.node.defaultChild as cognito.CfnUserPool;
    cfnUserPool.lambdaConfig = {
      preTokenGeneration: preTokenFn.functionArn,
      preTokenGenerationConfig: {
        lambdaArn: preTokenFn.functionArn,
        lambdaVersion: 'V2_0',
      },
    };

    const userPoolClient = new cognito.UserPoolClient(this, 'UserPoolClient', {
      userPool,
      userPoolClientName: `${appName}-client`,
      generateSecret: false,
      preventUserExistenceErrors: true,
      authFlows: {
        userPassword: true,
        adminUserPassword: true,
      },
      accessTokenValidity: cdk.Duration.hours(1),
      refreshTokenValidity: cdk.Duration.days(30),
    });

    const userPoolDomain = new cognito.UserPoolDomain(this, 'UserPoolDomain', {
      userPool,
      cognitoDomain: {
        domainPrefix: `${appName}-${this.account}`,
      },
    });

    // Cognito permissions for the EC2 role
    userPool.grant(roleEc2,
      'cognito-idp:CreateGroup',
      'cognito-idp:AdminAddUserToGroup',
      'cognito-idp:AdminGetUser',
    );

    // ── RDS PostgreSQL ────────────────────────────────────────────
    const rdsInstance = new rds.DatabaseInstance(this, 'RdsInstance', {
      engine: rds.DatabaseInstanceEngine.postgres({ version: rds.PostgresEngineVersion.VER_16 }),
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO),
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      securityGroups: [sgRds],
      allocatedStorage: 20,
      storageType: rds.StorageType.GP2,
      publiclyAccessible: false,
      multiAz: false,
      deletionProtection: false,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      databaseName: 'hermandad_db',
      credentials: rds.Credentials.fromGeneratedSecret('postgres'),
      backupRetention: cdk.Duration.days(1),
    });

    // ── KeyPair for SSH access ─────────────────────────────────────
    const keyPair = new ec2.KeyPair(this, 'Ec2KeyPair', {
      keyPairName: 'repertorio-deploy',
    });

    // ── EC2 Instance ──────────────────────────────────────────────
    const ami = ec2.MachineImage.latestAmazonLinux2023();

    // User data script — installs Docker + Docker Compose, then prepares the app directory
    // The actual deploy (pulling images, starting services) is done by the deploy script
    const userData = ec2.UserData.custom([
      '#!/bin/bash',
      'set -e',
      'exec > /var/log/user-data.log 2>&1',
      'REGION=eu-south-2',
      '# Update SSM agent to prevent ConnectionLost issues',
      'dnf update -y amazon-ssm-agent',
      'systemctl restart amazon-ssm-agent',
      '# Install Docker',
      'dnf install -y docker',
      'systemctl enable --now docker',
      'usermod -aG docker ec2-user',
      '# Install Docker Compose plugin',
      'DOCKER_CONFIG=/usr/local/lib/docker/cli-plugins',
      'mkdir -p $DOCKER_CONFIG',
      'curl -sL "https://github.com/docker/compose/releases/download/v2.30.3/docker-compose-linux-x86_64" -o $DOCKER_CONFIG/docker-compose',
      'chmod +x $DOCKER_CONFIG/docker-compose',
      '# Create app directory',
      'mkdir -p /opt/repertorio',
      '# Add 1GB swap as OOM safety net (uses existing EBS, $0 cost)',
      'fallocate -l 1G /swapfile',
      'chmod 600 /swapfile',
      'mkswap /swapfile',
      'swapon /swapfile',
      'echo "/swapfile swap swap defaults 0 0" >> /etc/fstab',
    ].join('\n'));

    const ec2Instance = new ec2.Instance(this, 'Ec2Instance', {
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.SMALL),
      machineImage: ami,
      securityGroup: sgEc2,
      role: roleEc2,
      associatePublicIpAddress: true,
      blockDevices: [{
        deviceName: '/dev/xvda',
        volume: ec2.BlockDeviceVolume.ebs(20, { volumeType: ec2.EbsDeviceVolumeType.GP2 }),
      }],
      userData,
      userDataCausesReplacement: true,
    });

    // ── Outputs ────────────────────────────────────────────────────
    new cdk.CfnOutput(this, 'Ec2PublicIp', { value: ec2Instance.instancePublicIp });
    new cdk.CfnOutput(this, 'RdsEndpoint', { value: rdsInstance.dbInstanceEndpointAddress });
    new cdk.CfnOutput(this, 'CognitoPoolId', { value: userPool.userPoolId });
    new cdk.CfnOutput(this, 'CognitoClientId', { value: userPoolClient.userPoolClientId });
    new cdk.CfnOutput(this, 'CognitoDomain', { value: userPoolDomain.domainName });
    new cdk.CfnOutput(this, 'Ec2KeyPairName', { value: keyPair.keyPairName });
    new cdk.CfnOutput(this, 'CognitoIssuerUrl', {
      value: `https://cognito-idp.${this.region}.amazonaws.com/${userPool.userPoolId}`,
    });

    for (const q of queues) {
      new cdk.CfnOutput(this, `Queue${q.name.replace(/[-]/g, '')}Url`, { value: q.queue.queueUrl });
    }

    for (const svc of services) {
      const safeName = svc.replace(/[-]/g, '');
      new cdk.CfnOutput(this, `Repo${safeName}Uri`, {
        value: repos[svc].repositoryUri,
      });
    }
  }
}
