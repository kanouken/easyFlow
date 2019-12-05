package io.kanouken.easyflow;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mvel2.MVEL;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kanouken.easyflow.JsonFlowReader.JsonFlowNode;
import io.kanouken.easyflow.model.EasyFlowInstance;
import io.kanouken.easyflow.model.EasyFlowTask;

public class Luancher {
	public static void main1(String[] args) throws FileNotFoundException {

		ObjectMapper om = new ObjectMapper();

		om.configure(Feature.WRITE_NUMBERS_AS_STRINGS, true);
		om.configure(Feature.QUOTE_NON_NUMERIC_NUMBERS, true);
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		om.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
		om.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

		EasyFlowContext context = new EasyFlowContext();

		context.put("userService", null);
		JsonFlowReader reader = new JsonFlowReader(om);
		JsonFlowFactory factory = new JsonFlowFactory(reader);
		List<JsonFlowNode> flow = factory.createFlow(new FileReader("src/main/java/io/kanouken/easyflow/myFlow.json"));
		EasyFlowEngine engine = new EasyFlowEngine();

		// auto skip start node
		context.put(EasyFlowEngine.EF_RESULT, "通过");
		EasyFlowInstance instance = engine.completeTask("", context);

	}

	public static void main(String[] args) {
		String expression = "userService.getUsers()";

		Map vars = new HashMap();
		vars.put("foobar", new Integer(100));
		vars.put("userService", new UserService());
		// We know this expression should return a boolean.
		Boolean result = (Boolean) MVEL.eval(expression, vars);

		if (result.booleanValue()) {
			System.out.println("It works!");
		}
	}
}
