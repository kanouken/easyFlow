package io.kanouken.easyflow.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import io.kanouken.easyflow.model.EasyFlowTask;

@Repository
public interface EasyFlowTaskRepository extends CrudRepository<EasyFlowTask, String> {

}
