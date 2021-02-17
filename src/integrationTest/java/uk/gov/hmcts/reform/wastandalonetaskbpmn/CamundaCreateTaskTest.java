package uk.gov.hmcts.reform.wastandalonetaskbpmn;

import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.JobQuery;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

import static java.time.ZonedDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Date.from;
import static java.util.Map.of;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.complete;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.task;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.externalTask;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;
import static org.junit.Assert.assertEquals;
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
            "delayUntil", ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "isDuplicate", false
        ));

        ActivityInstance activityInstance = runtimeService().getActivityInstance(processInstance.getId());
        ActivityInstance[] childActivityInstances = activityInstance.getChildActivityInstances();

        // only timer activity is created
        assertEquals(1, childActivityInstances.length);
        assertEquals("serviceTask", childActivityInstances[0].getActivityType());
        assertThat(processInstance).isStarted()
            .task().isNull();
        BpmnAwareTests.assertThat(processInstance).isWaitingAt("idempotencyCheck");
        BpmnAwareTests.complete(externalTask());

        JobQuery jobQuery = managementService.createJobQuery().processInstanceId(processInstance.getId());
        // will execute the delayUtil timer manually
        managementService.executeJob(jobQuery.singleResult().getId());

        activityInstance = runtimeService().getActivityInstance(processInstance.getId());
        childActivityInstances = activityInstance.getChildActivityInstances();

        assertEquals(1, childActivityInstances.length);
        assertEquals(childActivityInstances[0].getActivityId(), PROCESS_TASK);

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
    public void createsAndWaitsForDelayUntilTimerIsTriggered() {
        String testBusinessKey = "TestBusinessKey";
        ProcessInstance processInstance = startCreateTaskProcessWithBusinessKey(of(
            "taskId", "provideRespondentEvidence",
            "group", EXPECTED_GROUP,
            "dueDate", DUE_DATE_STRING,
            "name", TASK_NAME,
            "delayUntil", ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "isDuplicate", false
        ), testBusinessKey);

        ActivityInstance activityInstance = runtimeService().getActivityInstance(processInstance.getId());
        ActivityInstance[] childActivityInstances = activityInstance.getChildActivityInstances();

        assertEquals(1, childActivityInstances.length);
        assertEquals("serviceTask", childActivityInstances[0].getActivityType());
        assertThat(processInstance).isStarted()
            .task().isNull();

        BpmnAwareTests.assertThat(processInstance).isWaitingAt("idempotencyCheck");
        BpmnAwareTests.complete(externalTask());

        BpmnAwareTests.assertThat(processInstance).isWaitingAt("processStartTimer");

        processEngineRule.getRuntimeService().correlateMessage("cancelTasks", testBusinessKey);
        BpmnAwareTests.assertThat(processInstance).isEnded();
    }

    @Test
    @Deployment(resources = {"wa-task-initiation-ia-asylum.bpmn"})
    public void createsAndCancelACamundaTask() {
        String testBusinessKey = "TestBusinessKey";

        ProcessInstance createTaskAndCancel = startCreateTaskProcessWithBusinessKey(of(
            "group", EXPECTED_GROUP,
            "dueDate", DUE_DATE_STRING,
            "name", TASK_NAME,
            "delayUntil", ZonedDateTime.now().plusSeconds(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "isDuplicate", false
        ), testBusinessKey);

        BpmnAwareTests.assertThat(createTaskAndCancel).isWaitingAt("idempotencyCheck");
        BpmnAwareTests.complete(externalTask());

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

    @Test
    @Deployment(resources = {"wa-task-initiation-ia-asylum.bpmn"})
    public void createsAndWaitsAtExternalTask() {
        ProcessInstance processInstance = startCreateTaskProcess(of(
            "taskId", "provideRespondentEvidence",
            "group", EXPECTED_GROUP,
            "dueDate", DUE_DATE_STRING,
            "name", TASK_NAME,
            "delayUntil", ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "isDuplicate", false
        ));

        ActivityInstance activityInstance = runtimeService().getActivityInstance(processInstance.getId());
        ActivityInstance[] childActivityInstances = activityInstance.getChildActivityInstances();

        assertEquals(1, childActivityInstances.length);
        assertEquals("serviceTask", childActivityInstances[0].getActivityType());
        assertThat(processInstance).isStarted()
            .task().isNull();
        BpmnAwareTests.assertThat(processInstance).isWaitingAt("idempotencyCheck");
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
