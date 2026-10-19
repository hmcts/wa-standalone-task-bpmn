package uk.gov.hmcts.reform.wastandalonetaskbpmn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.wastandalonetaskbpmn.controller.BaseController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(BaseController.class)
@SuppressWarnings("PMD.LawOfDemeter")
class BaseControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void canRetrieveByIdWhenExists() throws Exception {
        mvc.perform(get("/")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string("Welcome to WA Standalone Task BPMN"));
    }
}
