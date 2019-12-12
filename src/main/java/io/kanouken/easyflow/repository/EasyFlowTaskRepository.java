package io.kanouken.easyflow.repository;

import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import io.kanouken.easyflow.model.EasyFlowTask;
import io.kanouken.easyflow.model.dto.EasyFlowTaskListDto;

@Repository
public interface EasyFlowTaskRepository extends CrudRepository<EasyFlowTask, String> {

	@Query(nativeQuery = true, name = "test", value = "select  " + "ef_task.id " + ",ef_task.instance_id "
			+ ",ef_task.is_done " + ",ef_task.node_name " + ",ef_task.node_description "
			+ ",  CAST( ef_instance.flow ->> '$.key' as char) as flowKey "
			+ ", CAST( ef_instance.flow ->> '$.name' as char) as flowName "
			+ ",CAST(  ef_instance.flow ->> '$.formUrl' as char) as formUrl " + ",ef_instance.create_by "
			+ ",ef_task.create_time "

			+ "from ef_task left join ef_instance  on ef_task.instance_id= ef_instance.id  where ef_task.assignment=?1 and ef_task.instance_id in (?2) order by ef_task.create_time desc")

	List<Tuple> findByAssignmentAndInstanceIdInOrderByCreateTimeDesc(Integer assignment, List<String> instanceIds);

	@Query(nativeQuery = true, value = "select  " + "ef_task.id " + ",ef_task.instance_id " + ",ef_task.is_done "
			+ ",ef_task.node_name " + ",ef_task.node_description "
			+ ",  CAST( ef_instance.flow ->> '$.key' as char) as flowKey "
			+ ", CAST( ef_instance.flow ->> '$.name' as char) as flowName "
			+ ",CAST(  ef_instance.flow ->> '$.formUrl' as char) as formUrl " + ",ef_instance.create_by "
			+ ",ef_task.create_time " + ",ef_instance.businesskey "
			+ "from ef_task left join ef_instance  on ef_task.instance_id= ef_instance.id  where ef_task.assignment=?1 and ef_task.is_done = ?2 order by ef_task.create_time desc")

	List<Object[]> findByAssignmentAndStatusOrderByCreateTimeDesc(Integer assignment, Byte status);

	List<EasyFlowTask> findByInstanceIdOrderByCreateTimeAsc(String instanceId);

}
