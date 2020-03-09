package io.kanouken.easyflow;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonFlowReader {

	private ObjectMapper mapper;

	public JsonFlowReader(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	// public JavaType getCollectionType(Class<?> collectionClass, Class<?>...
	// elementClasses) {
	// return
	// this.mapper.getTypeFactory().constructParametricType(collectionClass,
	// elementClasses);
	// }

	public JsonFlow read(InputStream is) {
		try {
			return mapper.readValue(is, JsonFlow.class);
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public static class JsonFlow {

		private String name;
		private String key;
		private String description;
		private String formUrl;

		public String getFormUrl() {
			return formUrl;
		}

		public void setFormUrl(String formUrl) {
			this.formUrl = formUrl;
		}

		private List<JsonFlowNode> nodes;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public List<JsonFlowNode> getNodes() {
			return nodes;
		}

		public void setNodes(List<JsonFlowNode> nodes) {
			this.nodes = nodes;
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
