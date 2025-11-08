package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class InfrastructureApp {
    public static void main(final String[] args) {
        App app = new App();

        Environment env = Environment.builder()
            .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
            .region(System.getenv("CDK_DEFAULT_REGION"))
            .build();

        // DataStack creates the VPC we’ll reuse
        DataStack data = new DataStack(app, "DataStack",
            StackProps.builder().env(env).build(),
            "appdb");

        // Auth (Cognito) – assumes you have an AuthStack exposing IDs
        AuthStack auth = new AuthStack(app, "AuthStack",
            StackProps.builder().env(env).build());

        // API uses the VPC from DataStack + DB + Secret + Cognito
        ApiStack api = new ApiStack(app, "ApiStack",
            StackProps.builder().env(env).build(),
            data.getVpc(),
            data.getDbInstance(),
            data.getDbSecret(),
            data.getDbName(),
            auth.getUserPoolId(),
            auth.getUserPoolClientId());

        api.addDependency(data);
        api.addDependency(auth);

        app.synth();
    }
}



