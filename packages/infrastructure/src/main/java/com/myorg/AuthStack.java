package com.myorg;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cognito.*;
import software.constructs.Construct;

// This cognito user pool has the following features:

// First we create a UserPool that does not have self-signup enabled to avoid malicious attacks.
// We set username sign in to false to simplify the login experience.
// At sign-in time we expect cognito to send a verification email to confirm the users login intent.
// The standard attributes for sign-up at login time are:
// - email, givenName and familyName
// Password requirements are pretty obvious
// Account recovery password resets occur via an email
// Removal policy is if you delete the stack the userpool is maintained. 
// - This is the retention policy that is handled at the time of deleting the cloud formation stack.


// User pool client

// When your web app asks users to log in, it must identify itself to Cognito — that’s what the User Pool Client represents.
// You can have multiple clients for the same pool:
// one for your web app
// one for your iOS app
// one for your backend service (if needed)
// Each client can have different permissions and token lifetimes.

// What is SRP??
// SRP = Secure Remote Password protocol, a cryptographic way for clients to perform password authentication without ever sending the password to Cognito in plaintext.

//
// | Token             | Purpose                                                   | Lifetime in your config |
// | ----------------- | --------------------------------------------------------- | ----------------------- |
// | **Access Token**  | Used to call APIs (like API Gateway authorizer checks it) | ⏱️ 1 hour               |
// | **ID Token**      | Contains user info (email, name, claims)                  | ⏱️ 1 hour               |
// | **Refresh Token** | Lets you get new tokens without re-login                  | ⏱️ 30 days              |
//
//




public class AuthStack extends Stack {

    private final UserPool userPool;
    private final UserPoolClient userPoolClient;
    private final String USER_POOL_ID = "WebPortalUserPool";
    private final String USER_POOL_NAME = "web-portal-users";
    private final String USER_POOL_CLIENT_ID = "WebPortalUserPoolClient";

    public AuthStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Cognito User Pool
        this.userPool = UserPool.Builder.create(this, USER_POOL_ID)
                .userPoolName(USER_POOL_NAME)
                .selfSignUpEnabled(false)
                .signInAliases(SignInAliases.builder()
                        .email(true)
                        .username(false)
                        .build())
                .autoVerify(AutoVerifiedAttrs.builder()
                        .email(true)
                        .build())
                .standardAttributes(StandardAttributes.builder()
                        .email(StandardAttribute.builder()
                                .required(true)
                                .mutable(true)
                                .build())
                        .givenName(StandardAttribute.builder()
                                .required(true)
                                .mutable(true)
                                .build())
                        .familyName(StandardAttribute.builder()
                                .required(true)
                                .mutable(true)
                                .build())
                        .build())
                .passwordPolicy(PasswordPolicy.builder()
                        .minLength(8)
                        .requireLowercase(true)
                        .requireUppercase(true)
                        .requireDigits(true)
                        .requireSymbols(true)
                        .build())
                .accountRecovery(AccountRecovery.EMAIL_ONLY)
                .removalPolicy(RemovalPolicy.RETAIN) // Change to DESTROY for dev
                .build();

        // User Pool Client for the web application
        this.userPoolClient = UserPoolClient.Builder.create(this, USER_POOL_CLIENT_ID)
                .userPool(userPool)
                .userPoolClientName("web-portal-client")
                .authFlows(AuthFlow.builder()
                        .userPassword(true)
                        .userSrp(true)
                        .build())
                .generateSecret(false) // Public client (frontend)
                .preventUserExistenceErrors(true)
                .refreshTokenValidity(Duration.days(30))
                .accessTokenValidity(Duration.hours(1))
                .idTokenValidity(Duration.hours(1))
                .build();

        // User Pool Domain for hosted UI (optional)
        UserPoolDomain domain = userPool.addDomain("CognitoDomain",
                UserPoolDomainOptions.builder()
                        .cognitoDomain(CognitoDomainOptions.builder()
                                .domainPrefix("web-portal-" + getAccount())
                                .build())
                        .build());

        // Outputs
        CfnOutput.Builder.create(this, "UserPoolIdOutput")
                .value(userPool.getUserPoolId())
                .description("User Pool ID")
                .exportName(getStackName() + "-UserPoolId")
                .build();

        CfnOutput.Builder.create(this, "UserPoolClientIdOutput")
                .value(userPoolClient.getUserPoolClientId())
                .description("User Pool Client ID")
                .exportName(getStackName() + "-UserPoolClientId")
                .build();

        CfnOutput.Builder.create(this, "UserPoolDomainOutput")
                .value(domain.getDomainName())
                .description("User Pool Domain")
                .build();
    }

    public UserPool getUserPool() {
        return userPool;
    }

    public String getUserPoolId() {
        return USER_POOL_ID;
    }

    public String getUserPoolClientId() {
        return USER_POOL_CLIENT_ID;
    }
    public UserPoolClient getUserPoolClient() {
        return userPoolClient;
    }
}
