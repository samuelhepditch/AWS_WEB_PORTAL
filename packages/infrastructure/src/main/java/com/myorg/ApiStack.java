package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.secretsmanager.ISecret;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.cognito.UserPool;

import java.util.List;
import java.util.Map;

public class ApiStack extends Stack {

    public ApiStack(
        final Construct scope,
        final String id,
        final StackProps props,
        final Vpc vpc,
        final DatabaseInstance dbInstance,
        final ISecret dbSecret,
        final String dbName,
        final String userPoolId,
        final String userPoolClientId
    ) {
        super(scope, id, props);

        // Lambda SG
        SecurityGroup lambdaSg = SecurityGroup.Builder.create(this, "LambdaSg")
            .vpc(vpc)
            .allowAllOutbound(true)
            .build();

        // Permit Lambda → Postgres (5432)
        dbInstance.getConnections().allowDefaultPortFrom(lambdaSg);

        // Lambda function (Python shown; Java/Node also fine)
        Function handler = Function.Builder.create(this, "ApiHandler")
            .runtime(Runtime.PYTHON_3_11)
            .handler("app.lambda_handler")
            .code(Code.fromAsset("lambda")) // folder with app.py
            .vpc(vpc)
            .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE_WITH_EGRESS).build())
            .securityGroups(List.of(lambdaSg))
            .timeout(software.amazon.awscdk.Duration.seconds(30))
            .memorySize(512)
            .environment(Map.of(
                "DB_HOST", dbInstance.getDbInstanceEndpointAddress(),
                "DB_PORT", String.valueOf(dbInstance.getDbInstanceEndpointPort()),
                "DB_NAME", dbName,
                "DB_SECRET_ARN", dbSecret.getSecretArn(),
                "AWS_REGION", Stack.of(this).getRegion()
            ))
            .build();

        // Secret access for Lambda
        dbSecret.grantRead(handler);

        // API Gateway (REST) + Cognito authorizer
        RestApi api = RestApi.Builder.create(this, "SecondBrainApi")
            .restApiName("SecondBrain Service")
            .deployOptions(StageOptions.builder()
                .stageName("prod")
                .metricsEnabled(true)
                .loggingLevel(MethodLoggingLevel.INFO)
                .build())
            .build();

        CognitoUserPoolsAuthorizer authorizer = CognitoUserPoolsAuthorizer.Builder.create(this, "CognitoAuthorizer")
            .cognitoUserPools(List.of(
                UserPool.fromUserPoolId(this, "ImportedPool", userPoolId)
            ))
            .build();

        LambdaIntegration integration = LambdaIntegration.Builder.create(handler)
            .proxy(true)
            .build();

        Resource notes = api.getRoot().addResource("notes");
        notes.addMethod("GET", integration, MethodOptions.builder()
            .authorizer(authorizer)
            .authorizationType(AuthorizationType.COGNITO)
            .build());
        notes.addMethod("POST", integration, MethodOptions.builder()
            .authorizer(authorizer)
            .authorizationType(AuthorizationType.COGNITO)
            .build());
    }
}
