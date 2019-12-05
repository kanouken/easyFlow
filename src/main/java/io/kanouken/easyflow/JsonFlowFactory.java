package io.kanouken.easyflow;

import java.io.FileReader;
import java.util.List;

import io.kanouken.easyflow.JsonFlowReader.JsonFlowNode;

public class JsonFlowFactory {

	private JsonFlowReader reader;

	public JsonFlowFactory(JsonFlowReader reader) {
		this.reader = reader;
	}

	public List<JsonFlowNode> createFlow(FileReader reader) {
		return this.reader.read(reader);
	}
}
