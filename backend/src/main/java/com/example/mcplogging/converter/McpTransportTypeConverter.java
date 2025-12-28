package com.example.mcplogging.converter;

import com.example.mcplogging.enums.McpTransportType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class McpTransportTypeConverter implements AttributeConverter<McpTransportType, String> {

    @Override
    public String convertToDatabaseColumn(McpTransportType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public McpTransportType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        try {
            return McpTransportType.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown McpTransportType: " + dbData, e);
        }
    }
}
