package io.kanouken.easyflow;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonFlowReader {

	private ObjectMapper mapper;

	public JsonFlowReader(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses) {
		return this.mapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
	}

	public List<JsonFlowNode> read(FileReader reader) {
		try {
			JavaType javaType = getCollectionType(List.class, JsonFlowNode.class);
			return mapper.readValue(reader, javaType);
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static class JsonFlowNode {
		// 节点名称
		private String name;
		// 描述
		private String description;
		// 执行人 支持 spel 表达式
		private String assignments;
		/**
		 * task gateway start end
		 */
		private String type;

		/**
		 * { condition:'' nextNode:'' }
		 */
		private List<Map<String, Object>> gatewayConditions;

		/**
		 * 下一个节点
		 */
		private String nextNode;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getAssignments() {
			return assignments;
		}

		public void setAssignments(String assignments) {
			this.assignments = assignments;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public List<Map<String, Object>> getGatewayConditions() {
			return gatewayConditions;
		}

		public void setGatewayConditions(List<Map<String, Object>> gatewayConditions) {
			this.gatewayConditions = gatewayConditions;
		}

		public String getNextNode() {
			return nextNode;
		}

		public void setNextNode(String nextNode) {
			this.nextNode = nextNode;
		}

	}
}
