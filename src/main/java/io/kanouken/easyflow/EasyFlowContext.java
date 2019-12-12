package io.kanouken.easyflow;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EasyFlowContext {
	private Map<String, Object> facts = new HashMap<String, Object>();

	public Object put(String name, Object fact) {
		Objects.requireNonNull(name);
		return facts.put(name, fact);
	}

	public Map<String, Object> getFacts() {

		return this.facts;
	}

	public Object get(String name) {
		return facts.get(name);
	}

	public void putResult(String result) {
		this.facts.put(EasyFlowEngine.EF_RESULT, result);
	}

	public void putApprovalStatus(Byte status) {
		this.facts.put(EasyFlowEngine.EF_APPROVAL_RESULT, status);
	}

	public void putVars(Map<String, Object> vars) {
		this.facts.put(EasyFlowEngine.EF_VARS, vars);
	}
}
