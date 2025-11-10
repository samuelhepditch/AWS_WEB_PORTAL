package main.java.com.mycompany;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.rdsdata.RdsDataClient;

import java.util.HashMap;
import java.util.Map;

public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiHandler.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Environment variables
    private final String dbClusterArn;
    private final String dbSecretArn;
    private final String bedrockModelId;
    private final Region region;
    
    // AWS Clients (initialized once per container)
    private final BedrockRuntimeClient bedrockClient;
    private final RdsDataClient rdsClient;
    
    public ApiHandler() {
        // Read environment variables
        this.dbClusterArn = System.getenv("DB_CLUSTER_ARN");
        this.dbSecretArn = System.getenv("DB_SECRET_ARN");
        this.bedrockModelId = System.getenv("BEDROCK_MODEL_ID");
        this.region = Region.of(System.getenv("AWS_REGION"));
        
        // Initialize AWS clients (reused across invocations)
        this.bedrockClient = BedrockRuntimeClient.builder()
                .region(region)
                .build();
                
        this.rdsClient = RdsDataClient.builder()
                .region(region)
                .build();
        
        logger.info("ApiHandler initialized with region: {}", region);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        logger.info("Received request: {} {}", input.getHttpMethod(), input.getPath());
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(getCorsHeaders());
        
        try {
            // Route based on path and method
            String path = input.getPath();
            String method = input.getHttpMethod();
            
            if ("/api/bedrock".equals(path) && "POST".equals(method)) {
                return handleBedrockRequest(input);
            } else if ("/api/data".equals(path) && "GET".equals(method)) {
                return handleDataRequest(input);
            } else {
                return createResponse(404, Map.of("error", "Not found"));
            }
            
        } catch (Exception e) {
            logger.error("Error processing request", e);
            return createResponse(500, Map.of("error", "Internal server error", "message", e.getMessage()));
        }
    }
    
    private APIGatewayProxyResponseEvent handleBedrockRequest(APIGatewayProxyRequestEvent input) {
        try {
            // Parse request body
            Map<String, Object> requestBody = gson.fromJson(input.getBody(), Map.class);
            String prompt = (String) requestBody.get("prompt");
            
            // Call Bedrock service
            BedrockService bedrockService = new BedrockService(bedrockClient, bedrockModelId);
            String result = bedrockService.invokeModel(prompt);
            
            return createResponse(200, Map.of("result", result));
            
        } catch (Exception e) {
            logger.error("Error invoking Bedrock", e);
            return createResponse(500, Map.of("error", "Failed to invoke Bedrock"));
        }
    }
    
    private APIGatewayProxyResponseEvent handleDataRequest(APIGatewayProxyRequestEvent input) {
        try {
            // Query parameters
            Map<String, String> queryParams = input.getQueryStringParameters();
            
            // Call database service
            DatabaseService dbService = new DatabaseService(rdsClient, dbClusterArn, dbSecretArn);
            List<Map<String, Object>> results = dbService.executeQuery("SELECT * FROM your_table LIMIT 10");
            
            return createResponse(200, Map.of("data", results));
            
        } catch (Exception e) {
            logger.error("Error querying database", e);
            return createResponse(500, Map.of("error", "Failed to query database"));
        }
    }
    
    private APIGatewayProxyResponseEvent createResponse(int statusCode, Map<String, Object> body) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(getCorsHeaders());
        response.setBody(gson.toJson(body));
        return response;
    }
    
    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        return headers;
    }
}