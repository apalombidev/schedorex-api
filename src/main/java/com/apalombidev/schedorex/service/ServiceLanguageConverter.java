package com.apalombidev.schedorex.service;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ServiceLanguageConverter implements AttributeConverter<ServiceLanguage, String> {
	
	@Override
	public String convertToDatabaseColumn(ServiceLanguage attribute) {
		return attribute == null ? null : attribute.name();
	}
	
	@Override
    public ServiceLanguage convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ServiceLanguage.valueOf(dbData.toUpperCase());
    }

}
