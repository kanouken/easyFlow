package io.kanouken.easyflow.config;

import java.io.IOException;
import java.util.Map;

import javax.persistence.AttributeConverter;

import org.apache.commons.collections.MapUtils;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JpaConverterMap implements AttributeConverter<Map<String, Object>, String> {

	public static ObjectMapper om = new ObjectMapper();

	{
		om.configure(Feature.WRITE_NUMBERS_AS_STRINGS, true);
		om.configure(Feature.QUOTE_NON_NUMERIC_NUMBERS, true);
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		om.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
		om.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
	}

	public Map<String, Object> convertToEntityAttribute(String dbData) {
		try {
			if (dbData == null) {
				return null;
			}
			return om.readValue(dbData, Map.class);
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
		return om.getTypeFactory().constructParametricType(collectionClass, elementClasses);
	}

	public String convertToDatabaseColumn(Map<String, Object> attribute) {
		try {
			if (MapUtils.isNotEmpty(attribute)) {
				return om.writeValueAsString(attribute);
			} else {
				return null;
			}
		} catch (JsonProcessingException ex) {
			return null;
		}
	}
}