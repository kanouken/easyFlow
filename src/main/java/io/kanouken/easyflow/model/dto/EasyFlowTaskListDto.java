package io.kanouken.easyflow.model.dto;

import java.util.Date;

public class EasyFlowTaskListDto {

	private String id;
	private String flowKey;
	private String formUrl;
	private String nodeName;
	private String nodeDescription;
	private String instanceId;
	private String flowName;
	// 发起人
	private String publisher;
	// 执行人 id
	private String assignment;

	private String businesskey;

	private Byte isDone;

	public String getBusinesskey() {
		return businesskey;
	}

	public void setBusinesskey(String businesskey) {
		this.businesskey = businesskey;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFlowKey() {
		return flowKey;
	}

	public void setFlowKey(String flowKey) {
		this.flowKey = flowKey;
	}

	public String getFormUrl() {
		return formUrl;
	}

	public void setFormUrl(String formUrl) {
		this.formUrl = formUrl;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getNodeDescription() {
		return nodeDescription;
	}

	public void setNodeDescription(String nodeDescription) {
		this.nodeDescription = nodeDescription;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getFlowName() {
		return flowName;
	}

	public void setFlowName(String flowName) {
		this.flowName = flowName;
	}

	public String getPublisher() {
		return publisher;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public String getAssignment() {
		return assignment;
	}

	public void setAssignment(String assignment) {
		this.assignment = assignment;
	}

	public Byte getIsDone() {
		return isDone;
	}

	public void setIsDone(Byte isDone) {
		this.isDone = isDone;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getCompleteResult() {
		return completeResult;
	}

	public void setCompleteResult(String completeResult) {
		this.completeResult = completeResult;
	}

	private Date createTime;
	private Date updateTime;

	private String completeResult;

}
