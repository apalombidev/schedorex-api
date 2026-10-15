package com.apalombidev.schedorex.service;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ServiceCurrencyConverter implements AttributeConverter<ServiceCurrency, String> {

	@Override
	public String convertToDatabaseColumn(ServiceCurrency attribute) {
		return attribute == null ? null : attribute.name();
	}

	@Override
	public ServiceCurrency convertToEntityAttribute(String dbData) {
		return dbData == null ? null : ServiceCurrency.valueOf(dbData.toUpperCase());
	}

}
