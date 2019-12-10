package io.kanouken.easyflow.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

import io.kanouken.easyflow.JsonFlowReader.JsonFlow;
import io.kanouken.easyflow.config.JpaConverterFlowNodeList;

@Entity(name = "ef_instance")
public class EasyFlowInstance {

	@Id
	@GenericGenerator(name = "uuid", strategy = "uuid")
	@GeneratedValue(generator = "uuid")
	private String id;
	@Convert(converter = JpaConverterFlowNodeList.class)
	@Column(columnDefinition = "json")
	private JsonFlow flow;
	private Date createTime;

	private String createBy;

	private String updateBy;

	private Date updateTime;

	private Byte isDone;

	private String currentNode;

	private String currentNodeDescription;

	/**
	 * 业务编号
	 */
	private String businesskey;

	public String getCreateBy() {
		return createBy;
	}

	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}

	public String getUpdateBy() {
		return updateBy;
	}

	public void setUpdateBy(String updateBy) {
		this.updateBy = updateBy;
	}

	public String getBusinesskey() {
		return businesskey;
	}

	public void setBusinesskey(String businesskey) {
		this.businesskey = businesskey;
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

	public JsonFlow getFlow() {
		return flow;
	}

	public void setFlow(JsonFlow flow) {
		this.flow = flow;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

}
