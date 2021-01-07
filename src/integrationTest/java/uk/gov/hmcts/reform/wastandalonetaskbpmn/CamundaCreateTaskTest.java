package uk.gov.hmcts.reform.wastandalonetaskbpmn;

import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.JobQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Date.from;
import static java.util.Map.of;
import static org.apache.commons.lang3.time.DateUtils.isSameDay;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.complete;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.task;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.job;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.wastandalonetaskbpmn.ProcessEngineBuilder.getProcessEngine;


@SuppressWarnings("PMD.UseConcurrentHashMap")
public class CamundaCreateTaskTest {

    private static final String PROCESS_TASK = "processTask";
    private static final String TASK_NAME = "task name";
    private static final String EXPECTED_GROUP = "TCW";
    private static final ZonedDateTime DUE_DATE = now().plusDays(7);
    private static final Date DUE_DATE_DATE = from(DUE_DATE.toInstant());
    private static final String DUE_DATE_STRING = DUE_DATE.format(ISO_INSTANT);
    @Rule
    public ProcessEngineRule processEngineRule = new ProcessEngineRule(getProcessEngine());

    private ManagementService managementService;

    @Before
    public void setUp() {
        managementService = processEngineRule.getManagementService();
    }

    @Test
    @Deployment(resources = {"wa-task-initiation-ia-asylum.bpmn"})
    public void createsAndCompletesATaskWithDelayUntilTimer() {
        ProcessInstance processInstance = startCreateTaskProcess(of(
            "taskId", "provideRespondentEvidence",
            "group", EXPECTED_GROUP,
            "dueDate", DUE_DATE_STRING,
            "name", TASK_NAME,
            "delayUntil", ZonedDateTime.now().plusSeconds(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));

        ActivityInstance activityInstance = runtimeService().getActivityInstance(processInstance.getId());
        ActivityInstance[] childActivityInstances = activityInstance.getChildActivityInstances();

        // only timer activity is created
        assertTrue(childActivityInstances.length == 1);
        assertTrue(childActivityInstances[0].getActivityType().equals("intermediateTimer"));
        assertThat(processInstance).isStarted()
            .task().isNull();

        JobQuery jobQuery = managementService.createJobQuery().processInstanceId(processInstance.getId());
        // will execute the delayUtil timer manually
        managementService.executeJob(jobQuery.singleResult().getId());

        activityInstance = runtimeService().getActivityInstance(processInstance.getId());
        childActivityInstances = activityInstance.getChildActivityInstances();

        assertTrue(childActivityInstances.length == 1);
        assertTrue(childActivityInstances[0].getActivityId().equals(PROCESS_TASK));

        assertThat(processInstance).isStarted()
            .task()
            .hasDefinitionKey(PROCESS_TASK)
            .hasCandidateGroup(EXPECTED_GROUP)
            .hasDueDate(DUE_DATE_DATE)
            .hasName(TASK_NAME)
            .isNotAssigned();
        assertThat(processInstance)
            .job();
        complete(task(PROCESS_TASK));
        assertThat(processInstance).isEnded();
    }

    @Test
    @Deployment(resources = {"wa-task-initiation-ia-asylum.bpmn"})
    public void createsAndCompletesATaskWithADueDate() throws InterruptedException {
        ProcessInstance processInstance = startCreateTaskProcess(of(
            "taskId", "provideRespondentEvidence",
            "group", EXPECTED_GROUP,
            "dueDate", DUE_DATE_STRING,
            "name", TASK_NAME,
            "delayUntil", ZonedDateTime.now().plusSeconds(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));

        JobQuery jobQuery = managementService.createJobQuery().processInstanceId(processInstance.getId());
        managementService.executeJob(jobQuery.singleResult().getId());

        assertThat(processInstance).isStarted()
            .task()
            .hasDefinitionKey(PROCESS_TASK)
            .hasCandidateGroup(EXPECTED_GROUP)
            .hasDueDate(DUE_DATE_DATE)
            .hasName(TASK_NAME)
            .isNotAssigned();
        assertThat(processInstance)
            .job();
        complete(task(PROCESS_TASK));
        assertThat(processInstance).isEnded();
    }

    @Test
    @Ignore
    @Deployment(resources = {"wa-task-initiation-ia-asylum.bpmn"})
    public void createsAndCompletesATaskWithoutADueDate() {
        Map<String, Object> processVariables = new HashMap<>();
        processVariables.put("taskId", "provideRespondentEvidence");
        processVariables.put("group", EXPECTED_GROUP);
        processVariables.put("dueDate", null);
        processVariables.put("name", TASK_NAME);
        processVariables.put("delayUntil", ZonedDateTime.now().plusSeconds(1)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        ProcessInstance processInstance = startCreateTaskProcess(processVariables);

        JobQuery jobQuery = managementService.createJobQuery().processInstanceId(processInstance.getId());
        managementService.executeJob(jobQuery.singleResult().getId());

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

    @Test
    @Deployment(resources = {"wa-task-initiation-ia-asylum.bpmn"})
    public void createsAndCancelACamundaTask() {
        String testBusinessKey = "TestBusinessKey";

        ProcessInstance createTaskAndCancel = startCreateTaskProcessWithBusinessKey(of(
            "group", EXPECTED_GROUP,
            "dueDate", DUE_DATE_STRING,
            "name", TASK_NAME,
            "delayUntil", ZonedDateTime.now().plusSeconds(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ), testBusinessKey);

        JobQuery jobQuery = managementService.createJobQuery().processInstanceId(createTaskAndCancel.getId());
        managementService.executeJob(jobQuery.singleResult().getId());

        assertThat(createTaskAndCancel).isStarted()
            .task()
            .hasDefinitionKey(PROCESS_TASK)
            .hasCandidateGroup(EXPECTED_GROUP)
            .hasName(TASK_NAME)
            .isNotAssigned();
        assertThat(createTaskAndCancel).isWaitingAt(PROCESS_TASK);


        processEngineRule.getRuntimeService().correlateMessage("cancelTasks", testBusinessKey);
        assertThat(createTaskAndCancel).isEnded();

    }

    private ProcessInstance startCreateTaskProcess(Map<String, Object> processVariables) {
        return processEngineRule.getRuntimeService()
            .startProcessInstanceByMessage("createTaskMessage", processVariables);
    }

    private ProcessInstance startCreateTaskProcessWithBusinessKey(Map<String, Object> processVariables,
                                                                  String businessKey) {
        return processEngineRule.getRuntimeService()
            .startProcessInstanceByMessage("createTaskMessage", businessKey, processVariables);
    }

}
