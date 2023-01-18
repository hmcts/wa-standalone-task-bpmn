package uk.gov.hmcts.reform.wastandalonetaskbpmn.bpmn;

import org.camunda.bpm.engine.runtime.JobQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.reform.wastandalonetaskbpmn.CamundaProcessEngineBaseUnitTest;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.externalTask;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CamundaDelayTaskTest extends CamundaProcessEngineBaseUnitTest {

    private ProcessInstance processInstance;

    @AfterEach
    public void tearDown() {

        processEngineRule.getRuntimeService().correlateMessage("cancelTasks", TEST_BUSINESS_KEY);
        BpmnAwareTests.assertThat(processInstance).isEnded();
    }

    @Test
    @Deployment(resources = {"wa-task-initiation-ia-asylum.bpmn"})
    public void should_create_a_task_with_delay_until_and_wait_at_processStartTimer() {

        String delayUntilValue = now().plusSeconds(1).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN));

        ZonedDateTime dueDate = now().plusDays(7);

        Map<String, Object> variables = new HashMap<>();
        variables.put("taskId", "provideRespondentEvidence");
        variables.put("dueDate", dueDate.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN)));
        variables.put("name", "Provide respondent evidence");
        variables.put("taskState", "configured");
        variables.put("taskCategory", "Case Progression");
        variables.put("location", "765324");
        variables.put("locationName", "Taylor House");
        variables.put("caseId", "012345678");
        variables.put("jurisdiction", "IA");
        variables.put("caseTypeId", "Asylum");
        variables.put("workingDaysAllowed", 2);
        variables.put("isDuplicate", false);
        variables.put("delayUntil", delayUntilValue);

        processInstance = startCreateTaskProcessWithBusinessKey(variables, TEST_BUSINESS_KEY);

        //Check task is waiting at idempotency check and manually execute
        BpmnAwareTests.assertThat(processInstance).isWaitingAt("idempotencyCheck");
        BpmnAwareTests.complete(externalTask());

        //Check task is waiting at timer check and manually execute
        BpmnAwareTests.assertThat(processInstance).isWaitingAt("processStartTimer");
        JobQuery jobQuery = managementService.createJobQuery().processInstanceId(processInstance.getId());

        final Date processDueDate = jobQuery.singleResult().getDuedate();

        assertEquals(delayUntilValue, dateFormat.format(processDueDate));

        managementService.executeJob(jobQuery.singleResult().getId());

    }

    @Test
    @Deployment(resources = {"wa-task-initiation-ia-asylum.bpmn"})
    public void should_create_a_task_with_no_delay_until_and_wait_at_processStartTimer() {

        ZonedDateTime dueDate = now().plusDays(7);

        Map<String, Object> variables = new HashMap<>();
        variables.put("taskId", "provideRespondentEvidence");
        variables.put("dueDate", dueDate.format(ISO_INSTANT));
        variables.put("name", "Provide respondent evidence");
        variables.put("taskState", "configured");
        variables.put("taskCategory", "Case Progression");
        variables.put("location", "765324");
        variables.put("locationName", "Taylor House");
        variables.put("caseId", "012345678");
        variables.put("jurisdiction", "IA");
        variables.put("caseTypeId", "Asylum");
        variables.put("workingDaysAllowed", 2);
        variables.put("isDuplicate", false);

        processInstance = startCreateTaskProcessWithBusinessKey(variables, TEST_BUSINESS_KEY);

        //Check task is waiting at idempotency check and manually execute
        BpmnAwareTests.assertThat(processInstance).isWaitingAt("idempotencyCheck");
        BpmnAwareTests.complete(externalTask());

        //Check task is waiting at timer check and manually execute
        BpmnAwareTests.assertThat(processInstance).isWaitingAt("processStartTimer");
        JobQuery jobQuery = managementService.createJobQuery().processInstanceId(processInstance.getId());

        final Date processDueDate = jobQuery.singleResult().getDuedate();

        //When DelayUntil is not set a default due date is added
        Assertions.assertEquals("2000-01-01T00:00:00", dateFormat.format(processDueDate));

        managementService.executeJob(jobQuery.singleResult().getId());

    }
}
