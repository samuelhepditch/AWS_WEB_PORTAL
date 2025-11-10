package com.mycompany.handler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.rdsdata.RdsDataClient;
import software.amazon.awssdk.services.rdsdata.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    
    private final RdsDataClient client;
    private final String clusterArn;
    private final String secretArn;
    
    public DatabaseService(RdsDataClient client, String clusterArn, String secretArn) {
        this.client = client;
        this.clusterArn = clusterArn;
        this.secretArn = secretArn;
    }
    
    public List<Map<String, Object>> executeQuery(String sql) {
        try {
            logger.info("Executing query: {}", sql);
            
            ExecuteStatementRequest request = ExecuteStatementRequest.builder()
                    .resourceArn(clusterArn)
                    .secretArn(secretArn)
                    .sql(sql)
                    .build();
            
            ExecuteStatementResponse response = client.executeStatement(request);
            
            return convertRecordsToMaps(response.records(), response.columnMetadata());
            
        } catch (Exception e) {
            logger.error("Error executing query", e);
            throw new RuntimeException("Failed to execute query", e);
        }
    }
    
    private List<Map<String, Object>> convertRecordsToMaps(
            List<List<Field>> records, 
            List<ColumnMetadata> columns) {
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (List<Field> record : records) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 0; i < columns.size(); i++) {
                String columnName = columns.get(i).name();
                Field field = record.get(i);
                row.put(columnName, extractFieldValue(field));
            }
            results.add(row);
        }
        
        return results;
    }
    
    private Object extractFieldValue(Field field) {
        if (field.stringValue() != null) return field.stringValue();
        if (field.longValue() != null) return field.longValue();
        if (field.doubleValue() != null) return field.doubleValue();
        if (field.booleanValue() != null) return field.booleanValue();
        return null;
    }
}