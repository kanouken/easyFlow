package io.kanouken.easyflow.config;

import java.io.IOException;
import java.util.List;

import javax.persistence.AttributeConverter;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kanouken.easyflow.JsonFlowReader.JsonFlowNode;

public class JpaConverterFlowNodeList implements AttributeConverter<List<JsonFlowNode>, String> {

	public static ObjectMapper om = new ObjectMapper();

	{
		om.configure(Feature.WRITE_NUMBERS_AS_STRINGS, true);
		om.configure(Feature.QUOTE_NON_NUMERIC_NUMBERS, true);
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		om.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
		om.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
	}

	public List<JsonFlowNode> convertToEntityAttribute(String dbData) {
		try {
			if (dbData == null) {
				return null;
			}
			JavaType javaType = getCollectionType(List.class, JsonFlowNode.class);
			return om.readValue(dbData, javaType);
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
		return om.getTypeFactory().constructParametricType(collectionClass, elementClasses);
	}

	public String convertToDatabaseColumn(List<JsonFlowNode> attribute) {
		try {
			if (CollectionUtils.isNotEmpty(attribute)) {
				return om.writeValueAsString(attribute);
			} else {
				return null;
			}
		} catch (JsonProcessingException ex) {
			return null;
		}
	}
}