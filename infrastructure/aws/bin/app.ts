#!/usr/bin/env node
import * as cdk from "aws-cdk-lib";
import { RepertorioInfraStack } from "../lib/stack";
import {
  ApplicationAssociator,
  TargetApplication,
} from "@aws-cdk/aws-servicecatalogappregistry-alpha";

const app = new cdk.App();

// Target region — set REPERTORIO_AWS_REGION to override (default: eu-south-2 Madrid)
// NOTE: CDK_DEFAULT_REGION is always set by the CDK CLI, so we use a custom env var
const region = process.env.REPERTORIO_AWS_REGION || "eu-south-2";
const account = process.env.CDK_DEFAULT_ACCOUNT;
const env = { account, region };

// Register in AWS AppRegistry so it appears in "My Applications" tab
new ApplicationAssociator(app, "RepertorioApplication", {
  applications: [
    TargetApplication.createApplicationStack({
      applicationName: "Repertorio",
      applicationDescription:
        "Semana Santa management system ? 3 microservices (hermandad, procesion, repertorio)",
      env,
    }),
  ],
});

new RepertorioInfraStack(app, "RepertorioInfraStack", {
  env,
  description: "Repertorio ? Semana Santa management system (free-tier)",
  terminationProtection: true,
});
