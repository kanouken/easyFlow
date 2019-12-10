package io.kanouken.easyflow.model;

import java.util.List;

public interface IEasyFlowBusinessService<T> {

	public List<T> transformToBusiness(List<EasyFlowInstance> instances);

}
