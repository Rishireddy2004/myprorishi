# Cloud Deployment Guide (AWS)

This guide covers deploying the Smart Ride Sharing System on AWS using EC2 + RDS + ElastiCache. An optional ECS/Fargate containerized deployment is included at the end.

---

## Architecture Overview

```
Internet → EC2 (Spring Boot JAR)
               ├── RDS MySQL (database)
               └── ElastiCache Redis (caching / real-time tracking)
```

---

## 1. Build the JAR Locally

```bash
mvn clean package -DskipTests
```

This produces `target/smart-ride-sharing-system-0.0.1-SNAPSHOT.jar`.

---

## 2. RDS MySQL Setup

1. Open **RDS** in the AWS Console → **Create database**
2. Engine: **MySQL 8.0**
3. Template: **Free tier** (dev) or **Production**
4. DB instance identifier: `ridesharing-db`
5. Master username: `admin`, set a strong password
6. Instance class: `db.t3.micro` (dev) / `db.t3.medium` (prod)
7. Storage: 20 GB gp3, enable autoscaling
8. VPC: same VPC as your EC2 instance
9. Public access: **No** (access only from within VPC)
10. Create a security group `rds-sg` that allows port **3306** from the EC2 security group

After creation, note the **Endpoint** (e.g. `ridesharing-db.xxxx.us-east-1.rds.amazonaws.com`).

Connect and create the schema:

```bash
mysql -h <rds-endpoint> -u admin -p
```

```sql
CREATE DATABASE ridesharing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'appuser'@'%' IDENTIFIED BY 'strong_password';
GRANT ALL PRIVILEGES ON ridesharing.* TO 'appuser'@'%';
FLUSH PRIVILEGES;
```

---

## 3. ElastiCache Redis Setup

1. Open **ElastiCache** → **Create cluster** → **Redis OSS**
2. Cluster mode: **Disabled** (single node for simplicity)
3. Node type: `cache.t3.micro`
4. Engine version: **7.x**
5. Subnet group: same VPC subnets as EC2
6. Security group `redis-sg`: allow port **6379** from the EC2 security group
7. Enable **in-transit encryption** and set an auth token (this becomes `REDIS_PASSWORD`)

Note the **Primary endpoint** (e.g. `ridesharing-redis.xxxx.cache.amazonaws.com`).

---

## 4. EC2 Instance Setup

### Launch Instance

1. Open **EC2** → **Launch Instance**
2. AMI: **Amazon Linux 2023**
3. Instance type: `t3.small` (minimum) / `t3.medium` (recommended)
4. Key pair: create or select an existing key pair
5. Security group `app-sg`:
   - Inbound: port **22** (SSH) from your IP, port **8080** (or **443** if behind ALB) from anywhere
   - Outbound: all traffic
6. Storage: 20 GB gp3

### Install JDK 17

```bash
ssh -i your-key.pem ec2-user@<ec2-public-ip>

sudo dnf update -y
sudo dnf install -y java-17-amazon-corretto-headless
java -version
```

### Upload the JAR

From your local machine:

```bash
scp -i your-key.pem \
  target/smart-ride-sharing-system-0.0.1-SNAPSHOT.jar \
  ec2-user@<ec2-public-ip>:/opt/ridesharing/app.jar
```

Or use S3:

```bash
# Local
aws s3 cp target/smart-ride-sharing-system-0.0.1-SNAPSHOT.jar s3://your-bucket/app.jar

# On EC2
sudo mkdir -p /opt/ridesharing
sudo aws s3 cp s3://your-bucket/app.jar /opt/ridesharing/app.jar
```

---

## 5. Environment Variables

Create a secure environment file on the EC2 instance:

```bash
sudo mkdir -p /opt/ridesharing
sudo nano /opt/ridesharing/app.env
```

```ini
DB_URL=jdbc:mysql://ridesharing-db.xxxx.us-east-1.rds.amazonaws.com:3306/ridesharing
DB_USERNAME=appuser
DB_PASSWORD=strong_password
REDIS_HOST=ridesharing-redis.xxxx.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_auth_token
JWT_SECRET=a-very-long-random-secret-key-at-least-256-bits
GOOGLE_MAPS_API_KEY=AIza...
STRIPE_SECRET_KEY=sk_live_...
STRIPE_PUBLISHABLE_KEY=pk_live_...
TWILIO_ACCOUNT_SID=AC...
TWILIO_AUTH_TOKEN=...
TWILIO_FROM_NUMBER=+15551234567
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=noreply@yourdomain.com
MAIL_PASSWORD=your_app_password
```

```bash
sudo chmod 600 /opt/ridesharing/app.env
sudo chown root:root /opt/ridesharing/app.env
```

---

## 6. Run as a systemd Service

```bash
sudo nano /etc/systemd/system/ridesharing.service
```

```ini
[Unit]
Description=Smart Ride Sharing System
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/ridesharing
EnvironmentFile=/opt/ridesharing/app.env
ExecStart=/usr/bin/java -jar /opt/ridesharing/app.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=ridesharing

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable ridesharing
sudo systemctl start ridesharing
sudo systemctl status ridesharing

# View logs
sudo journalctl -u ridesharing -f
```

The application is now accessible at `http://<ec2-public-ip>:8080`.

---

## 7. (Optional) Application Load Balancer + HTTPS

1. Create an **ALB** in the same VPC
2. Target group: HTTP, port 8080, health check path `/api-docs`
3. Register the EC2 instance
4. Add an HTTPS listener (port 443) with an ACM certificate
5. Update `app-sg` to allow port 8080 only from the ALB security group

---

## 8. (Optional) ECS/Fargate Containerized Deployment

### Dockerfile

Create `Dockerfile` in the project root:

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/smart-ride-sharing-system-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Build and Push to ECR

```bash
# Authenticate
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Create repository
aws ecr create-repository --repository-name ridesharing --region us-east-1

# Build and push
docker build -t ridesharing .
docker tag ridesharing:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/ridesharing:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/ridesharing:latest
```

### ECS Task Definition (key environment variables)

In the ECS task definition, pass environment variables via **AWS Secrets Manager** or **Systems Manager Parameter Store** rather than plain text. Example using SSM:

```json
{
  "environment": [],
  "secrets": [
    { "name": "DB_URL",            "valueFrom": "arn:aws:ssm:us-east-1:ACCOUNT:parameter/ridesharing/DB_URL" },
    { "name": "DB_USERNAME",       "valueFrom": "arn:aws:ssm:us-east-1:ACCOUNT:parameter/ridesharing/DB_USERNAME" },
    { "name": "DB_PASSWORD",       "valueFrom": "arn:aws:ssm:us-east-1:ACCOUNT:parameter/ridesharing/DB_PASSWORD" },
    { "name": "JWT_SECRET",        "valueFrom": "arn:aws:ssm:us-east-1:ACCOUNT:parameter/ridesharing/JWT_SECRET" },
    { "name": "STRIPE_SECRET_KEY", "valueFrom": "arn:aws:ssm:us-east-1:ACCOUNT:parameter/ridesharing/STRIPE_SECRET_KEY" }
  ]
}
```

Create an ECS Fargate service with the task definition, attach it to the ALB target group, and the application will scale horizontally.

---

## Deployment Checklist

- [ ] RDS instance running and schema created
- [ ] ElastiCache Redis cluster running
- [ ] EC2 instance has JDK 17 installed
- [ ] JAR uploaded to `/opt/ridesharing/app.jar`
- [ ] `/opt/ridesharing/app.env` created with all variables (chmod 600)
- [ ] `ridesharing.service` enabled and started
- [ ] Security groups allow EC2 → RDS (3306) and EC2 → Redis (6379)
- [ ] Application accessible at `http://<ec2-ip>:8080/swagger-ui.html`
