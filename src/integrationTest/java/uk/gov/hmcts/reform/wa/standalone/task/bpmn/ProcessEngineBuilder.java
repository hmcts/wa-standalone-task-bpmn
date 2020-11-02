package uk.gov.hmcts.reform.wa.standalone.task.bpmn;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;

// The way the rule is written we can only have a single instance of this across our tests.
public final class ProcessEngineBuilder {
    private static final ProcessEngine INSTANCE = new StandaloneInMemProcessEngineConfiguration().buildProcessEngine();

    private ProcessEngineBuilder() {
    }

    public static ProcessEngine getProcessEngine() {
        return INSTANCE;
    }
}
