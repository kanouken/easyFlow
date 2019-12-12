package io.kanouken.easyflow;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.mvel2.MVEL;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kanouken.easyflow.JsonFlowReader.JsonFlow;
import io.kanouken.easyflow.model.EasyFlowInstance;

/**
 * for test
 * 
 * @author Administrator
 *
 */
public class Luancher {
	public static void main1(String[] args) throws FileNotFoundException {

	}

	public static void main(String[] args) {
		String expression = "userService.getUsers()";

		Map vars = new HashMap();
		vars.put("foobar", new Integer(100));
		// We know this expression should return a boolean.
		Boolean result = (Boolean) MVEL.eval(expression, vars);

		if (result.booleanValue()) {
			System.out.println("It works!");
		}
	}
}
