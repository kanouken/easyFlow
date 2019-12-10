package io.kanouken.easyflow;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.kanouken.easyflow.JsonFlowReader.JsonFlow;
import io.kanouken.easyflow.JsonFlowReader.JsonFlowNode;
import io.kanouken.easyflow.model.EasyFlowInstance;
import io.kanouken.easyflow.model.EasyFlowTask;
import io.kanouken.easyflow.model.IEasyFlowBusinessService;
import io.kanouken.easyflow.model.IEasyFlowEntity;
import io.kanouken.easyflow.model.dto.EasyFlowTaskListDto;
import io.kanouken.easyflow.repository.EasyFlowInstanceRepository;
import io.kanouken.easyflow.repository.EasyFlowTaskRepository;
import io.kanouken.easyflow.user.IEasyFlowUser;

@Component
public class EasyFlowEngine {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	IEasyFlowUser user;

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
	public EasyFlowInstance start(JsonFlow flow, EasyFlowContext context, String businessKey) {
		EasyFlowInstance instance = new EasyFlowInstance();
		instance.setFlow(flow);
		instance.setCreateTime(new Date());
		instance.setUpdateTime(new Date());
		instance.setIsDone(Byte.valueOf("0"));
		instance.setBusinesskey(businessKey);
		instance.setCreateBy(user.getCurrentUsername());
		List<JsonFlowNode> nodes = flow.getNodes();
		// skip first node
		String nextNodeName = nodes.get(1).getNextNode();
		JsonFlowNode nextNode = this.filterNode(flow, nextNodeName);
		String type = nextNode.getType();
		Integer assignment = null;
		if (type.equals("gateway")) {
			// FIXME first node must not be the gateway
		} else {
			Integer assignmentIds = this.evalAssignments(nextNode.getAssignments(), context.getFacts());
			assignment = assignmentIds;
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
		task.setNodeDescription(nextNode.getDescription());
		task.setVars((Map<String, Object>) context.getFacts().get(EF_VARS));
		this.taskRepository.save(task);

		return instance;
	}

	/**
	 * 完成任务 使用gateway 来解决 驳回的场景 ，通过流程变量判断去掉上一个环节
	 * 
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
				// 根据网关选取下一个节点
				nextNode = this.gatewayChoose(instance, context, nextNode);
			}
			// task
			Integer assignmentIds = this.evalAssignments(nextNode.getAssignments(), context.getFacts());
			assignment = assignmentIds;
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

	private JsonFlowNode filterNode(JsonFlow flows, String nodeName) {

		Optional<JsonFlowNode> findFirst = flows.getNodes().stream().filter(f -> f.getName().equals(nodeName))
				.findFirst();

		if (findFirst.isPresent()) {
			return findFirst.get();
		} else {
			throw new RuntimeException("找不到节点");
		}
	}

	private Boolean evalCondition(String expression, Map<String, Object> context) {
		Boolean result = (Boolean) MVEL.eval(expression, context);
		return result;
	}

	private Integer evalAssignments(String expression, Map<String, Object> context) {
		Integer eval = MVEL.eval(expression, context, Integer.class);
		return eval;
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

	/**
	 * 根据办理人 + 流程key 查询办理事项 包含 已办 和待办
	 * 
	 * @param assignment
	 *            办理人
	 * @param flowKey
	 *            流程key
	 * @return
	 */
	public List<EasyFlowTaskListDto> queryTask(Integer assignment, String flowKey) {
		List<EasyFlowTaskListDto> result = new ArrayList<EasyFlowTaskListDto>();
		// 根据流程key 查询流程实例
		List<EasyFlowInstance> instances = this.instanceRepository.findByFlowKey(flowKey);
		if (CollectionUtils.isNotEmpty(instances)) {

		}
		List<String> instanceIds = instances.stream().map(EasyFlowInstance::getId).collect(Collectors.toList());

		List<Tuple> tasksTuple = this.taskRepository.findByAssignmentAndInstanceIdInOrderByCreateTimeDesc(assignment,
				instanceIds);

		if (CollectionUtils.isNotEmpty(tasksTuple)) {
			EasyFlowTaskListDto taskListDto = null;
			for (Tuple tuple : tasksTuple) {
				taskListDto = new EasyFlowTaskListDto();
				taskListDto.setId(tuple.get(0, String.class));
				taskListDto.setInstanceId(tuple.get(1, String.class));
				taskListDto.setIsDone(tuple.get(2, Byte.class));
				taskListDto.setNodeName(tuple.get(3, String.class));
				taskListDto.setNodeDescription(tuple.get(4, String.class));
				taskListDto.setFlowKey(tuple.get(5, String.class));
				taskListDto.setFlowName(tuple.get(6, String.class));
				taskListDto.setFormUrl(tuple.get(7, String.class));
				taskListDto.setPublisher(tuple.get(8, String.class));
				taskListDto.setCreateTime(tuple.get(9, Date.class));
				result.add(taskListDto);
			}
		}
		return result;

	}

	/**
	 * 根据流程实ids 例查询 实例列表
	 * 
	 * @param instanceIds
	 * @return
	 */
	public List<EasyFlowInstance> queryFlowInstance(List<String> instanceIds) {
		return this.instanceRepository.findByIdIn(instanceIds);
	}

	/**
	 * 查询流程实例
	 * 
	 * @param id
	 * @return
	 */
	public Optional<EasyFlowInstance> queryInstance(String id) {
		return this.instanceRepository.findById(id);
	}

	// 泛型 直接返回业务对象
	public <T> List<T> queryInstance(String nodeName, IEasyFlowBusinessService<T> service) {
		List<EasyFlowInstance> instances = this.instanceRepository.findByNodeName(nodeName);
		return service.transformToBusiness(instances);

	}
}
