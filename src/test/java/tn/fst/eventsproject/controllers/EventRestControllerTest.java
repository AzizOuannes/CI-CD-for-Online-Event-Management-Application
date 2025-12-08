package tn.fst.eventsproject.controllers;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.fst.eventsproject.entities.Logistics;
import tn.fst.eventsproject.services.IEventServices;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventRestControllerTest {

    @Mock
    IEventServices eventServices;

    @InjectMocks
    EventRestController controller;

    @Test
    void getLogistiquesDates_endpointRespondsOk() throws Exception {
        Logistics l = new Logistics();
        List<Logistics> list = Collections.singletonList(l);
        when(eventServices.getLogisticsDates(any(LocalDate.class), any(LocalDate.class))).thenReturn(list);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        mockMvc.perform(get("/event/getLogs/2024-01-01/2024-12-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
