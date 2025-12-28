package com.example.mcplogging.converter;

import com.example.mcplogging.entity.McpStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class McpStatusConverter implements AttributeConverter<McpStatus, String> {

    @Override
    public String convertToDatabaseColumn(McpStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public McpStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        try {
            return McpStatus.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown McpStatus: " + dbData, e);
        }
    }
}
