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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ManagerController.class)
@Import(GlobalExceptionHandler.class)
class ManagerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ManagerService managerService;

    @Test
    void create_shouldReturnCreated() throws Exception {
        ManagerRequest request = ManagerRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+375291234567")
                .build();

        ManagerResponse response = ManagerResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+375291234567")
                .status(ManagerStatus.ACTIVE)
                .maxCapacity(10)
                .currentLoad(0)
                .build();

        when(managerService.create(any(ManagerRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(managerService).create(any(ManagerRequest.class));
    }

    @Test
    void getById_shouldReturnManager() throws Exception {
        ManagerResponse response = ManagerResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+375291234567")
                .status(ManagerStatus.ACTIVE)
                .maxCapacity(10)
                .currentLoad(2)
                .build();

        when(managerService.getById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/managers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.currentLoad").value(2));

        verify(managerService).getById(1L);
    }

    @Test
    void getById_shouldReturnNotFound_whenManagerDoesNotExist() throws Exception {
        when(managerService.getById(999L))
                .thenThrow(new NotFoundException("Manager not found: 999"));

        mockMvc.perform(get("/api/v1/managers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Manager not found: 999"));

        verify(managerService).getById(999L);
    }

    @Test
    void getAll_shouldReturnPageOfManagers() throws Exception {
        ManagerResponse firstManager = ManagerResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+375291234567")
                .status(ManagerStatus.ACTIVE)
                .maxCapacity(10)
                .currentLoad(0)
                .build();

        ManagerResponse secondManager = ManagerResponse.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .phone("+375299876543")
                .status(ManagerStatus.ACTIVE)
                .maxCapacity(10)
                .currentLoad(1)
                .build();

        PageImpl<ManagerResponse> page = new PageImpl<>(
                List.of(firstManager, secondManager),
                PageRequest.of(0, 20),
                2
        );

        when(managerService.getAll(any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/managers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[1].id").value(2));

        verify(managerService).getAll(any());
    }

    @Test
    void update_shouldReturnUpdatedManager() throws Exception {
        ManagerUpdateRequest request = ManagerUpdateRequest.builder()
                .firstName("John")
                .lastName("Updated")
                .email("john.updated@example.com")
                .phone("+375291234567")
                .build();

        ManagerResponse response = ManagerResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Updated")
                .email("john.updated@example.com")
                .phone("+375291234567")
                .status(ManagerStatus.ACTIVE)
                .maxCapacity(10)
                .currentLoad(0)
                .build();

        when(managerService.update(eq(1L), any(ManagerUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/managers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lastName").value("Updated"))
                .andExpect(jsonPath("$.email").value("john.updated@example.com"));

        verify(managerService).update(eq(1L), any(ManagerUpdateRequest.class));
    }

    @Test
    void updateStatus_shouldReturnUpdatedManager() throws Exception {
        ManagerStatusUpdateRequest request = ManagerStatusUpdateRequest.builder()
                .status(ManagerStatus.BUSY)
                .comment("Maximum workload reached")
                .build();

        ManagerResponse response = ManagerResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+375291234567")
                .status(ManagerStatus.BUSY)
                .maxCapacity(10)
                .currentLoad(10)
                .build();

        when(managerService.updateStatus(eq(1L), any(ManagerStatusUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/managers/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("BUSY"));

        verify(managerService)
                .updateStatus(eq(1L), any(ManagerStatusUpdateRequest.class));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        doNothing().when(managerService).delete(1L);

        mockMvc.perform(delete("/api/v1/managers/1"))
                .andExpect(status().isNoContent());

        verify(managerService).delete(1L);
    }

    @Test
    void create_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        ManagerRequest request = ManagerRequest.builder()
                .firstName("")
                .lastName("")
                .email("invalid-email")
                .phone("123")
                .build();

        mockMvc.perform(post("/api/v1/managers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phone").exists());

        verify(managerService, org.mockito.Mockito.never())
                .create(any(ManagerRequest.class));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .registerModule(new JavaTimeModule());
        }
    }
}
