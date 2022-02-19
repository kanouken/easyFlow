package io.kanouken.easyflow.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

import io.kanouken.easyflow.config.JpaConverterCandidateList;
import io.kanouken.easyflow.config.JpaConverterMap;

/**
 * 带签收任务
 * 
 * @author Administrator
 *
 */
@Entity(name = "ef_claim")
public class EasyFlowClaim {

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
	 * 候选人
	 */
	@Convert(converter = JpaConverterCandidateList.class)
	@Column(columnDefinition = "json")
	private List<Integer> candidates;

	/**
	 * 变量
	 */
	@Convert(converter = JpaConverterMap.class)
	@Column(columnDefinition = "json")
	private Map<String, Object> vars;

	private String type;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * 0 未签收 1 已签收
	 */
	private Byte status;

	/**
	 * 签收时间
	 */
	private Date claimTime;
	/*
	 * 签收人
	 */
	private Integer claimer;

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

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public Byte getIsDelete() {
		return isDelete;
	}

	public void setIsDelete(Byte isDelete) {
		this.isDelete = isDelete;
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

	public List<Integer> getCandidates() {
		return candidates;
	}

	public void setCandidates(List<Integer> candidates) {
		this.candidates = candidates;
	}

	public Byte getStatus() {
		return status;
	}

	public void setStatus(Byte status) {
		this.status = status;
	}

	public Date getClaimTime() {
		return claimTime;
	}

	public void setClaimTime(Date claimTime) {
		this.claimTime = claimTime;
	}

	public Integer getClaimer() {
		return claimer;
	}

	public void setClaimer(Integer claimer) {
		this.claimer = claimer;
	}

	public Map<String, Object> getVars() {
		return vars;
	}

	public void setVars(Map<String, Object> vars) {
		this.vars = vars;
	}

}
