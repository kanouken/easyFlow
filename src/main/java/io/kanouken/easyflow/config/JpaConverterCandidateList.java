package io.kanouken.easyflow.config;

import java.util.List;

import javax.persistence.AttributeConverter;

import org.apache.commons.collections.CollectionUtils;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JpaConverterCandidateList implements AttributeConverter<List<Integer>, String> {

	public static ObjectMapper om = new ObjectMapper();

	{
		om.configure(Feature.WRITE_NUMBERS_AS_STRINGS, true);
		om.configure(Feature.QUOTE_NON_NUMERIC_NUMBERS, true);
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		om.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
		om.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

	}

	@Override
	public String convertToDatabaseColumn(List<Integer> meta) {
		try {
			if (CollectionUtils.isNotEmpty(meta)) {
				return om.writeValueAsString(meta);
			} else {
				return null;
			}
		} catch (JsonProcessingException ex) {
			return null;
		}
	}

	@Override
	public List<Integer> convertToEntityAttribute(String dbData) {
		try {
			if (dbData == null || dbData.equals("")) {
				return null;
			} else {
				return om.readValue(dbData, List.class);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

}