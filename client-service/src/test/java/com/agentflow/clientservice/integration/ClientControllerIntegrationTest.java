package com.agentflow.clientservice.integration;

import com.agentflow.clientservice.controller.ClientController;
import com.agentflow.clientservice.dto.request.ClientRequest;
import com.agentflow.clientservice.dto.request.ClientStatusUpdateRequest;
import com.agentflow.clientservice.dto.request.ClientUpdateRequest;
import com.agentflow.clientservice.dto.response.ClientResponse;
import com.agentflow.clientservice.entity.DealStatus;
import com.agentflow.clientservice.exception.GlobalExceptionHandler;
import com.agentflow.clientservice.exception.NotFoundException;
import com.agentflow.clientservice.service.ClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
@Import(GlobalExceptionHandler.class)
class ClientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @Test
    void create_ReturnsCreatedClient() throws Exception {
        ClientResponse response = createResponse(1L);

        when(clientService.create(any(ClientRequest.class)))
                .thenReturn(response);

        String request = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "john@example.com",
                    "phone": "+375291111111"
                }
                """;

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.phone").value("+375291111111"))
                .andExpect(jsonPath("$.dealStatus").value("NEW"));
    }

    @Test
    void getById_ReturnsClient() throws Exception {
        ClientResponse response = createResponse(1L);

        when(clientService.getById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/clients/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void getById_ReturnsNotFound_WhenClientDoesNotExist() throws Exception {
        when(clientService.getById(999L))
                .thenThrow(new NotFoundException("Client not found: 999"));

        mockMvc.perform(get("/api/v1/clients/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Client not found: 999"));
    }

    @Test
    void getAll_ReturnsClients() throws Exception {
        ClientResponse first = createResponse(1L);
        ClientResponse second = createResponse(2L);

        when(clientService.getAll())
                .thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void update_ReturnsUpdatedClient() throws Exception {
        ClientResponse response = createResponse(1L);
        response.setFirstName("Updated");
        response.setLastName("Name");

        when(clientService.update(eq(1L), any(ClientUpdateRequest.class)))
                .thenReturn(response);

        String request = """
                {
                    "firstName": "Updated",
                    "lastName": "Name",
                    "email": "updated@example.com",
                    "phone": "+375292222222"
                }
                """;

        mockMvc.perform(put("/api/v1/clients/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("Name"));
    }

    @Test
    void updateStatus_ReturnsUpdatedClient() throws Exception {
        ClientResponse response = createResponse(1L);
        response.setDealStatus(DealStatus.SUCCESS);

        when(clientService.updateStatus(
                eq(1L),
                any(ClientStatusUpdateRequest.class)
        )).thenReturn(response);

        String request = """
                {
                    "dealStatus": "SUCCESS",
                    "comment": "Deal completed"
                }
                """;

        mockMvc.perform(patch("/api/v1/clients/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.dealStatus").value("SUCCESS"));
    }

    @Test
    void delete_ReturnsNoContent() throws Exception {
        doNothing().when(clientService).delete(1L);

        mockMvc.perform(delete("/api/v1/clients/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void create_ReturnsBadRequest_WhenRequestIsInvalid() throws Exception {
        String request = """
                {
                    "firstName": "",
                    "lastName": "",
                    "email": "invalid-email",
                    "phone": ""
                }
                """;

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    private ClientResponse createResponse(Long id) {
        ClientResponse response = new ClientResponse();

        response.setId(id);
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setEmail("john@example.com");
        response.setPhone("+375291111111");
        response.setDealStatus(DealStatus.NEW);

        return response;
    }
}