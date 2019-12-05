package io.kanouken.easyflow.model;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

import io.kanouken.easyflow.JsonFlowReader.JsonFlowNode;
import io.kanouken.easyflow.config.JpaConverterFlowNodeList;

@Entity(name = "ef_instance")
public class EasyFlowInstance {

	@Id
	@GenericGenerator(name = "uuid", strategy = "uuid")
	@GeneratedValue(generator = "uuid")
	private String id;
	@Convert(converter = JpaConverterFlowNodeList.class)
	@Column(columnDefinition = "json")
	private List<JsonFlowNode> flow;
	private Date createTime;

	private Date updateTime;

	private Byte isDone;

	private String currentNode;

	private String currentNodeDescription;

	/**
	 * 表单路径
	 */
	private String formUrl;
	
	/**
	 * 业务编号
	 */
	private String bussinesskey;
	
	/**
	 * 流程key
	 */
	private String flowKey; 
	
	
	

	public String getBussinesskey() {
		return bussinesskey;
	}

	public void setBussinesskey(String bussinesskey) {
		this.bussinesskey = bussinesskey;
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

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public Byte getIsDone() {
		return isDone;
	}

	public void setIsDone(Byte isDone) {
		this.isDone = isDone;
	}

	public String getCurrentNode() {
		return currentNode;
	}

	public void setCurrentNode(String currentNode) {
		this.currentNode = currentNode;
	}

	public String getCurrentNodeDescription() {
		return currentNodeDescription;
	}

	public void setCurrentNodeDescription(String currentNodeDescription) {
		this.currentNodeDescription = currentNodeDescription;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<JsonFlowNode> getFlow() {
		return flow;
	}

	public void setFlow(List<JsonFlowNode> flow) {
		this.flow = flow;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

}
