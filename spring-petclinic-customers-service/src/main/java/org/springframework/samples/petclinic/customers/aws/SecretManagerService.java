// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package org.springframework.samples.petclinic.customers.aws;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

@Component
public class SecretManagerService {
    final SecretsManagerClient secretsManagerClient;
    final static String topicArn = "arn:aws:sns:us-east-1:252610625673:apm-sns-test";

    public SecretManagerService() {
        // AWS web identity is set for EKS clusters, if these are not set then use default credentials
        if (System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE") == null && System.getProperty("aws.webIdentityTokenFile") == null) {
            secretsManagerClient = SecretsManagerClient.builder()
                    .region(Region.US_EAST_1)
                .build();
        }
        else {
            secretsManagerClient = SecretsManagerClient.builder()
                    .region(Region.US_EAST_1)
                .credentialsProvider(WebIdentityTokenFileCredentialsProvider.create())
                .build();
        }
    }

    public void getValue(String secretName) {
        try {
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            GetSecretValueResponse valueResponse = secretsManagerClient.getSecretValue(valueRequest);
            String secret = valueResponse.secretString();
            System.out.println(secret);
            System.out.println(valueResponse.toString());

        } catch (SecretsManagerException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        SecretManagerService client = new SecretManagerService();
        client.getValue("test/apm-secrets");
    }
}
