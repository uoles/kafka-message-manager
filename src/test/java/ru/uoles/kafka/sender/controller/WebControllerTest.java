package ru.uoles.kafka.sender.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WebController.class)
@AutoConfigureMockMvc(addFilters = false)
class WebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void showForm_returnsIndexView() throws Exception {
        mockMvc.perform(get("/web/send-message"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void homeAlias_returnsIndexView() throws Exception {
        mockMvc.perform(get("/web/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        mockMvc.perform(get("/web/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void unknownRoute_returnsNotFound() throws Exception {
        mockMvc.perform(get("/web/unknown"))
                .andExpect(status().isNotFound());
    }
}
