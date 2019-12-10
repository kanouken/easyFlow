package io.kanouken.easyflow;

import java.io.FileReader;

import io.kanouken.easyflow.JsonFlowReader.JsonFlow;

public class JsonFlowFactory {

	private JsonFlowReader reader;

	public JsonFlowFactory(JsonFlowReader reader) {
		this.reader = reader;
	}

	public JsonFlow createFlow(FileReader reader) {
		return this.reader.read(reader);
	}
}
