package uk.gov.hmcts.reform.wastandalonetaskbpmn.bpmn;

import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.DeploymentBuilder;
import org.camunda.bpm.engine.repository.DeploymentQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.junit.Test;
import uk.gov.hmcts.reform.wastandalonetaskbpmn.CamundaProcessEngineBaseUnitTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class CamundaCreateTaskTest extends CamundaProcessEngineBaseUnitTest {
    @Test
    @Deployment(resources = {"wa-task-initiation-ia-asylum.bpmn"})
    public void should_create_a_task_with_delay_until() {
        //helper method has assertions to check the task is raised with a delayUntil attribute
        final ProcessInstance processInstance = createTask(true);
        assertNotNull(processInstance);
    }

    @Test
    @Deployment(resources = {"wa-task-initiation-ia-asylum.bpmn"})
    public void should_create_a_task_with_no_delay_until() {
        //helper method has assertions to check the task is raised with no delayUntil attribute
        final ProcessInstance processInstance = createTask(false);
        assertNotNull(processInstance);
    }

    @Test
    public void should_not_create_a_task_with_different_tenant_id_multiple_resource() {

        clearDeployments();

        RepositoryService repositoryService = processEngineRule.getRepositoryService();

        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
            .tenantId("someTenantId");
        deploymentBuilder.addClasspathResource("wa-task-initiation-ia-asylum.bpmn")
            .deploy();

        DeploymentBuilder deploymentBuilder2 = repositoryService.createDeployment()
            .tenantId("someTenantId2");

        deploymentBuilder2.addClasspathResource("wa-task-initiation-ia-asylum_duplicate.bpmn")
            .deploy();

        assertThrows(MismatchingMessageCorrelationException.class, () -> {
            createTask(false);
        });
    }

    @Test
    public void should_create_a_task_with_and_without_tenant_id_multiple_resources() {

        clearDeployments();

        RepositoryService repositoryService = processEngineRule.getRepositoryService();

        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
            .tenantId("someTenantId");
        deploymentBuilder.addClasspathResource("wa-task-initiation-ia-asylum.bpmn")
            .deploy();

        DeploymentBuilder deploymentBuilder2 = repositoryService.createDeployment();

        deploymentBuilder2.addClasspathResource("wa-task-initiation-ia-asylum_duplicate.bpmn")
            .deploy();

        ProcessInstance processInstance =  createTask(false);

        BpmnAwareTests.complete(BpmnAwareTests.task("processTask"));
        assertNull(processInstance.getTenantId());
        BpmnAwareTests.assertThat(processInstance).isEnded();

        clearDeployments();
    }

    @Test
    public void should_not_create_a_task_without_tenant_id_multiple_resource() {

        clearDeployments();

        RepositoryService repositoryService = processEngineRule.getRepositoryService();

        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("wa-task-initiation-ia-asylum.bpmn")
            .deploy();

        DeploymentBuilder deploymentBuilder2 = repositoryService.createDeployment();
        deploymentBuilder2.addClasspathResource("wa-task-initiation-ia-asylum_duplicate.bpmn");
        assertThrows(ProcessEngineException.class, deploymentBuilder2::deploy);

    }

    @Test
    public void should_not_create_a_task_with_same_tenant_id_multiple_resource() {

        clearDeployments();

        RepositoryService repositoryService = processEngineRule.getRepositoryService();

        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().tenantId("someTenantId");
        deploymentBuilder.addClasspathResource("wa-task-initiation-ia-asylum.bpmn")
            .deploy();

        DeploymentBuilder deploymentBuilder2 = repositoryService.createDeployment().tenantId("someTenantId");
        deploymentBuilder2.addClasspathResource("wa-task-initiation-ia-asylum_duplicate.bpmn");
        assertThrows(ProcessEngineException.class, deploymentBuilder2::deploy);

    }

    private void clearDeployments() {
        DeploymentQuery deploymentQuery = processEngineRule.getRepositoryService().createDeploymentQuery();
        for (org.camunda.bpm.engine.repository.Deployment deployment : deploymentQuery.list()) {
            processEngineRule.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

}
