package io.kanouken.easyflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import io.kanouken.easyflow.model.EasyFlowInstance;

@Repository
public interface EasyFlowInstanceRepository extends CrudRepository<EasyFlowInstance, String> {

	@Query(nativeQuery = true, value = "select * from ef_instance where ef_instance.flow -> '$.key' = ?1")
	List<EasyFlowInstance> findByFlowKey(String flowKey);

	List<EasyFlowInstance> findByIdIn(List<String> instanceIds);

	// List<EasyFlowInstance> findByNodeName(String nodeName);

}
