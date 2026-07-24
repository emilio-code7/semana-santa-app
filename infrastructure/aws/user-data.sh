#!/bin/bash
# EC2 user-data script — repertorio stack bootstrap
set -e
exec > /var/log/user-data.log 2>&1

REGION=eu-south-2

# Update SSM agent to latest version (prevents ConnectionLost issues)
dnf update -y amazon-ssm-agent
systemctl restart amazon-ssm-agent

# Install Docker
dnf install -y docker
systemctl enable --now docker
usermod -aG docker ec2-user

# Install Docker Compose plugin (not available in AL2023 repos)
DOCKER_CONFIG=/usr/local/lib/docker/cli-plugins
mkdir -p $DOCKER_CONFIG
curl -sL "https://github.com/docker/compose/releases/download/v2.30.3/docker-compose-linux-x86_64" -o $DOCKER_CONFIG/docker-compose
chmod +x $DOCKER_CONFIG/docker-compose

# Create app directory
mkdir -p /opt/repertorio
cd /opt/repertorio
