package uk.gov.hmcts.reform.wastandalonetaskbpmn.bpmn;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wastandalonetaskbpmn.CamundaProcessEngineBaseUnitTest;

public class CamundaCompleteTaskTest extends CamundaProcessEngineBaseUnitTest {

    @Test
    @Deployment(resources = {"wa-task-initiation-ia-asylum.bpmn"})
    public void should_create_a_task_with_delay_until_and_complete_the_task() {

        ProcessInstance processInstance = createTask(true);

        BpmnAwareTests.complete(BpmnAwareTests.task("processTask"));
        BpmnAwareTests.assertThat(processInstance).isEnded();
    }

    @Test
    @Deployment(resources = {"wa-task-initiation-ia-asylum.bpmn"})
    public void should_create_a_task_with_no_delay_until_and_complete_the_task() {

        ProcessInstance processInstance = createTask(false);

        BpmnAwareTests.complete(BpmnAwareTests.task("processTask"));
        BpmnAwareTests.assertThat(processInstance).isEnded();
    }
}
