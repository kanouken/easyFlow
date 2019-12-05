package io.kanouken.easyflow;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.transaction.Transactional;

import org.apache.commons.collections4.MapUtils;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.kanouken.easyflow.JsonFlowReader.JsonFlowNode;
import io.kanouken.easyflow.model.EasyFlowInstance;
import io.kanouken.easyflow.model.EasyFlowTask;
import io.kanouken.easyflow.repository.EasyFlowInstanceRepository;
import io.kanouken.easyflow.repository.EasyFlowTaskRepository;

@Component
public class EasyFlowEngine {

	public static final String EF_RESULT = "complete_result";

	public static final String EF_VARS = "vars";

	@Autowired
	EasyFlowInstanceRepository instanceRepository;

	@Autowired
	EasyFlowTaskRepository taskRepository;

	/**
	 * start
	 * 
	 * @param flow
	 * @param context
	 * @return
	 */
	@Transactional
	public EasyFlowInstance start(List<JsonFlowNode> flow, EasyFlowContext context) {
		EasyFlowInstance instance = new EasyFlowInstance();
		instance.setFlow(flow);
		instance.setCreateTime(new Date());
		instance.setUpdateTime(new Date());
		instance.setIsDone(Byte.valueOf("0"));

		// skip start node
		String nextNodeName = flow.get(0).getNextNode();
		JsonFlowNode nextNode = this.filterNode(flow, nextNodeName);
		String type = nextNode.getType();
		Integer assignment = null;
		if (type.equals("gateway")) {
			// this.gatewayChoose(instance,nextNode);
		} else {
			List<Integer> assignmentIds = this.evalAssignments(nextNode.getAssignments(), context.getFacts());
			assignment = assignmentIds.get(0);
		}

		instance.setCurrentNode(nextNode.getName());
		instance.setCurrentNodeDescription(nextNode.getDescription());
		this.instanceRepository.save(instance);

		// createTask
		// 不支持签收
		EasyFlowTask task = new EasyFlowTask();
		task.setAssignment(assignment);
		task.setInstanceId(instance.getId());
		task.setIsDone(Byte.valueOf("0"));
		task.setCreateTime(new Date());
		task.setNodeName(nextNode.getName());
		task.setVars((Map<String, Object>) context.getFacts().get(EF_VARS));
		this.taskRepository.save(task);

		return instance;
	}

	/**
	 * 完成任务
	 * 使用gateway 来解决 驳回的场景 ，通过流程变量判断去掉上一个环节
	 * @param task
	 */
	public EasyFlowInstance completeTask(String taskId, EasyFlowContext context) {
		EasyFlowTask task = this.taskRepository.findById(taskId).get();
		EasyFlowInstance instance = this.instanceRepository.findById(task.getInstanceId()).get();
		JsonFlowNode currentNode = this.filterNode(instance.getFlow(), task.getNodeName());
		JsonFlowNode nextNode = this.filterNode(instance.getFlow(), currentNode.getNextNode());
		task.setIsDone(Byte.valueOf("1"));
		task.setCompleteResult((String) context.get(EF_RESULT));
		task.setUpdateTime(new Date());
		// save currentTask
		this.taskRepository.save(task);
		Integer assignment = null;
		if (nextNode.getType().equals("end")) {
			instance.setUpdateTime(new Date());
			instance.setIsDone(Byte.valueOf("1"));
			this.instanceRepository.save(instance);
			// end
			return instance;
		} else {
			if (nextNode.getType().equals("gateway")) {
				//根据网关选取下一个节点
				nextNode = this.gatewayChoose(instance, context, nextNode);
			}
			// task
			List<Integer> assignmentIds = this.evalAssignments(nextNode.getAssignments(), context.getFacts());
			assignment = assignmentIds.get(0);
		}
		// 不支持签收！！！！！
		task.setId(null);
		task.setCreateTime(new Date());
		task.setUpdateTime(new Date());
		task.setAssignment(assignment);
		task.setIsDone(Byte.valueOf("0"));
		task.setVars((Map<String, Object>) context.get(EF_VARS));
		this.taskRepository.save(task);
		instance.setUpdateTime(new Date());
		instance.setCurrentNode(nextNode.getName());
		instance.setCurrentNodeDescription(nextNode.getDescription());
		this.instanceRepository.save(instance);

		return instance;
	}

	private JsonFlowNode filterNode(List<JsonFlowNode> flows, String nodeName) {

		Optional<JsonFlowNode> findFirst = flows.stream().filter(f -> f.getName().equals(nodeName)).findFirst();

		if (findFirst.isPresent()) {
			return findFirst.get();
		} else {
			throw new RuntimeException("找不到节点");
		}
	}

	private Boolean evalCondition(String expression, Map<String, Object> context) {
		// We know this expression should return a boolean.
		Boolean result = (Boolean) MVEL.eval(expression, context);
		return result;
	}

	private List<Integer> evalAssignments(String expression, Map<String, Object> context) {
		// We know this expression should return a boolean.
		List<Integer> assignments = (List<Integer>) MVEL.eval(expression, context);
		return assignments;
	}

	/**
	 * 
	 * @param instance
	 *            流程实例
	 * @param context
	 *            流程context
	 * @param gateway
	 *            node 当前网关节点
	 */
	private JsonFlowNode gatewayChoose(EasyFlowInstance instance, EasyFlowContext context, JsonFlowNode node) {
		List<Map<String, Object>> gatewayConditions = node.getGatewayConditions();
		// {
		// "condition": "days>3",
		// "nextNode": "node3"
		// }
		JsonFlowNode nextNode = null;
		for (Map<String, Object> condition : gatewayConditions) {

			Boolean result = this.evalCondition(MapUtils.getString(condition, "condition"), context.getFacts());
			if (result) {
				nextNode = this.filterNode(instance.getFlow(), MapUtils.getString(condition, "nextNode"));
				break;
			}
		}
		if (nextNode == null) {
			throw new RuntimeException("gateway exception");
		}
		return nextNode;

	}
}
