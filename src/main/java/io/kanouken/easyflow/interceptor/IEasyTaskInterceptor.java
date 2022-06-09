package io.kanouken.easyflow.interceptor;

import io.kanouken.easyflow.EasyFlowContext;
import io.kanouken.easyflow.model.EasyFlowClaim;
import io.kanouken.easyflow.model.EasyFlowInstance;
import io.kanouken.easyflow.model.EasyFlowTask;

/**
 * 任务执行前后拦截
 * 
 * @author xiamiaomiao
 *
 */
public interface IEasyTaskInterceptor {

	public void beforeTask(EasyFlowTask task, EasyFlowInstance instance, EasyFlowContext context);

	public void afterTask(EasyFlowTask task, EasyFlowInstance instance, EasyFlowContext context, EasyFlowTask nextTask, EasyFlowClaim nextClaim);
}
