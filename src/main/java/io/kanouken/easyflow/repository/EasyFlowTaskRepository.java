package io.kanouken.easyflow.repository;

import java.util.Date;
import java.util.List;

import javax.persistence.Tuple;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import io.kanouken.easyflow.model.EasyFlowTask;

@Repository
public interface EasyFlowTaskRepository extends CrudRepository<EasyFlowTask, String> {

	@Query(nativeQuery = true, name = "test", value = "select  " + "ef_task.id " + ",ef_task.instance_id "
			+ ",ef_task.is_done " + ",ef_task.node_name " + ",ef_task.node_description "
			+ ",  CAST( ef_instance.flow ->> '$.key' as char) as flowKey "
			+ ", CAST( ef_instance.flow ->> '$.name' as char) as flowName "
			+ ",CAST(  ef_instance.flow ->> '$.formUrl' as char) as formUrl " + ",ef_instance.create_by "
			+ ",ef_task.create_time "

			+ "from ef_task left join ef_instance  on ef_task.instance_id= ef_instance.id  where ef_task.is_delete = 0 and  ef_task.assignment=?1 and ef_task.instance_id in (?2) order by ef_task.create_time desc")

	List<Tuple> findByAssignmentAndInstanceIdInOrderByCreateTimeDesc(Integer assignment, List<String> instanceIds);

	@Query(nativeQuery = true, value = "select  " + "ef_task.id " + ",ef_task.instance_id " + ",ef_task.is_done "
			+ ",ef_task.node_name " + ",ef_task.node_description "
			+ ",  CAST( ef_instance.flow ->> '$.key' as char) as flowKey "
			+ ", CAST( ef_instance.flow ->> '$.name' as char) as flowName "
			+ ",CAST(  ef_instance.flow ->> '$.formUrl' as char) as formUrl " + ",ef_instance.create_by "
			+ ",DATE_FORMAT(ef_task.create_time, '%Y-%m-%d %H:%i')  " + ",ef_instance.businesskey "
            + ",CAST( ef_task.vars as char )  as vars "
			+ "from ef_task left join ef_instance  on ef_task.instance_id= ef_instance.id  "
			+ "where ef_task.is_delete = 0 and  ef_task.assignment=?1 and ef_task.is_done = ?2  "
			+ "order by ef_task.create_time desc")

	List<Object[]> findByAssignmentAndStatusOrderByCreateTimeDesc(Integer assignment, Byte status);

	@Query(nativeQuery = true, value = "select  " + "ef_task.id " + ",ef_task.instance_id " + ",ef_task.is_done "
			+ ",ef_task.node_name " + ",ef_task.node_description " + ",ef_task.approval_status "
			+ ",DATE_FORMAT(ef_task.update_time, '%Y-%m-%d %H:%i') " + ",ef_task.complete_result "
			+ ",  CAST( ef_instance.flow ->> '$.key' as char) as flowKey "
			+ ", CAST( ef_instance.flow ->> '$.name' as char) as flowName "
			+ ",CAST(  ef_instance.flow ->> '$.formUrl' as char) as formUrl " + ",ef_instance.create_by "
			+ ",DATE_FORMAT(ef_task.create_time, '%Y-%m-%d %H:%i') " + ",ef_instance.businesskey "
			+ ",ef_task.assignment "
            + ",CAST(  ef_task.vars as char) as vars "
			+ "from ef_task left join ef_instance  on ef_task.instance_id= ef_instance.id  where ef_task.is_delete =0 and  ef_task.instance_id=?1  order by ef_task.create_time asc")
	List<Object[]> findByInstanceIdOrderByCreateTimeAsc(String instanceId);

	List<EasyFlowTask> findByInstanceId(String flowInstanceId);
	
	List<EasyFlowTask> findByInstanceIdAndType(String flowInstanceId,String type);

	Integer countByAssignmentAndIsDoneAndIsDelete(Integer assignment, Byte valueOf, Byte valueOf2);


	@Query(nativeQuery = true, value =
			   "select  " + "ef_task.id " + ",ef_task.instance_id " + ",ef_task.is_done "
			+ ",ef_task.node_name " + ",ef_task.node_description "
			+ ",  CAST( ef_instance.flow ->> '$.key' as char) as flowKey "
			+ ", CAST( ef_instance.flow ->> '$.name' as char) as flowName "
			+ ",CAST(  ef_instance.flow ->> '$.formUrl' as char) as formUrl " + ",ef_instance.create_by "
			+ ",DATE_FORMAT(ef_task.create_time, '%Y-%m-%d %H:%i')  " + ",ef_instance.businesskey "
			+ "from ef_task left join ef_instance  on ef_task.instance_id= ef_instance.id  "
			+ "where if(?4 is not null and ?4!='',ef_instance.flow ->'$.name' like CONCAT('%',?4,'%'),1=1)  and if(?5 is not null and ?5!='',ef_instance.businesskey  like CONCAT('%',?5,'%'),1=1) and  ef_task.is_delete = 0 and  ef_task.assignment=?1 and ef_task.is_done = 1  "
			+ "and ef_task.create_time BETWEEN  ?2 and ?3 "
			+ "order by ef_task.create_time desc \n#pageable\n",
			countQuery =
					"select  count(1)  from ef_task left join ef_instance  on ef_task.instance_id= ef_instance.id "
					+ "where if(?4 is not null and ?4!='',ef_instance.flow ->'$.name' like CONCAT('%',?4,'%'),1=1)  and if(?5 is not null and ?5!='',ef_instance.businesskey  like CONCAT('%',?5,'%'),1=1) and  ef_task.is_delete = 0 and  ef_task.assignment=?1 and ef_task.is_done = 1  "
					+ "and ef_task.create_time BETWEEN  ?2 and ?3 "
	)
	Page<Object[]> findDonePaged(Integer assignment, Date startDate, Date endDate ,String flowName,String bizCode, Pageable pageable);




}
