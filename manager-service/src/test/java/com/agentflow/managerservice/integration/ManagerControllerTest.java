package com.agentflow.managerservice.integration;

import com.agentflow.managerservice.controller.ManagerController;
import com.agentflow.managerservice.dto.request.ManagerRequest;
import com.agentflow.managerservice.dto.request.ManagerStatusUpdateRequest;
import com.agentflow.managerservice.dto.request.ManagerUpdateRequest;
import com.agentflow.managerservice.dto.response.ManagerResponse;
import com.agentflow.managerservice.entity.ManagerStatus;
import com.agentflow.managerservice.exception.GlobalExceptionHandler;
import com.agentflow.managerservice.exception.NotFoundException;
import com.agentflow.managerservice.service.ManagerService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManagerController.class)
@Import(GlobalExceptionHandler.class)
class ManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ManagerService managerService;

    private ManagerResponse managerResponse;
    private ManagerRequest validCreateRequest;
    private final Long managerId = 1L;

    @BeforeEach
    void setUp() {
        managerResponse = ManagerResponse.builder()
                .id(managerId)
                .firstName("John")
                .lastName("Doe")
                .status(ManagerStatus.ACTIVE)
                .currentLoad(0)
                .maxCapacity(5)
                .build();

        validCreateRequest = ManagerRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+12345678901")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/managers - Should return 201 Created")
    void create_ShouldReturn201Created() throws Exception {
        when(managerService.create(any(ManagerRequest.class))).thenReturn(managerResponse);

        mockMvc.perform(post("/api/v1/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(managerId))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @DisplayName("POST /api/v1/managers - Should return 400 Bad Request when validation fails")
    void create_InvalidRequest_ShouldReturn400() throws Exception {
        ManagerRequest invalidRequest = ManagerRequest.builder()
                .firstName("")
                .lastName("Doe")
                .email("invalid-email")
                .phone("123")
                .build();

        mockMvc.perform(post("/api/v1/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/managers/{id} - Should return 200 OK")
    void getById_ShouldReturn200OK() throws Exception {
        when(managerService.getById(managerId)).thenReturn(managerResponse);

        mockMvc.perform(get("/api/v1/managers/{id}", managerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(managerId))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    @DisplayName("GET /api/v1/managers/{id} - Should return 404 Not Found")
    void getById_NotFound_ShouldReturn404() throws Exception {
        when(managerService.getById(managerId)).thenThrow(new NotFoundException("Manager not found: " + managerId));

        mockMvc.perform(get("/api/v1/managers/{id}", managerId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/managers - Should return 200 OK with Page")
    void getAll_ShouldReturn200OK() throws Exception {
        when(managerService.getAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(managerResponse)));

        mockMvc.perform(get("/api/v1/managers")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(managerId));
    }

    @Test
    @DisplayName("PUT /api/v1/managers/{id} - Should return 200 OK")
    void update_ShouldReturn200OK() throws Exception {
        ManagerUpdateRequest updateRequest = ManagerUpdateRequest.builder()
                .firstName("John")
                .lastName("Updated")
                .email("john.updated@example.com")
                .phone("+12345678901")
                .build();

        when(managerService.update(eq(managerId), any(ManagerUpdateRequest.class))).thenReturn(managerResponse);

        mockMvc.perform(put("/api/v1/managers/{id}", managerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(managerId));
    }

    @Test
    @DisplayName("PATCH /api/v1/managers/{id}/status - Should return 200 OK")
    void updateStatus_ShouldReturn200OK() throws Exception {
        ManagerStatusUpdateRequest statusRequest = ManagerStatusUpdateRequest.builder()
                .status(ManagerStatus.ACTIVE)
                .comment("Status updated to active")
                .build();

        when(managerService.updateStatus(eq(managerId), any(ManagerStatusUpdateRequest.class)))
                .thenReturn(managerResponse);

        mockMvc.perform(patch("/api/v1/managers/{id}/status", managerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/v1/managers/{id} - Should return 204 No Content")
    void delete_ShouldReturn204NoContent() throws Exception {
        doNothing().when(managerService).delete(managerId);

        mockMvc.perform(delete("/api/v1/managers/{id}", managerId))
                .andExpect(status().isNoContent());
    }
}
