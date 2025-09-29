package com.todo.eod.web;

import com.todo.eod.domain.DodPolicy;
import com.todo.eod.infra.repo.DodPolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
class PolicyControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    DodPolicyRepository repo;

    @Test
    void list_returns200_with_array() throws Exception {
        var p = DodPolicy.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .name("Default")
                .spec("{}")
                .build();
        when(repo.findAll()).thenReturn(List.of(p));
        mvc.perform(get("/dod-policies").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Default"));
    }
}

