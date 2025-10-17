// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0
package org.springframework.samples.petclinic.customers.aws;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.samples.petclinic.customers.Util;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

@Component
public class SqsService {
    private static final Logger logger = LoggerFactory.getLogger(SqsService.class);
    private static final String QUEUE_NAME = "apm_test";
    
    // OpenTelemetry instrumentation
    private final Tracer tracer;
    private final Meter meter;
    private final LongCounter sqsOperationCounter;
    private final LongCounter sqsErrorCounter;
    private final LongHistogram sqsOperationDuration;
    
    // Attribute keys for consistent telemetry
    private static final AttributeKey<String> SQS_OPERATION = AttributeKey.stringKey("sqs.operation");
    private static final AttributeKey<String> SQS_QUEUE_NAME = AttributeKey.stringKey("sqs.queue.name");
    private static final AttributeKey<String> SQS_ERROR_CODE = AttributeKey.stringKey("sqs.error.code");
    private static final AttributeKey<String> SQS_ERROR_MESSAGE = AttributeKey.stringKey("sqs.error.message");
    private static final AttributeKey<String> AWS_REGION = AttributeKey.stringKey("aws.region");
    
    final SqsClient sqs;

    public SqsService() {
        // Initialize OpenTelemetry instrumentation
        this.tracer = GlobalOpenTelemetry.getTracer("petclinic.sqs.service", "1.0.0");
        this.meter = GlobalOpenTelemetry.getMeter("petclinic.sqs.service", "1.0.0");
        
        // Create metrics for monitoring SQS operations
        this.sqsOperationCounter = meter
            .counterBuilder("sqs.operations.total")
            .setDescription("Total number of SQS operations")
            .build();
            
        this.sqsErrorCounter = meter
            .counterBuilder("sqs.errors.total")
            .setDescription("Total number of SQS operation errors")
            .build();
            
        this.sqsOperationDuration = meter
            .histogramBuilder("sqs.operation.duration")
            .setDescription("Duration of SQS operations in milliseconds")
            .setUnit("ms")
            .ofLongs()
            .build();

        logger.info("Initializing SQS service with OpenTelemetry instrumentation");
        
        // AWS web identity is set for EKS clusters, if these are not set then use default credentials
        String regionName;
        if (System.getenv("REGION_FROM_ECS") != null) {
            regionName = System.getenv("REGION_FROM_ECS");
            sqs = SqsClient.builder()
                .region(Region.of(regionName))
                .build();
        }
        else if (System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE") == null && System.getProperty("aws.webIdentityTokenFile") == null) {
            regionName = Util.REGION_FROM_EC2;
            sqs = SqsClient.builder()
                .region(Region.of(regionName))
                .build();
        }
        else {
            regionName = Util.REGION_FROM_EKS;
            sqs = SqsClient.builder()
                .region(Region.of(regionName))
                .credentialsProvider(WebIdentityTokenFileCredentialsProvider.create())
                .build();
        }

        // Initialize queue with instrumentation
        initializeQueue(regionName);
    }
    
    private void initializeQueue(String regionName) {
        Span span = tracer.spanBuilder("sqs.create_queue")
            .setSpanKind(io.opentelemetry.api.trace.SpanKind.CLIENT)
            .setAttribute(SQS_OPERATION, "CreateQueue")
            .setAttribute(SQS_QUEUE_NAME, QUEUE_NAME)
            .setAttribute(AWS_REGION, regionName)
            .startSpan();
            
        long startTime = System.currentTimeMillis();
        
        try (Scope scope = span.makeCurrent()) {
            CreateQueueResponse createResult = sqs.createQueue(CreateQueueRequest.builder().queueName(QUEUE_NAME).build());
            
            // Record successful operation
            sqsOperationCounter.add(1, Attributes.of(
                SQS_OPERATION, "CreateQueue",
                SQS_QUEUE_NAME, QUEUE_NAME,
                AWS_REGION, regionName
            ));
            
            span.setStatus(StatusCode.OK);
            logger.info("SQS queue '{}' initialized successfully in region '{}'", QUEUE_NAME, regionName);
            
        } catch (SqsException e) {
            if (e.awsErrorDetails().errorCode().equals("QueueAlreadyExists")) {
                // This is expected, not an error
                span.setStatus(StatusCode.OK);
                span.setAttribute("sqs.queue.already_exists", true);
                logger.info("SQS queue '{}' already exists in region '{}'", QUEUE_NAME, regionName);
            } else {
                // Record error
                sqsErrorCounter.add(1, Attributes.of(
                    SQS_OPERATION, "CreateQueue",
                    SQS_QUEUE_NAME, QUEUE_NAME,
                    SQS_ERROR_CODE, e.awsErrorDetails().errorCode(),
                    AWS_REGION, regionName
                ));
                
                span.setStatus(StatusCode.ERROR, "Failed to create SQS queue");
                span.setAttribute(SQS_ERROR_CODE, e.awsErrorDetails().errorCode());
                span.setAttribute(SQS_ERROR_MESSAGE, e.awsErrorDetails().errorMessage());
                
                logger.error("Failed to create SQS queue '{}' in region '{}': {} - {}", 
                    QUEUE_NAME, regionName, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());
                throw e;
            }
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            sqsOperationDuration.record(duration, Attributes.of(
                SQS_OPERATION, "CreateQueue",
                SQS_QUEUE_NAME, QUEUE_NAME,
                AWS_REGION, regionName
            ));
            span.end();
        }
    }

    public void sendMsg() {
        Span span = tracer.spanBuilder("sqs.send_message")
            .setSpanKind(io.opentelemetry.api.trace.SpanKind.CLIENT)
            .setAttribute(SQS_OPERATION, "SendMessage")
            .setAttribute(SQS_QUEUE_NAME, QUEUE_NAME)
            .startSpan();
            
        long startTime = System.currentTimeMillis();
        String queueUrl = null;
        
        try (Scope scope = span.makeCurrent()) {
            logger.debug("Starting SQS send message operation for queue '{}'", QUEUE_NAME);
            
            // Get queue URL with instrumentation
            queueUrl = getQueueUrlWithInstrumentation();
            span.setAttribute("sqs.queue.url", queueUrl);
            
            // Send message with instrumentation
            sendMessageWithInstrumentation(queueUrl);
            
            // Record successful operation
            sqsOperationCounter.add(1, Attributes.of(
                SQS_OPERATION, "SendMessage",
                SQS_QUEUE_NAME, QUEUE_NAME
            ));
            
            span.setStatus(StatusCode.OK);
            logger.info("Successfully sent message to SQS queue '{}'", QUEUE_NAME);
            
        } catch (SqsException e) {
            // Record error with detailed attributes
            sqsErrorCounter.add(1, Attributes.of(
                SQS_OPERATION, "SendMessage",
                SQS_QUEUE_NAME, QUEUE_NAME,
                SQS_ERROR_CODE, e.awsErrorDetails().errorCode()
            ));
            
            span.setStatus(StatusCode.ERROR, "Failed to send message to SQS");
            span.setAttribute(SQS_ERROR_CODE, e.awsErrorDetails().errorCode());
            span.setAttribute(SQS_ERROR_MESSAGE, e.awsErrorDetails().errorMessage());
            
            // Log detailed error information for troubleshooting
            logger.error("SQS SendMessage failed for queue '{}': {} - {} (HTTP Status: {})", 
                QUEUE_NAME, 
                e.awsErrorDetails().errorCode(), 
                e.awsErrorDetails().errorMessage(),
                e.statusCode());
                
            // Add specific handling for common SQS errors
            if (e.statusCode() == 403) {
                logger.error("🚨 SQS Permission Error: Check IAM permissions for SQS operations. " +
                    "Required permissions: sqs:SendMessage, sqs:GetQueueUrl");
                span.setAttribute("sqs.troubleshooting.hint", 
                    "Check IAM permissions for SQS operations");
            } else if (e.statusCode() == 400 && e.awsErrorDetails().errorCode().equals("AWS.SimpleQueueService.NonExistentQueue")) {
                logger.error("🚨 SQS Queue Not Found: Queue '{}' does not exist or is not accessible", QUEUE_NAME);
                span.setAttribute("sqs.troubleshooting.hint", 
                    "Queue does not exist or is not accessible");
            }
            
            throw e;
        } catch (Exception e) {
            // Handle unexpected errors
            sqsErrorCounter.add(1, Attributes.of(
                SQS_OPERATION, "SendMessage",
                SQS_QUEUE_NAME, QUEUE_NAME,
                SQS_ERROR_CODE, "UnexpectedError"
            ));
            
            span.setStatus(StatusCode.ERROR, "Unexpected error during SQS operation");
            span.setAttribute("error.type", e.getClass().getSimpleName());
            span.setAttribute("error.message", e.getMessage());
            
            logger.error("Unexpected error during SQS send message operation for queue '{}': {}", 
                QUEUE_NAME, e.getMessage(), e);
            throw new RuntimeException("SQS operation failed", e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            sqsOperationDuration.record(duration, Attributes.of(
                SQS_OPERATION, "SendMessage",
                SQS_QUEUE_NAME, QUEUE_NAME
            ));
            span.end();
        }
        
        // Note: PurgeQueue removed to prevent 403 errors due to AWS SQS rate limiting
        // PurgeQueue can only be called once every 60 seconds per queue
        // For demo purposes, we'll skip purging the queue after each message
        logger.debug("SQS PurgeQueue operation skipped to prevent rate limiting issues");
    }
    
    private String getQueueUrlWithInstrumentation() {
        Span span = tracer.spanBuilder("sqs.get_queue_url")
            .setSpanKind(io.opentelemetry.api.trace.SpanKind.CLIENT)
            .setAttribute(SQS_OPERATION, "GetQueueUrl")
            .setAttribute(SQS_QUEUE_NAME, QUEUE_NAME)
            .startSpan();
            
        long startTime = System.currentTimeMillis();
        
        try (Scope scope = span.makeCurrent()) {
            String queueUrl = sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(QUEUE_NAME).build()).queueUrl();
            
            sqsOperationCounter.add(1, Attributes.of(
                SQS_OPERATION, "GetQueueUrl",
                SQS_QUEUE_NAME, QUEUE_NAME
            ));
            
            span.setStatus(StatusCode.OK);
            span.setAttribute("sqs.queue.url", queueUrl);
            logger.debug("Retrieved queue URL for '{}': {}", QUEUE_NAME, queueUrl);
            
            return queueUrl;
        } catch (SqsException e) {
            sqsErrorCounter.add(1, Attributes.of(
                SQS_OPERATION, "GetQueueUrl",
                SQS_QUEUE_NAME, QUEUE_NAME,
                SQS_ERROR_CODE, e.awsErrorDetails().errorCode()
            ));
            
            span.setStatus(StatusCode.ERROR, "Failed to get queue URL");
            span.setAttribute(SQS_ERROR_CODE, e.awsErrorDetails().errorCode());
            span.setAttribute(SQS_ERROR_MESSAGE, e.awsErrorDetails().errorMessage());
            
            logger.error("Failed to get queue URL for '{}': {} - {}", 
                QUEUE_NAME, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage());
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            sqsOperationDuration.record(duration, Attributes.of(
                SQS_OPERATION, "GetQueueUrl",
                SQS_QUEUE_NAME, QUEUE_NAME
            ));
            span.end();
        }
    }
    
    private void sendMessageWithInstrumentation(String queueUrl) {
        Span span = tracer.spanBuilder("sqs.send_message_request")
            .setSpanKind(io.opentelemetry.api.trace.SpanKind.CLIENT)
            .setAttribute(SQS_OPERATION, "SendMessageRequest")
            .setAttribute(SQS_QUEUE_NAME, QUEUE_NAME)
            .setAttribute("sqs.queue.url", queueUrl)
            .startSpan();
            
        long startTime = System.currentTimeMillis();
        
        try (Scope scope = span.makeCurrent()) {
            SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody("hello world")
                .delaySeconds(5)
                .build();
                
            span.setAttribute("sqs.message.delay_seconds", 5);
            span.setAttribute("sqs.message.body_length", "hello world".length());
            
            sqs.sendMessage(sendMsgRequest);
            
            sqsOperationCounter.add(1, Attributes.of(
                SQS_OPERATION, "SendMessageRequest",
                SQS_QUEUE_NAME, QUEUE_NAME
            ));
            
            span.setStatus(StatusCode.OK);
            logger.debug("Message sent successfully to queue URL: {}", queueUrl);
            
        } catch (SqsException e) {
            sqsErrorCounter.add(1, Attributes.of(
                SQS_OPERATION, "SendMessageRequest",
                SQS_QUEUE_NAME, QUEUE_NAME,
                SQS_ERROR_CODE, e.awsErrorDetails().errorCode()
            ));
            
            span.setStatus(StatusCode.ERROR, "Failed to send message");
            span.setAttribute(SQS_ERROR_CODE, e.awsErrorDetails().errorCode());
            span.setAttribute(SQS_ERROR_MESSAGE, e.awsErrorDetails().errorMessage());
            
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            sqsOperationDuration.record(duration, Attributes.of(
                SQS_OPERATION, "SendMessageRequest",
                SQS_QUEUE_NAME, QUEUE_NAME
            ));
            span.end();
        }
    }

}
