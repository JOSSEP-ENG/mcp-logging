package com.example.mcplogging.converter;

import com.example.mcplogging.enums.McpServerType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class McpServerTypeConverter implements AttributeConverter<McpServerType, String> {

    @Override
    public String convertToDatabaseColumn(McpServerType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public McpServerType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        try {
            return McpServerType.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown McpServerType: " + dbData, e);
        }
    }
}
