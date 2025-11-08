# Quick Start Guide - AWS Web Portal

## 🚀 What You Have

A production-ready, full-stack web portal monorepo with:

- **Frontend**: React 18 + TypeScript + Vite
- **Backend**: Java 17 + Spring Boot 3
- **Infrastructure**: AWS CDK (TypeScript)
- **Database**: PostgreSQL on RDS
- **Auth**: AWS Cognito
- **Hosting**: S3 + CloudFront (frontend), ECS Fargate (backend)

## 📁 Project Structure

```
web-portal/
├── packages/
│   ├── infrastructure/       # AWS CDK infrastructure as code
│   │   ├── bin/             # CDK app entry point
│   │   └── lib/             # Stack definitions
│   │       ├── network-stack.ts      # VPC, subnets, security groups
│   │       ├── auth-stack.ts         # Cognito user pools
│   │       ├── database-stack.ts     # RDS PostgreSQL
│   │       ├── backend-stack.ts      # ECS Fargate + ALB
│   │       └── frontend-stack.ts     # S3 + CloudFront
│   │
│   ├── frontend/            # React application
│   │   ├── src/
│   │   │   ├── components/  # React components
│   │   │   ├── pages/       # Page components
│   │   │   ├── hooks/       # Custom hooks (auth)
│   │   │   ├── services/    # API client
│   │   │   └── App.tsx      # Main app with routing
│   │   └── package.json
│   │
│   └── backend/             # Java Spring Boot API
│       ├── src/main/java/com/webportal/
│       │   ├── controller/  # REST controllers
│       │   ├── service/     # Business logic
│       │   ├── repository/  # Data access
│       │   ├── model/       # JPA entities
│       │   ├── config/      # Spring configuration
│       │   └── security/    # Security configuration
│       ├── Dockerfile       # Container definition
│       └── pom.xml          # Maven dependencies
│
├── docs/                    # Documentation
│   └── DEPLOYMENT.md       # Detailed deployment guide
├── package.json            # Root workspace config
└── README.md               # Main documentation
```

## ⚡ Quick Start (5 minutes)

### 1. Prerequisites Check

```bash
# Check Node.js version (need 18+)
node --version

# Check Java version (need 17+)
java --version

# Check AWS CLI is configured
aws sts get-caller-identity

# Install CDK CLI globally
npm install -g aws-cdk
```

### 2. Install Dependencies

```bash
cd web-portal

# Install all dependencies (frontend, infrastructure)
npm run install:all

# Note: Backend uses Maven which will download dependencies on first build
```

### 3. Bootstrap AWS CDK (First Time Only)

```bash
# Set your AWS account and region
export CDK_DEFAULT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
export CDK_DEFAULT_REGION=us-east-1

# Bootstrap CDK (creates S3 bucket for artifacts)
cd packages/infrastructure
cdk bootstrap aws://${CDK_DEFAULT_ACCOUNT}/${CDK_DEFAULT_REGION}
```

### 4. Build Everything

```bash
# From project root
cd ../..

# Build frontend
npm run build:frontend

# Build backend (creates JAR file)
npm run build:backend

# Build infrastructure (compiles TypeScript)
npm run build:infrastructure
```

### 5. Deploy to AWS

```bash
# Deploy to development environment
npm run deploy:dev

# This will deploy all 5 stacks in order:
# 1. Network (VPC, subnets)
# 2. Auth (Cognito)
# 3. Database (RDS)
# 4. Backend (ECS)
# 5. Frontend (S3 + CloudFront)

# Takes ~15-20 minutes on first deployment
```

### 6. Get Your URLs

After deployment completes, look for outputs:

```
Outputs:
WebPortal-dev-Frontend.WebsiteURL = https://d123abc456def.cloudfront.net
WebPortal-dev-Backend.ApiEndpoint = http://WebPo-LoadB-ABC123.us-east-1.elb.amazonaws.com
WebPortal-dev-Auth.UserPoolId = us-east-1_ABC123XYZ
WebPortal-dev-Auth.UserPoolClientId = 1a2b3c4d5e6f7g8h9i0j
```

### 7. Create a Test User

```bash
# Replace <USER_POOL_ID> with your actual User Pool ID from outputs
aws cognito-idp admin-create-user \
  --user-pool-id <USER_POOL_ID> \
  --username admin \
  --temporary-password TempPass123! \
  --user-attributes Name=email,Value=admin@example.com \
  --message-action SUPPRESS

# Set permanent password
aws cognito-idp admin-set-user-password \
  --user-pool-id <USER_POOL_ID> \
  --username admin \
  --password SecurePass123! \
  --permanent
```

### 8. Access Your Portal

Visit the CloudFront URL from the outputs and login with:
- Username: `admin`
- Password: `SecurePass123!`

## 🔧 Local Development

### Frontend Development (with hot reload)

```bash
cd packages/frontend

# Create environment file
cp .env.template .env

# Edit .env with your deployed Cognito details
# VITE_API_URL=http://localhost:8080
# VITE_COGNITO_USER_POOL_ID=<from deployment>
# VITE_COGNITO_CLIENT_ID=<from deployment>

# Start dev server
npm run dev

# Opens at http://localhost:5173
```

### Backend Development (local Spring Boot)

```bash
# Option 1: Use deployed RDS (requires VPN or bastion)
cd packages/backend
./mvnw spring-boot:run

# Option 2: Use local PostgreSQL with Docker
docker run -d \
  --name postgres \
  -e POSTGRES_DB=webportal \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15

./mvnw spring-boot:run

# API runs at http://localhost:8080
```

## 📝 Common Commands

```bash
# Deploy specific environment
npm run deploy:dev      # Development
npm run deploy:prod     # Production

# Destroy all resources
npm run destroy

# View CloudFormation templates
npm run synth

# Check what will change before deploying
cd packages/infrastructure
cdk diff

# Deploy individual stacks
cdk deploy WebPortal-dev-Backend --context environment=dev

# View deployment outputs
aws cloudformation describe-stacks \
  --stack-name WebPortal-dev-Frontend \
  --query 'Stacks[0].Outputs'
```

## 🎯 Next Steps

1. **Customize Frontend**
   - Edit components in `packages/frontend/src/components/`
   - Add new pages in `packages/frontend/src/pages/`
   - Style with CSS files or add Tailwind/Material-UI

2. **Add Backend Endpoints**
   - Create controllers in `packages/backend/src/main/java/com/webportal/controller/`
   - Add services in `packages/backend/src/main/java/com/webportal/service/`
   - Define entities in `packages/backend/src/main/java/com/webportal/model/`

3. **Update Infrastructure**
   - Modify stacks in `packages/infrastructure/lib/`
   - Add resources like SQS, SNS, Lambda
   - Configure auto-scaling, monitoring, alarms

4. **Configure Custom Domain**
   - Purchase domain in Route 53
   - Request ACM certificate
   - Update CloudFront distribution with custom domain

5. **Set Up CI/CD**
   - GitHub Actions workflow
   - AWS CodePipeline
   - Automated testing and deployment

## 🔒 Security Checklist

- ✅ VPC with private subnets for backend/database
- ✅ Security groups restricting access
- ✅ Cognito for authentication
- ✅ HTTPS via CloudFront
- ✅ Database credentials in Secrets Manager
- ✅ IAM roles with least privilege
- ⚠️ Configure WAF for production
- ⚠️ Enable MFA for Cognito users
- ⚠️ Set up CloudWatch alarms
- ⚠️ Enable database encryption at rest

## 💰 Cost Estimate

**Development environment (running 24/7):**
- RDS t3.micro: ~$15/month
- ECS Fargate (1 task): ~$15/month
- NAT Gateway: ~$32/month
- ALB: ~$20/month
- CloudFront: ~$1/month (low traffic)
- S3: <$1/month
- **Total: ~$85-90/month**

**Production (with high availability):**
- Multiply by 2-3x for multi-AZ, larger instances

**Cost savings:**
- Stop/start RDS when not in use
- Delete dev stacks on weekends
- Use Fargate Spot for non-critical workloads

## 🐛 Troubleshooting

### CDK Bootstrap Fails
```bash
# Check AWS credentials
aws sts get-caller-identity

# Ensure you have admin permissions or these policies:
# - CloudFormation, S3, IAM, SSM
```

### Backend Container Won't Start
```bash
# Check ECS logs
aws logs tail /ecs/web-portal-backend --follow

# Common issues:
# - Database connection failed → Check security groups
# - Out of memory → Increase task memory in backend-stack.ts
# - Environment variables missing → Check ECS task definition
```

### Frontend Shows "Network Error"
```bash
# Check if API is accessible
curl http://<ALB_DNS>/api/hello

# Check CORS configuration in SecurityConfig.java
# Verify CloudFront can reach S3
# Check browser console for specific errors
```

### Database Connection Timeout
```bash
# Verify RDS is in correct subnets
# Check security group allows ECS → RDS on port 5432
# Test from ECS task:
aws ecs execute-command \
  --cluster web-portal-backend \
  --task <TASK_ID> \
  --container api \
  --interactive \
  --command "psql -h <RDS_ENDPOINT> -U webportaladmin -d webportal"
```

## 📚 Additional Resources

- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/)
- [React Documentation](https://react.dev/)
- [Full Deployment Guide](./docs/DEPLOYMENT.md)

## 🆘 Support

For issues or questions:
1. Check logs in CloudWatch
2. Review CloudFormation events in AWS Console
3. Consult documentation in `docs/` folder

## ✅ Verification Checklist

After deployment, verify:
- [ ] CloudFront URL loads the login page
- [ ] Can login with test user credentials
- [ ] Dashboard page displays after login
- [ ] API health endpoint returns 200: `curl <API_URL>/actuator/health`
- [ ] Backend can connect to RDS
- [ ] CloudWatch logs showing backend activity

Happy coding! 🚀