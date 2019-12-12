package io.kanouken.easyflow;

import java.io.InputStream;

import io.kanouken.easyflow.JsonFlowReader.JsonFlow;

public class JsonFlowFactory {

	private JsonFlowReader reader;

	public JsonFlowFactory(JsonFlowReader reader) {
		this.reader = reader;
	}

	public JsonFlow createFlow(InputStream is) {
		return this.reader.read(is);
	}
}
