
package io.kanouken.easyflow;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.Tuple;
import javax.transaction.TransactionScoped;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.kanouken.easyflow.JsonFlowReader.JsonFlow;
import io.kanouken.easyflow.JsonFlowReader.JsonFlowForm;
import io.kanouken.easyflow.JsonFlowReader.JsonFlowFormField;
import io.kanouken.easyflow.JsonFlowReader.JsonFlowNode;
import io.kanouken.easyflow.exception.WorkflowException;
import io.kanouken.easyflow.interceptor.IEasyTaskInterceptor;
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

	@Autowired(required = false)
	IEasyTaskInterceptor taskInterceptor;

	public static final String EF_RESULT = "complete_result";
	public static final String EF_APPROVAL_RESULT = "approvalStatus";
	public static final String EF_VARS = "vars";

    /**
     * 标记上一次是驳回
     */
    public static final String EF_VAR_LAST_REJECT = "var_last_reject";

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


    public static ObjectMapper om = new ObjectMapper();

    {
        om.configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, true);
        om.configure(JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS, true);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        om.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }


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
			nextNode = this.gatewayChoose(instance, context, nextNode);
		}
		List<Integer> assignmentIds = this.evalAssignments(nextNode.getAssignments(), context.getFacts());
		assignment = assignmentIds;
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
		firstTask.setType(autoCompleteNode.getType());
		this.taskRepository.save(firstTask);

		EasyFlowTask nextTask = null;
		EasyFlowClaim  nextClaim = null;
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
			claim.setType(nextNode.getType());
			nextClaim = claim;
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
			task.setType(nextNode.getType());
			nextTask = task;
			this.taskRepository.save(task);
		}
		
		if (taskInterceptor != null) {
			taskInterceptor.afterTask(firstTask, instance, context, nextTask, nextClaim);
		}

		return instance;
	}

	/**
	 * 完成任务 使用gateway 来解决 驳回的场景 ，通过流程变量判断去掉上一个环节
	 * 
	 * @param taskId
	 */
	@Transactional(rollbackFor = { Exception.class }, propagation = Propagation.REQUIRED)
	public EasyFlowInstance completeTask(String taskId, EasyFlowContext context) {
		context.put("flowUser", this.user);
		// 检查表单必填项
		EasyFlowTask task = this.taskRepository.findOne(taskId);
		if (task.getIsDone().equals(Byte.valueOf("1"))) {
			throw new WorkflowException("节点已审批 请勿重复操作！");
		}
		EasyFlowInstance instance = this.instanceRepository.findOne(task.getInstanceId());

		if(!instance.getStatus().equals(STATUS_RUNNING)){
			throw  new WorkflowException("流程实例已停止");
		}

		if (taskInterceptor != null) {
			taskInterceptor.beforeTask(task, instance, context);
		}
//		checkNodeFormParams(task, instance,context);
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
			if (taskInterceptor != null) {
				taskInterceptor.afterTask(task, instance, context, null, null);
			}
			// end
			return instance;
		} else if (nextNode.getType().equals("event")) {
			// 不需要生成任务
			assignment = Arrays.asList(0);
		} else {
			// task
			List<Integer> assignmentIds = this.evalAssignments(nextNode.getAssignments(), context.getFacts());
			assignment = assignmentIds;

		}
		//

		EasyFlowTask nextTask = null;
		EasyFlowClaim nextClaim = null;
		if (assignment != null) {

			if (assignment.size() > 1) {
				EasyFlowClaim claim = new EasyFlowClaim();
				claim.setCandidates(assignment);
				claim.setInstanceId(instance.getId());
				claim.setStatus(Byte.valueOf("0"));
				claim.setCreateTime(new Date());
				claim.setUpdateTime(new Date());
				claim.setNodeName(nextNode.getName());
				claim.setNodeDescription(nextNode.getDescription());
				claim.setVars((Map<String, Object>) context.getFacts().get(EF_VARS));
                //如果是驳回记录到新的任务中
                if(task.getApprovalStatus().equals(Byte.valueOf("0"))){
                    if(claim.getVars() == null){
                        claim.setVars(new HashMap<>());
                    }
                    claim.getVars().put(EF_VAR_LAST_REJECT,true);
                }
				claim.setType(nextNode.getType());
				this.claimRepo.save(claim);
				nextClaim = claim;
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
                //如果是驳回记录到新的任务中
                if(task.getApprovalStatus().equals(Byte.valueOf("0"))){
                    if(newTask.getVars() == null){
                        newTask.setVars(new HashMap<>());
                    }
                    newTask.getVars().put(EF_VAR_LAST_REJECT,true);
                }
				newTask.setType(nextNode.getType());
				this.taskRepository.save(newTask);
				nextTask = newTask;
			}
		}
		instance.setUpdateTime(new Date());
		instance.setCurrentNode(nextNode.getName());
		instance.setCurrentNodeDescription(nextNode.getDescription());
		this.instanceRepository.save(instance);

		if (taskInterceptor != null) {
			taskInterceptor.afterTask(task, instance, context, nextTask, nextClaim);
		}
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
	 * @param instance 流程实例
	 * @param context  流程context
	 * @param gateway  node 当前网关节点
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
	 * @param assignment 办理人
	 * @param flowKey    流程key
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
                Object var = tuple[11];
                if(var!= null){
                     Map<String,Object> varMap = null;
                     try {
                         varMap = om.readValue(String.valueOf(var), Map.class);
                     } catch (IOException e) {
                         throw new RuntimeException(e);
                     }
                     taskListDto.setVars(varMap);
                }
				result.add(taskListDto);
			}
		}

		return result;
	}


	/**
	 * 查询已办任务 分页
	 * @param assignment
	 * @param completeStart 完成开始时间
	 * @Param completeEnd  完成结束时间
	 * PageRequest pr = new PageRequest(page.getCurPage() - 1, page.getPerPageSum());
	 * @Param pr
	 */
	@Transactional(readOnly = true)
	public Page<EasyFlowTaskListDto> queryDoneTasksPaged(Integer assignment, Date completeStart , Date completeEnd,String flowName, String bizCode, Pageable pr  ){

		List<EasyFlowTaskListDto> result = new ArrayList<EasyFlowTaskListDto>();


			Page<Object[]> tasksTuple = this.taskRepository.findDonePaged(assignment,completeStart,completeEnd,flowName,bizCode,pr);

				EasyFlowTaskListDto taskListDto = null;
				for (Object[] tuple : tasksTuple.getContent()) {
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

		return new PageImpl<EasyFlowTaskListDto>(result,pr,tasksTuple.getTotalElements());
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
                Object var = tuple[15];
                if(var!= null){
                    Map<String,Object> varMap = null;
                    try {
                        varMap = om.readValue(String.valueOf(var), Map.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    taskListDto.setVars(varMap);
                }
				result.add(taskListDto);
			}
		}
		// fill name

		List<Integer> assignmentIds = result.stream().map(t -> t.getAssignment()).collect(Collectors.toList());

		Map<Integer, String> userNameMap = this.user.getUserName(assignmentIds);

		result.stream().forEach(t -> t.setAssignmentName(userNameMap.get(t.getAssignment())));

		return result;
	}

	/**
	 * 根据流程实例id 任务类型查询 不会返回执行人信息 谨慎调用
	 * 
	 * @param instanceId
	 * @param taskType
	 * @return
	 */
	@Transactional(readOnly = true)
	public List<EasyFlowTaskListDto> queryTask(String instanceId, String taskType) {
		List<EasyFlowTaskListDto> result = new ArrayList<EasyFlowTaskListDto>();

		List<EasyFlowTask> tasks = this.taskRepository.findByInstanceIdAndType(instanceId, taskType);

		if (CollectionUtils.isNotEmpty(tasks)) {
			EasyFlowTaskListDto taskListDto = null;
			for (EasyFlowTask t : tasks) {
				taskListDto = new EasyFlowTaskListDto();
				taskListDto.setId(t.getId());
				taskListDto.setInstanceId(t.getInstanceId());
				taskListDto.setIsDone(t.getIsDone());
				taskListDto.setNodeName(t.getNodeName());
				taskListDto.setNodeDescription(t.getNodeDescription());
				taskListDto.setApprovalStatus(t.getApprovalStatus());
//				taskListDto.setUpdateTime(t.getUpdateTime());
				taskListDto.setCompleteResult(t.getCompleteResult());
				taskListDto.setVars(t.getVars());
				result.add(taskListDto);
			}
		}
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
				claimListDto.setPublisher(String.valueOf(tuple[5]));
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
		// TODO 签收任务也要删除
		this.taskRepository.save(tasks);

		List<EasyFlowClaim> claims = this.claimRepo.findByInstanceId(flowInstanceId);
		if (CollectionUtils.isNotEmpty(claims)) {
			claims.forEach(c -> c.setIsDelete(Byte.valueOf("1")));
			this.claimRepo.save(claims);
		}
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
		if(taskInterceptor != null){
			taskInterceptor.afterFlowFinished(instance,context);
		}
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
		EasyFlowTask task = this.taskRepository.findOne(taskId);
		EasyFlowInstance instance = this.instanceRepository.findOne(old.getInstanceId());
		if (old.getStatus().equals(Byte.valueOf("1"))) {
			throw new RuntimeException("任务已签收");
		}

		// 取消的时候 还没点签收 ，签收单被一并删除。
		// 取消的时候 已经被签收了且生成了任务 。。任务和签收单一起被删除
		// 判断 流程是否被关闭
		if (instance.getStatus().equals(STATUS_ENDING)) {
			throw new RuntimeException("流程已结束");
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
		newTask.setType(old.getType());
		this.taskRepository.save(newTask);
		
		//任务签收后创建任务调用 任务创建拦截
		if (taskInterceptor != null) {
			taskInterceptor.afterTask(null, instance, null, newTask, null);
		}
	}

	/**
	 * 当前任务
	 * 
	 * @param task     流程实例
	 * @param instance 流程context
	 * @param context
	 */
	@SuppressWarnings("all")
	public void validateFormParams(String taskId, EasyFlowContext context) {

		EasyFlowTask task = this.taskRepository.findOne(taskId);
		EasyFlowInstance instance = this.instanceRepository.findOne(task.getInstanceId());
		// vars
		Map taskVars = (Map) context.getFacts().get(EF_VARS);
		if (MapUtils.isEmpty(taskVars)) {
			return;
		}
		JsonFlow flow = instance.getFlow();
		List<JsonFlowForm> forms = flow.getForms();
		if (CollectionUtils.isNotEmpty(forms)) {
			// target node
			String nodeName = task.getNodeName();
			JsonFlowForm targetForm = forms.stream().filter(d -> d.getRefNode().equals(nodeName)).findFirst()
					.orElse(null);
			if (null != targetForm) {
				List<JsonFlowFormField> fields = targetForm.getFields();
				if (CollectionUtils.isNotEmpty(fields)) {
					for (JsonFlowFormField jsonFlowFormField : fields) {
						if (jsonFlowFormField.getRequired()) {
							Object object = taskVars.get(jsonFlowFormField.getName());
							if (!isObjectNotEmpty(object)) {
								throw new WorkflowException(String.format("表单项 %s 是必填项！", StringUtils.defaultIfBlank(
										jsonFlowFormField.getDescription(), jsonFlowFormField.getName())));
							}
						}
					}
				}
			}
		}
	}

	public static Boolean isObjectNotEmpty(Object obj) {
		String str = Objects.toString(obj, "");
		return StringUtils.isNotBlank(str);
	}

	@Transactional(readOnly = true)
	public EasyFlowTask getTask(String taskId) {
		return this.taskRepository.findOne(taskId);
	}

	@Transactional(readOnly = true)
	public EasyFlowClaim getClaim(String claimId) {
		return this.claimRepo.findOne(claimId);
	}

	/**
	 * 查询待办任务数量 包括 待签收的任务
	 * 
	 * @param assignment
	 */
	@Transactional(readOnly = true)
	public Integer countTodoTask(Integer assignment) {
		Integer count = this.taskRepository.countByAssignmentAndIsDoneAndIsDelete(assignment, Byte.valueOf("0"),
				Byte.valueOf("0"));
		if (count == null) {
			count = 0;
		}
		Integer results = this.claimRepo.countByCandidaterAndStatus(assignment + "");
		Integer claimCount = null;
		claimCount = results == null ? 0 : results;
		return count + claimCount;

	}

}
