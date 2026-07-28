package com.agentflow.clientservice.integration;

import com.agentflow.clientservice.controller.ClientController;
import com.agentflow.clientservice.dto.request.ClientRequest;
import com.agentflow.clientservice.dto.response.ClientResponse;
import com.agentflow.clientservice.exception.GlobalExceptionHandler;
import com.agentflow.clientservice.exception.NotFoundException;
import com.agentflow.clientservice.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
@Import(GlobalExceptionHandler.class)
public class ClientControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClientService clientService;

    @Test
    public void createClient_InvalidDto_Returns400() throws Exception {
        ClientRequest clientRequest = ClientRequest.builder()
                .firstName(" ")
                .lastName(" ")
                .email(" ")
                .phone(" ")
                .build();
        when(clientService.create(any())).thenReturn(ClientResponse.builder().build());

        mockMvc.perform(post("/api/v1/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(clientRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getClient_NotFound_Returns404() throws Exception {
        Long id = 1L;
        when(clientService.getById(id)).thenThrow(new NotFoundException("Client not found: " + id));

        mockMvc.perform(get("/api/v1/clients/1"))
                .andExpect(status().isNotFound());
    }

}
