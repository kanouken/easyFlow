package io.kanouken.easyflow.model;

import java.util.Date;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;

import io.kanouken.easyflow.config.JpaConverterMap;

@Entity(name = "ef_task")
public class EasyFlowTask {

	@Id
	@GenericGenerator(name = "uuid", strategy = "uuid")
	@GeneratedValue(generator = "uuid")
	private String id;
	/**
	 * 流程實例 id
	 */
	private String instanceId;

	private Date createTime;

	private Date updateTime;

	/**
	 * 节点名称
	 */
	private String nodeName;

	@Transient
	private String flowKey;
	@Transient
	private String flowName;

	/**
	 * 节点描述
	 */
	private String nodeDescription;

	/**
	 * 执行人 id
	 */
	private Integer assignment;

	/***
	 * 办结意见
	 */
	private String completeResult;

	private Byte isDone;

	public String getFlowKey() {
		return flowKey;
	}

	public void setFlowKey(String flowKey) {
		this.flowKey = flowKey;
	}

	public String getFlowName() {
		return flowName;
	}

	public void setFlowName(String flowName) {
		this.flowName = flowName;
	}

	/**
	 * 变量
	 */
	@Convert(converter = JpaConverterMap.class)
	@Column(columnDefinition = "json")
	private Map<String, Object> vars;

	public String getNodeDescription() {
		return nodeDescription;
	}

	public void setNodeDescription(String nodeDescription) {
		this.nodeDescription = nodeDescription;
	}

	public Map<String, Object> getVars() {
		return vars;
	}

	public void setVars(Map<String, Object> vars) {
		this.vars = vars;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Integer getAssignment() {
		return assignment;
	}

	public void setAssignment(Integer assignment) {
		this.assignment = assignment;
	}

	public String getCompleteResult() {
		return completeResult;
	}

	public void setCompleteResult(String completeResult) {
		this.completeResult = completeResult;
	}

	public Byte getIsDone() {
		return isDone;
	}

	public void setIsDone(Byte isDone) {
		this.isDone = isDone;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

}
