package io.kanouken.easyflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import io.kanouken.easyflow.model.EasyFlowClaim;

@Repository
public interface EasyFlowClaimRepository extends CrudRepository<EasyFlowClaim, String> {

	@Query(nativeQuery = true, value = " select ef_claim.id ,node_name,node_description,CAST( ef_instance.flow ->> '$.name' as char) as flowName, "
			+ " DATE_FORMAT(ef_claim.create_time, '%Y-%m-%d %H:%i')    "
			+ " from ef_claim  left join ef_instance  on ef_claim.instance_id= ef_instance.id  "
			+ " where ef_claim.status  = 0 and json_contains(candidates->'$[*]',CONCAT( '\"',?1, '\"'),'$')   ")
	public List<Object[]> findByCandidaterAndStatus(String candidater);
}
