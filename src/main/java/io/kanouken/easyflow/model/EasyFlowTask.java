package io.kanouken.easyflow.model;

import java.util.Date;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

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

	private Byte isDelete = Byte.valueOf("0");

	/**
	 * 节点名称
	 */
	private String nodeName;

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
	/**
	 * 办结状态
	 */
	private Byte approvalStatus;
	
	private String type;

	/**
	 * 变量
	 */
	@Convert(converter = JpaConverterMap.class)
	@Column(columnDefinition = "json")
	private Map<String, Object> vars;

	public Byte getIsDelete() {
		return isDelete;
	}

	public void setIsDelete(Byte isDelete) {
		this.isDelete = isDelete;
	}

	public String getNodeDescription() {
		return nodeDescription;
	}

	public Byte getApprovalStatus() {
		return approvalStatus;
	}

	public void setApprovalStatus(Byte approvalStatus) {
		this.approvalStatus = approvalStatus;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	

}
