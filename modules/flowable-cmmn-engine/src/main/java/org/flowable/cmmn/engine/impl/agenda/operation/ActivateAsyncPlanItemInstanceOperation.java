/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.cmmn.engine.impl.agenda.operation;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.impl.job.AsyncActivatePlanItemInstanceJobHandler;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.Task;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

/**
 * @author Dennis Federico
 */
public class ActivateAsyncPlanItemInstanceOperation extends AbstractChangePlanItemInstanceStateOperation {

    protected String entryCriterionId;

    public ActivateAsyncPlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, String entryCriterionId) {
        super(commandContext, planItemInstanceEntity);
        this.entryCriterionId = entryCriterionId;
    }

    @Override
    protected String getLifeCycleTransition() {
        return PlanItemTransition.ASYNC_ACTIVATE;
    }

    @Override
    protected String getNewState() {
        return PlanItemInstanceState.ASYNC_ACTIVE;
    }

    @Override
    protected void internalExecute() {
        planItemInstanceEntity.setLastStartedTime(getCurrentTime(commandContext));
        CommandContextUtil.getCmmnHistoryManager(commandContext).recordPlanItemInstanceStarted(planItemInstanceEntity);
        createAsyncJob((Task) planItemInstanceEntity.getPlanItem().getPlanItemDefinition());
    }

    protected void createAsyncJob(Task task) {
        JobService jobService = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getJobServiceConfiguration().getJobService();
        JobEntity job = jobService.createJob();
        job.setJobHandlerType(AsyncActivatePlanItemInstanceJobHandler.TYPE);
        job.setScopeId(planItemInstanceEntity.getCaseInstanceId());
        job.setSubScopeId(planItemInstanceEntity.getId());
        job.setScopeDefinitionId(planItemInstanceEntity.getCaseDefinitionId());
        job.setScopeType(ScopeTypes.CMMN);
        job.setElementId(task.getId());
        job.setElementName(task.getName());
        job.setJobHandlerConfiguration(entryCriterionId);
        job.setTenantId(planItemInstanceEntity.getTenantId());
        jobService.createAsyncJob(job, task.isExclusive());
        jobService.scheduleAsyncJob(job);
    }

    @Override
    public String toString() {
        PlanItem planItem = planItemInstanceEntity.getPlanItem();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[Async activate PlanItem] ");
        stringBuilder.append(planItem);

        if (entryCriterionId != null) {
            stringBuilder.append(" via entry criterion ").append(entryCriterionId);
        }

        return stringBuilder.toString();
    }

    @Override
    protected String getOperationName() {
        return "[Async activate plan item]";
    }

}
