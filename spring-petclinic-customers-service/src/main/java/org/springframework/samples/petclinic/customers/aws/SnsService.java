// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package org.springframework.samples.petclinic.customers.aws;

import org.springframework.samples.petclinic.customers.Util;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorResponse;
import software.amazon.awssdk.services.kinesis.model.KinesisException;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.ArrayList;
import java.util.List;

@Component
public class SnsService {
    final SnsClient snsClient;
    final static String topicArn = "arn:aws:sns:us-east-1:252610625673:apm-sns-test";

    public SnsService() {
        // AWS web identity is set for EKS clusters, if these are not set then use default credentials
        if (System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE") == null && System.getProperty("aws.webIdentityTokenFile") == null) {
            snsClient = SnsClient.builder()
//                .region(Region.of(Util."REGION_FROM_EC2"))
                    .region(Region.US_EAST_1)
                .build();
        }
        else {
            snsClient = SnsClient.builder()
//                .region(Region.of(Util.REGION_FROM_EKS))
                    .region(Region.US_EAST_1)
                .credentialsProvider(WebIdentityTokenFileCredentialsProvider.create())
                .build();
        }
    }

    public void pubTopic(String message) {
        try {
            PublishRequest request = PublishRequest.builder()
                    .message(message)
                    .topicArn(topicArn)
                    .build();

            PublishResponse result = snsClient.publish(request);
            System.out
                    .println(result.messageId() + " Message sent. Status is " + result.sdkHttpResponse().statusCode());

        } catch (SnsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        SnsService client = new SnsService();
        client.pubTopic("test sns msg");
    }
}
