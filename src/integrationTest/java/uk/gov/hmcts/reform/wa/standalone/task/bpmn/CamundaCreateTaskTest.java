package uk.gov.hmcts.reform.wa.standalone.task.bpmn;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Collections.singletonMap;
import static java.util.Date.from;
import static java.util.Map.of;
import static org.apache.commons.lang3.time.DateUtils.isSameDay;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.complete;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.task;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;
import static org.junit.Assert.assertTrue;

import static uk.gov.hmcts.reform.wa.standalone.task.bpmn.ProcessEngineBuilder.getProcessEngine;

public class CamundaCreateTaskTest {

    public static final String PROCESS_TASK = "processTask";
    private static final String EXPECTED_GROUP = "TCW";
    private static final ZonedDateTime DUE_DATE = now().plusDays(7);
    private static final String DUE_DATE_STRING = DUE_DATE.format(ISO_INSTANT);
    public static final Date DUE_DATE_DATE = from(DUE_DATE.toInstant());
    public static final String TASK_NAME = "task name";

    @Rule
    public ProcessEngineRule processEngineRule = new ProcessEngineRule(getProcessEngine());

    @Test
    @Deployment(resources = {"create_task.bpmn"})
    public void createsAndCompletesATaskWithADueDate() {
        ProcessInstance processInstance = startCreateTaskProcess(of(
            "taskId", "provideRespondentEvidence",
            "group", EXPECTED_GROUP,
            "dueDate", DUE_DATE_STRING,
            "name", TASK_NAME
        ));

        assertThat(processInstance).isStarted()
            .task()
            .hasDefinitionKey(PROCESS_TASK)
            .hasCandidateGroup(EXPECTED_GROUP)
            .hasDueDate(DUE_DATE_DATE)
            .hasName(TASK_NAME)
            .isNotAssigned();
        assertThat(processInstance)
            .job()
            .hasDueDate(DUE_DATE_DATE);
        complete(task(PROCESS_TASK));
        assertThat(processInstance).isEnded();
    }

    @Test
    @Deployment(resources = {"create_task.bpmn"})
    public void createsAndCompletesATaskWithoutADueDate() {
        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("taskId", "provideRespondentEvidence");
        processVariables.put("group", EXPECTED_GROUP);
        processVariables.put("dueDate", null);
        processVariables.put("name", TASK_NAME);
        ProcessInstance processInstance = startCreateTaskProcess(processVariables);

        assertThat(processInstance).isStarted()
            .task()
            .hasDefinitionKey(PROCESS_TASK)
            .hasCandidateGroup(EXPECTED_GROUP)
            .hasName(TASK_NAME)
            .isNotAssigned();

        Date dueDate = task().getDueDate();
        Date expectedDueDate = from(LocalDate.now().plusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC));
        assertTrue(
            "Expected [" + dueDate + "] to be on [" + expectedDueDate + "]",
            isSameDay(dueDate, expectedDueDate)
        );

        Date jobDueDate = job().getDuedate();
        assertTrue(
            "Expected [" + jobDueDate + "] to be on [" + expectedDueDate + "]",
            isSameDay(jobDueDate, expectedDueDate)
        );
        complete(task(PROCESS_TASK));
        assertThat(processInstance).isEnded();
    }

    private ProcessInstance startCreateTaskProcess(Map<String, Object> processVariables) {
        return processEngineRule.getRuntimeService()
            .startProcessInstanceByMessage("createTaskMessage", processVariables);
    }
}
