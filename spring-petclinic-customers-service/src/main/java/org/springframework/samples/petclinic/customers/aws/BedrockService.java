package org.springframework.samples.petclinic.customers.aws;

import org.json.JSONObject;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Component
public class BedrockService {
    final String modelId = "anthropic.claude-3-haiku-20240307-v1:0";
    //    Region region;
    final BedrockRuntimeAsyncClient clientSync;
    final BedrockRuntimeClient client;

    public BedrockService() {
        clientSync = BedrockRuntimeAsyncClient.builder()
//                .credentialsProvider(ProfileCredentialsProvider.create())
                .region(Region.US_EAST_1)
                .build();

        client = BedrockRuntimeClient.builder()
                .region(Region.US_EAST_1)
//                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();
    }

    /**
     * Invokes Anthropic Claude 3 Haiku and processes the response stream.
     *
     * @param prompt The prompt for the model to complete.
     * @return A JSON object containing the complete response along with some metadata.
     */
    public String invokeModelWithResponseStream(String prompt) {
        // Prepare the JSON payload for the Messages API request
        JSONObject payload = new JSONObject()
                .put("anthropic_version", "bedrock-2023-05-31")
                .put("max_tokens", 1000)
                .append("messages", new JSONObject()
                        .put("role", "user")
                        .append("content", new JSONObject()
                                .put("type", "text")
                                .put("text", prompt)
                        ));

        // Create the request object using the payload and the model ID
        InvokeModelWithResponseStreamRequest request = InvokeModelWithResponseStreamRequest.builder()
                .contentType("application/json")
                .body(SdkBytes.fromUtf8String(payload.toString()))
                .modelId(modelId)
                .build();

        // Create a handler to print the stream in real-time and add metadata to a response object
        JSONObject structuredResponse = new JSONObject();
        InvokeModelWithResponseStreamResponseHandler handler = createMessagesApiResponseStreamHandler(structuredResponse);

        // Invoke the model with the request payload and the response stream handler
        clientSync.invokeModelWithResponseStream(request, handler).join();

        return structuredResponse.toString(2);
    }

    private InvokeModelWithResponseStreamResponseHandler createMessagesApiResponseStreamHandler(JSONObject structuredResponse) {
        AtomicReference<String> completeMessage = new AtomicReference<>("");

        Consumer<ResponseStream> responseStreamHandler = event -> event.accept(InvokeModelWithResponseStreamResponseHandler.Visitor.builder()
                .onChunk(c -> {
                    // Decode the chunk
                    JSONObject chunk = new JSONObject(c.bytes().asUtf8String());

                    // The Messages API returns different types:
                    String chunkType = chunk.getString("type");
                    if ("message_start".equals(chunkType)) {
                        // The first chunk contains information about the message role
                        String role = chunk.optJSONObject("message").optString("role");
                        structuredResponse.put("role", role);

                    } else if ("content_block_delta".equals(chunkType)) {
                        // These chunks contain the text fragments
                        String text = chunk.optJSONObject("delta").optString("text");
                        // Print the text fragment to the console ...
                        System.out.print(text);
                        // ... and append it to the complete message
                        completeMessage.getAndUpdate(current -> current + text);

                    } else if ("message_delta".equals(chunkType)) {
                        // This chunk contains the stop reason
                        String stopReason = chunk.optJSONObject("delta").optString("stop_reason");
                        structuredResponse.put("stop_reason", stopReason);

                    } else if ("message_stop".equals(chunkType)) {
                        // The last chunk contains the metrics
                        JSONObject metrics = chunk.optJSONObject("amazon-bedrock-invocationMetrics");
                        structuredResponse.put("metrics", new JSONObject()
                                .put("inputTokenCount", metrics.optString("inputTokenCount"))
                                .put("outputTokenCount", metrics.optString("outputTokenCount"))
                                .put("firstByteLatency", metrics.optString("firstByteLatency"))
                                .put("invocationLatency", metrics.optString("invocationLatency")));
                    }
                })
                .build());

        return InvokeModelWithResponseStreamResponseHandler.builder()
                .onEventStream(stream -> stream.subscribe(responseStreamHandler))
                .onComplete(() ->
                        // Add the complete message to the response object
                        structuredResponse.append("content", new JSONObject()
                                .put("type", "text")
                                .put("text", completeMessage.get())))
                .build();
    }

    public String invokeLlama2(String prompt) {
        /*
         * The different model providers have individual request and response formats.
         * For the format, ranges, and default values for Meta Llama 2 Chat, refer to:
         * https://docs.aws.amazon.com/bedrock/latest/userguide/model-parameters-meta.
         * html
         */

        String llama2ModelId = "meta.llama2-13b-chat-v1";

        String payload = new JSONObject()
                .put("prompt", prompt)
                .put("max_gen_len", 1000)
                .put("temperature", 0.5)
                .put("top_p", 0.9)
                .toString();

        InvokeModelRequest request = InvokeModelRequest.builder()
                .body(SdkBytes.fromUtf8String(payload))
                .modelId(llama2ModelId)
                .contentType("application/json")
                .accept("application/json")
                .build();
        System.out.println("invokeLlama2 request:" + request.body().asUtf8String());

        InvokeModelResponse response = client.invokeModel(request);

        JSONObject responseBody = new JSONObject(response.body().asUtf8String());
        System.out.println("invokeLlama2 response:" + response.body().asUtf8String());
        String generatedText = responseBody.getString("generation");
        System.out.println("prompt_token:" + responseBody.getInt("prompt_token_count"));

        return generatedText;
    }

    public String invokeTitanText(String prompt) {
        String llama2ModelId = "amazon.titan-text-express-v1";

        JSONObject textGenerationConfig = new JSONObject()
                .put("temperature", 0.5)
                .put("maxTokenCount", 1000)
                .put("topP", 0.9);
        String payload = new JSONObject()
                .put("inputText", prompt)
                .put("textGenerationConfig", textGenerationConfig)
                .toString();

        InvokeModelRequest request = InvokeModelRequest.builder()
                .body(SdkBytes.fromUtf8String(payload))
                .modelId(llama2ModelId)
                .contentType("application/json")
                .accept("application/json")
                .build();
        System.out.println("invokeTitanText request:" + request.body().asUtf8String());
        InvokeModelResponse response = client.invokeModel(request);

        JSONObject responseBody = new JSONObject(response.body().asUtf8String());
        System.out.println("invokeTitanText response:" + response.body().asUtf8String());
        JSONObject result = (JSONObject) responseBody.getJSONArray("results").get(0);
        System.out.println("invokeTitanText result:" +  result.toString());
        if (result.has("tokenCount")) {
            System.out.println("invokeTitanText token:" +  result.getInt("tokenCount"));
        }
        String generatedText = result.getString("outputText");
        return generatedText;
    }

    public String invokeAnthropicModel(String prompt) {

        // Set the model ID, e.g., Claude 3 Haiku.
        String modelId = "anthropic.claude-3-haiku-20240307-v1:0";

        // The InvokeModel API uses the model's native payload.
        // Learn more about the available inference parameters and response fields at:
        // https://docs.aws.amazon.com/bedrock/latest/userguide/model-parameters-anthropic-claude-messages.html
        ArrayList<JSONObject> contents = new ArrayList<JSONObject>();
        contents.add(new JSONObject().put("type", "text").put("text", prompt));

        JSONObject session1 = new JSONObject()
                .put("role", "user")
                .put("content", contents);
        ArrayList<JSONObject> enclosedPrompt = new ArrayList<>();

        enclosedPrompt.add(session1);
        String payload = new JSONObject()
                .put("anthropic_version", "bedrock-2023-05-31")
                .put("max_tokens", 2000)
                .put("temperature", 0.5)
//                .put("system", "")  // add on demand
                .put("top_p", 0.999)
//                .put("top_k", 250)
                .put("messages", enclosedPrompt)
                .toString();


        InvokeModelRequest request = InvokeModelRequest.builder()
                .body(SdkBytes.fromUtf8String(payload))
                .modelId(modelId)
                .contentType("application/json")
                .accept("application/json")
                .build();
        System.out.println("invokeAnthropicText request:" + request.body().asUtf8String());
        // Encode and send the request to the Bedrock Runtime.
        InvokeModelResponse response = client.invokeModel(request);

        JSONObject responseBody = new JSONObject(response.body().asUtf8String());
        System.out.println("invokeAnthropicText response:" + response.body().asUtf8String());


        // Decode the response body.
//            JSONObject responseBody = new JSONObject(response.body().asUtf8String());
//
//            // Retrieve the generated text from the model's response.
//            var text = new JSONPointer("/content/0/text").queryFrom(responseBody).toString();
//            System.out.println(text);

        return response.body().asUtf8String();
    }

    public static void main(String[] args) {
        BedrockService service = new BedrockService();
//        String resp = service.invokeTitanText("Hi, how are you?.");
//        System.out.println(resp);
//        String resp = service.invokeLlama2("Hi, how are you?.");
//        System.out.println(resp);
        String resp = service.invokeAnthropicModel("Hi, how are you?.");
        System.out.println(resp);

    }

}
