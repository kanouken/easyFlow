package io.kanouken.easyflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.Tuple;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.hibernate.type.TrueFalseType;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.kanouken.easyflow.JsonFlowReader.JsonFlow;
import io.kanouken.easyflow.JsonFlowReader.JsonFlowNode;
import io.kanouken.easyflow.model.EasyFlowClaim;
import io.kanouken.easyflow.model.EasyFlowInstance;
import io.kanouken.easyflow.model.EasyFlowTask;
import io.kanouken.easyflow.model.IEasyFlowBusinessService;
import io.kanouken.easyflow.model.dto.EasyFlowClaimListDto;
import io.kanouken.easyflow.model.dto.EasyFlowTaskListDto;
import io.kanouken.easyflow.repository.EasyFlowClaimRepository;
import io.kanouken.easyflow.repository.EasyFlowInstanceRepository;
import io.kanouken.easyflow.repository.EasyFlowTaskRepository;
import io.kanouken.easyflow.user.IEasyFlowUser;

@Component
public class EasyFlowEngine {

	@Autowired
	IEasyFlowUser user;

	public static final String EF_RESULT = "complete_result";
	public static final String EF_APPROVAL_RESULT = "approvalStatus";
	public static final String EF_VARS = "vars";

	public static final String NODE_TYPE_END = "end";

	public static final String NODE_TYPE_EVENT_SHUTDOWN = "shutdownEvent";

	public static final Byte STATUS_RUNNING = Byte.valueOf("1");

	public static final Byte STATUS_ENDING = Byte.valueOf("2");
	private static Object claimLock = new Object();

	@Autowired
	EasyFlowInstanceRepository instanceRepository;

	@Autowired
	EasyFlowTaskRepository taskRepository;

	@Autowired
	EasyFlowClaimRepository claimRepo;

	/**
	 * start
	 * 
	 * @param flow
	 * @param context
	 * @return
	 */
	@Transactional(rollbackFor = { Exception.class }, propagation = Propagation.REQUIRED)
	public EasyFlowInstance start(JsonFlow flow, EasyFlowContext context, String businessKey, Boolean skipFirstNode) {
		EasyFlowInstance instance = new EasyFlowInstance();
		instance.setFlow(flow);
		instance.setCreateTime(new Date());
		instance.setUpdateTime(new Date());
		instance.setStatus(STATUS_RUNNING);
		instance.setBusinesskey(businessKey);
		instance.setCreateBy(user.getCurrentUsername());
		instance.setCreateId(user.getCurrentUserid());
		List<JsonFlowNode> nodes = flow.getNodes();

		JsonFlowNode autoCompleteNode = nodes.get(1);
		// skip first node
		String nextNodeName = nodes.get(1).getNextNode();
		JsonFlowNode nextNode = this.filterNode(flow, nextNodeName);
		String type = nextNode.getType();
		List<Integer> assignment = null;
		if (type.equals("gateway")) {
			// FIXME first node must not be the gateway
		} else {
			List<Integer> assignmentIds = this.evalAssignments(nextNode.getAssignments(), context.getFacts());
			assignment = assignmentIds;
		}

		instance.setCurrentNode(nextNode.getName());
		instance.setCurrentNodeDescription(nextNode.getDescription());
		this.instanceRepository.save(instance);

		// auto complete first task
		EasyFlowTask firstTask = new EasyFlowTask();
		firstTask.setAssignment(Integer.valueOf(user.getCurrentUserid()));
		firstTask.setInstanceId(instance.getId());
		firstTask.setIsDone(Byte.valueOf("1"));// 已完成
		firstTask.setApprovalStatus(Byte.valueOf("1"));
		firstTask.setCreateTime(new Date());
		firstTask.setUpdateTime(new Date());
		firstTask.setNodeName(autoCompleteNode.getName());
		firstTask.setNodeDescription(autoCompleteNode.getDescription());
		this.taskRepository.save(firstTask);

		// createTask
		// 签收
		if (assignment != null && assignment.size() > 1) {
			EasyFlowClaim claim = new EasyFlowClaim();
			claim.setCandidates(assignment);
			claim.setInstanceId(instance.getId());
			claim.setStatus(Byte.valueOf("0"));
			claim.setCreateTime(new Date());
			claim.setUpdateTime(new Date());
			claim.setNodeName(nextNode.getName());
			claim.setNodeDescription(nextNode.getDescription());
			claim.setVars((Map<String, Object>) context.getFacts().get(EF_VARS));
			this.claimRepo.save(claim);
		} else {
			EasyFlowTask task = new EasyFlowTask();
			task.setAssignment(assignment.get(0));
			task.setInstanceId(instance.getId());
			task.setIsDone(Byte.valueOf("0"));
			task.setCreateTime(new Date());
			task.setUpdateTime(new Date());
			task.setNodeName(nextNode.getName());
			task.setNodeDescription(nextNode.getDescription());
			task.setVars((Map<String, Object>) context.getFacts().get(EF_VARS));
			this.taskRepository.save(task);
		}

		return instance;
	}

	/**
	 * 完成任务 使用gateway 来解决 驳回的场景 ，通过流程变量判断去掉上一个环节
	 * 
	 * @param task
	 */
	@Transactional(rollbackFor = { Exception.class }, propagation = Propagation.REQUIRED)
	public EasyFlowInstance completeTask(String taskId, EasyFlowContext context) {
		context.put("flowUser", this.user);

		EasyFlowTask task = this.taskRepository.findOne(taskId);
		EasyFlowInstance instance = this.instanceRepository.findOne(task.getInstanceId());
		context.put("publisher", Arrays.asList(instance.getCreateId()));
		JsonFlowNode currentNode = this.filterNode(instance.getFlow(), task.getNodeName());
		JsonFlowNode nextNode = this.filterNode(instance.getFlow(), currentNode.getNextNode());
		task.setIsDone(Byte.valueOf("1"));
		task.setCompleteResult((String) context.get(EF_RESULT));
		task.setApprovalStatus((Byte) context.get(EF_APPROVAL_RESULT));
		task.setUpdateTime(new Date());
		// save currentTask
		this.taskRepository.save(task);
		List<Integer> assignment = null;

		// next node
		if (nextNode.getType().equals("gateway")) {
			// 根据网关选取下一个节点
			nextNode = this.gatewayChoose(instance, context, nextNode);
		}

		if (nextNode.getType().equals("end")) {
			instance.setUpdateTime(new Date());
			instance.setStatus(STATUS_ENDING);
			instance.setCurrentNode(nextNode.getName());
			instance.setCurrentNodeDescription(nextNode.getDescription());
			this.instanceRepository.save(instance);
			// end
			return instance;
		} else {
			// task
			List<Integer> assignmentIds = this.evalAssignments(nextNode.getAssignments(), context.getFacts());
			assignment = assignmentIds;

		}
		//

		if (assignment != null && assignment.size() > 1) {
			EasyFlowClaim claim = new EasyFlowClaim();
			claim.setCandidates(assignment);
			claim.setInstanceId(instance.getId());
			claim.setStatus(Byte.valueOf("0"));
			claim.setCreateTime(new Date());
			claim.setUpdateTime(new Date());
			claim.setNodeName(nextNode.getName());
			claim.setNodeDescription(nextNode.getDescription());
			claim.setVars((Map<String, Object>) context.getFacts().get(EF_VARS));
			this.claimRepo.save(claim);
		} else {
			EasyFlowTask newTask = new EasyFlowTask();
			newTask.setAssignment(assignment.get(0));
			newTask.setInstanceId(instance.getId());
			newTask.setIsDone(Byte.valueOf("0"));
			newTask.setCreateTime(new Date());
			newTask.setUpdateTime(new Date());
			newTask.setNodeName(nextNode.getName());
			newTask.setNodeDescription(nextNode.getDescription());
			newTask.setVars((Map<String, Object>) context.getFacts().get(EF_VARS));
			this.taskRepository.save(newTask);
		}

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

	private List<Integer> evalAssignments(String expression, Map<String, Object> context) {
		List<Integer> eval = MVEL.eval(expression, context, List.class);
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
	@Transactional(readOnly = true)
	public List<EasyFlowTaskListDto> queryTask(Integer assignment, String flowKey) {
		List<EasyFlowTaskListDto> result = new ArrayList<EasyFlowTaskListDto>();
		// 根据流程key 查询流程实例
		List<EasyFlowInstance> instances = this.instanceRepository.findByFlowKey(flowKey);
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
	@Transactional(readOnly = true)
	public List<EasyFlowInstance> queryFlowInstance(List<String> instanceIds) {
		return this.instanceRepository.findByIdIn(instanceIds);
	}

	/**
	 * 查询流程实例
	 * 
	 * @param id
	 * @return
	 */
	@Transactional(readOnly = true)
	public EasyFlowInstance queryInstance(String id) {
		return this.instanceRepository.findOne(id);
	}

	// 泛型 直接返回业务对象
	@Transactional(readOnly = true)
	public <T> List<T> queryInstance(String nodeName, IEasyFlowBusinessService<T> service) {
		// List<EasyFlowInstance> instances =
		// this.instanceRepository.findByNodeName(nodeName);
		// return service.transformToBusiness(instances);
		return null;

	}

	/**
	 * 查询所有待办
	 */
	@Transactional(readOnly = true)
	public List<EasyFlowTaskListDto> queryTasksAndStatus(Integer assignment, Byte status) {

		List<EasyFlowTaskListDto> result = new ArrayList<EasyFlowTaskListDto>();

		List<Object[]> tasksTuple = this.taskRepository.findByAssignmentAndStatusOrderByCreateTimeDesc(assignment,
				status);

		if (CollectionUtils.isNotEmpty(tasksTuple)) {
			EasyFlowTaskListDto taskListDto = null;
			for (Object[] tuple : tasksTuple) {
				taskListDto = new EasyFlowTaskListDto();
				taskListDto.setId(String.valueOf(tuple[0]));
				taskListDto.setInstanceId(String.valueOf(tuple[1]));
				taskListDto.setIsDone((Byte) tuple[2]);
				taskListDto.setNodeName(String.valueOf(tuple[3]));
				taskListDto.setNodeDescription(String.valueOf(tuple[4]));
				taskListDto.setFlowKey(String.valueOf(tuple[5]));
				taskListDto.setFlowName(String.valueOf(tuple[6]));
				taskListDto.setFormUrl(String.valueOf(tuple[7]));
				taskListDto.setPublisher(String.valueOf(tuple[8]));
				taskListDto.setCreateTime(String.valueOf(tuple[9]));
				taskListDto.setBusinesskey(String.valueOf(tuple[10]));
				result.add(taskListDto);
			}
		}

		return result;
	}

	/**
	 * 查询流程所有任务 顺序 最后一条数据是当前任务
	 * 
	 * @param instanceId
	 * @return
	 */
	@Transactional(readOnly = true)
	public List<EasyFlowTaskListDto> queryTask(String instanceId) {
		List<EasyFlowTaskListDto> result = new ArrayList<EasyFlowTaskListDto>();

		List<Object[]> tasksTuple = this.taskRepository.findByInstanceIdOrderByCreateTimeAsc(instanceId);

		if (CollectionUtils.isNotEmpty(tasksTuple)) {
			EasyFlowTaskListDto taskListDto = null;
			for (Object[] tuple : tasksTuple) {
				taskListDto = new EasyFlowTaskListDto();
				taskListDto.setId(String.valueOf(tuple[0]));
				taskListDto.setInstanceId(String.valueOf(tuple[1]));
				taskListDto.setIsDone((Byte) tuple[2]);
				taskListDto.setNodeName(String.valueOf(tuple[3]));
				taskListDto.setNodeDescription(String.valueOf(tuple[4]));

				taskListDto.setApprovalStatus(tuple[5] != null ? (Byte) tuple[5] : null);
				if (tuple[6] != null) {
					taskListDto.setUpdateTime(tuple[6].toString());
				} else {
					taskListDto.setUpdateTime("");
				}

				taskListDto.setCompleteResult(tuple[7] != null ? tuple[7].toString() : "");
				taskListDto.setFlowKey(String.valueOf(tuple[8]));
				taskListDto.setFlowName(String.valueOf(tuple[9]));
				taskListDto.setFormUrl(String.valueOf(tuple[10]));
				taskListDto.setPublisher(String.valueOf(tuple[11]));
				taskListDto.setCreateTime(String.valueOf(tuple[12]));
				taskListDto.setBusinesskey(tuple[13].toString());
				taskListDto.setAssignment(Integer.valueOf(tuple[14].toString()));
				result.add(taskListDto);
			}
		}
		// fill name

		List<Integer> assignmentIds = result.stream().map(t -> t.getAssignment()).collect(Collectors.toList());

		Map<Integer, String> userNameMap = this.user.getUserName(assignmentIds);

		result.stream().forEach(t -> t.setAssignmentName(userNameMap.get(t.getAssignment())));

		return result;
	}

	@Transactional(readOnly = true)
	public List<EasyFlowClaimListDto> queryClaim(Integer assignment) {

		List<EasyFlowClaimListDto> result = new ArrayList<EasyFlowClaimListDto>();

		List<Object[]> tasksTuple = this.claimRepo.findByCandidaterAndStatus(assignment + "");

		if (CollectionUtils.isNotEmpty(tasksTuple)) {
			EasyFlowClaimListDto claimListDto = null;
			for (Object[] tuple : tasksTuple) {
				claimListDto = new EasyFlowClaimListDto();
				claimListDto.setId(String.valueOf(tuple[0]));
				claimListDto.setNodeName(String.valueOf(tuple[1]));
				claimListDto.setNodeDescription(String.valueOf(tuple[2]));
				claimListDto.setFlowName(String.valueOf(tuple[3]));
				claimListDto.setCreateTime(String.valueOf(tuple[4]));
				result.add(claimListDto);
			}
		}

		return result;

	}

	/**
	 * 关闭流程
	 * 
	 * @see #{@link #shutdown(String)}
	 * @param flowInstanceId
	 */
	@Deprecated
	@Transactional(rollbackFor = { Exception.class }, propagation = Propagation.REQUIRED)
	public void shutdown(String flowInstanceId, String nodeName) {
		EasyFlowInstance old = this.instanceRepository.findOne(flowInstanceId);

		JsonFlowNode filterNode = this.filterNode(old.getFlow(), nodeName);

		old.setCurrentNode(filterNode.getName());
		old.setCurrentNodeDescription(filterNode.getDescription());
		old.setStatus(STATUS_ENDING);
		old.setUpdateTime(new Date());
		old.setUpdateBy(this.user.getCurrentUsername());
		// delete task
		List<EasyFlowTask> tasks = this.taskRepository.findByInstanceId(flowInstanceId);
		tasks.forEach(t -> t.setIsDelete(Byte.valueOf("1")));
		this.taskRepository.save(tasks);
	}

	/**
	 * 关闭流程 获取type 类型为 shutdownEvent 的节点 如果找不到就填充已取消
	 * 
	 * @param flowInstanceId
	 */
	@Transactional(rollbackFor = { Exception.class }, propagation = Propagation.REQUIRED)
	public void shutdown(String flowInstanceId) {
		EasyFlowInstance old = this.instanceRepository.findOne(flowInstanceId);
		JsonFlowNode filterNode = null;
		try {
			filterNode = this.filterNodeByType(old.getFlow(), NODE_TYPE_EVENT_SHUTDOWN);
		} catch (Exception e) {

		}
		if (null == filterNode) {
			// 填充默认停止流程时候的节点信息
			filterNode = new JsonFlowNode();
			filterNode.setDescription("已取消");
		}
		old.setCurrentNode(filterNode.getName());
		old.setCurrentNodeDescription(filterNode.getDescription());
		old.setStatus(STATUS_ENDING);
		old.setUpdateTime(new Date());
		old.setUpdateBy(this.user.getCurrentUsername());
		// delete task
		List<EasyFlowTask> tasks = this.taskRepository.findByInstanceId(flowInstanceId);
		tasks.forEach(t -> t.setIsDelete(Byte.valueOf("1")));
		this.taskRepository.save(tasks);
	}

	/**
	 * 
	 * 直接完成任务 。无审批记录
	 * 
	 * @param flow
	 * @param context
	 * @return
	 */
	@Transactional(rollbackFor = { Exception.class }, propagation = Propagation.REQUIRED)
	public EasyFlowInstance complete(JsonFlow flow, EasyFlowContext context, String businessKey) {
		EasyFlowInstance instance = new EasyFlowInstance();
		instance.setFlow(flow);
		instance.setCreateTime(new Date());
		instance.setUpdateTime(new Date());
		instance.setStatus(STATUS_ENDING);
		instance.setBusinesskey(businessKey);
		instance.setCreateBy(user.getCurrentUsername());
		instance.setCreateId(user.getCurrentUserid());
		// 最后完成任务点
		JsonFlowNode lastNode = this.filterNodeByType(flow, NODE_TYPE_END);
		instance.setCurrentNode(lastNode.getName());
		instance.setCurrentNodeDescription(lastNode.getDescription());
		this.instanceRepository.save(instance);
		return instance;
	}

	public JsonFlowNode filterNodeByType(JsonFlow flow, String nodeType) {
		Optional<JsonFlowNode> findFirst = flow.getNodes().stream().filter(f -> f.getType().equals(nodeType))
				.findFirst();
		if (findFirst.isPresent()) {
			return findFirst.get();
		} else {
			throw new RuntimeException("找不到节点");
		}
	}

	@Transactional(rollbackFor = { Exception.class }, propagation = Propagation.REQUIRED)
	public synchronized void claim(String taskId, Integer assignment) {
		EasyFlowClaim old = this.claimRepo.findOne(taskId);
		EasyFlowInstance instance = this.instanceRepository.findOne(old.getInstanceId());
		if (old.getStatus().equals(Byte.valueOf("1"))) {
			throw new RuntimeException("任务已签收");
		}
		old.setStatus(Byte.valueOf("1"));
		old.setClaimTime(new Date());
		old.setClaimer(assignment);
		this.claimRepo.save(old);
		// add task
		EasyFlowTask newTask = new EasyFlowTask();
		newTask.setAssignment(assignment);
		newTask.setInstanceId(instance.getId());
		newTask.setIsDone(Byte.valueOf("0"));
		newTask.setCreateTime(new Date());
		newTask.setUpdateTime(new Date());
		newTask.setNodeName(old.getNodeName());
		newTask.setNodeDescription(old.getNodeDescription());
		newTask.setVars(old.getVars());
		this.taskRepository.save(newTask);
	}
}
