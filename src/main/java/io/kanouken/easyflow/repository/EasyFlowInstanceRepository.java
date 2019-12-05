package io.kanouken.easyflow.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import io.kanouken.easyflow.model.EasyFlowInstance;

@Repository
public interface EasyFlowInstanceRepository extends CrudRepository<EasyFlowInstance, String> {

}
