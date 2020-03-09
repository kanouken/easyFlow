package io.kanouken.easyflow.user;

import java.util.List;
import java.util.Map;

/**
 * 
 * @author Administrator
 *
 */
public interface IEasyFlowUser {

	String getCurrentUsername();

	Integer getCurrentUserid();

	public Map<Integer, String> getUserName(List<Integer> ids);
}
