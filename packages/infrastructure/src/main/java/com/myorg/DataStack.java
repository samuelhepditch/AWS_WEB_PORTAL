package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.constructs.Construct;

import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.secretsmanager.*;

import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;


import java.util.List;

public class DataStack extends Stack {

    private final Vpc vpc;
    private final DatabaseInstance dbInstance;
    private final ISecret dbSecret;
    private final String dbName;
    private final SecurityGroup dbSecurityGroup;

    public DataStack(final Construct scope, final String id, final StackProps props) {
        this(scope, id, props, "appdb");
    }

    public DataStack(final Construct scope, final String id, final StackProps props, final String dbName) {
        super(scope, id, props);
        this.dbName = dbName;

        // --- VPC (3 subnet tiers; 1 NAT for simplicity) ---
        this.vpc = Vpc.Builder.create(this, "AppVpc")
            .maxAzs(1)
            .natGateways(1) // Easiest to get Lambda egress to AWS APIs; see note below to remove
            .subnetConfiguration(List.of(
                SubnetConfiguration.builder().name("public").subnetType(SubnetType.PUBLIC).build(),
                SubnetConfiguration.builder().name("private-egress").subnetType(SubnetType.PRIVATE_WITH_EGRESS).build(),
                SubnetConfiguration.builder().name("isolated").subnetType(SubnetType.PRIVATE_ISOLATED).build()
            ))
            .build();

        // --- DB security group ---
        this.dbSecurityGroup = SecurityGroup.Builder.create(this, "DbSg")
            .vpc(vpc)
            .allowAllOutbound(false)
            .build();

        // --- Credentials in Secrets Manager ---
        this.dbSecret = Secret.Builder.create(this, "DbSecret")
            .generateSecretString(SecretStringGenerator.builder()
                .secretStringTemplate("{\"username\":\"appuser\"}")
                .generateStringKey("password")
                .excludePunctuation(true)
                .build())
            .build();

        // --- Cost-sensitive RDS PostgreSQL (single-AZ) ---
        this.dbInstance = DatabaseInstance.Builder.create(this, "PostgresDb")
            .engine(DatabaseInstanceEngine.postgres(
                PostgresInstanceEngineProps.builder()
                    .version(PostgresEngineVersion.VER_17) // adjust to your CDK version if needed
                    .build()))
            .credentials(Credentials.fromSecret(this.dbSecret))
            .databaseName(this.dbName)
            .vpc(vpc)
            .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE_ISOLATED).build())
            .securityGroups(List.of(this.dbSecurityGroup))
            .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO)) // t3.micro
            .allocatedStorage(20)
            .storageType(StorageType.GP3)
            .multiAz(false)
            .publiclyAccessible(false)
            .autoMinorVersionUpgrade(true)
            .backupRetention(Duration.days(1))
            .deletionProtection(false)
            .removalPolicy(RemovalPolicy.SNAPSHOT)
            .build();
    }

    // --- Getters for other stacks ---
    public Vpc getVpc() { return vpc; }
    public DatabaseInstance getDbInstance() { return dbInstance; }
    public ISecret getDbSecret() { return dbSecret; }
    public String getDbName() { return dbName; }
    public SecurityGroup getDbSecurityGroup() { return dbSecurityGroup; }
}
