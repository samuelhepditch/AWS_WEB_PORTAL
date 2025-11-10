package com.mycompany.handler.service;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BedrockService {
    
    private static final Logger logger = LoggerFactory.getLogger(BedrockService.class);
    private static final Gson gson = new Gson();
    
    private final BedrockRuntimeClient client;
    private final String modelId;
    
    public BedrockService(BedrockRuntimeClient client, String modelId) {
        this.client = client;
        this.modelId = modelId;
    }
    
    public String invokeModel(String prompt) {
        try {
            // Build request payload for Claude
            Map<String, Object> payload = new HashMap<>();
            payload.put("anthropic_version", "bedrock-2023-05-31");
            payload.put("max_tokens", 1000);
            payload.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            
            String jsonPayload = gson.toJson(payload);
            logger.info("Invoking Bedrock model: {}", modelId);
            
            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .body(SdkBytes.fromUtf8String(jsonPayload))
                    .build();
            
            InvokeModelResponse response = client.invokeModel(request);
            String responseBody = response.body().asUtf8String();
            
            // Parse response
            Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");
            
            return (String) content.get(0).get("text");
            
        } catch (Exception e) {
            logger.error("Error invoking Bedrock model", e);
            throw new RuntimeException("Failed to invoke Bedrock", e);
        }
    }
}