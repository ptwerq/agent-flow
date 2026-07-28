package com.agentflow.clientservice.unit;

import com.agentflow.clientservice.dto.request.ClientRequest;
import com.agentflow.clientservice.dto.response.ClientResponse;
import com.agentflow.clientservice.dto.request.ClientStatusUpdateRequest;
import com.agentflow.clientservice.dto.request.ClientUpdateRequest;
import com.agentflow.clientservice.entity.Client;
import com.agentflow.clientservice.entity.DealStatus;
import com.agentflow.clientservice.exception.NotFoundException;
import com.agentflow.clientservice.mapper.ClientMapper;
import com.agentflow.clientservice.repository.ClientRepository;
import com.agentflow.clientservice.service.ClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ClientServiceTest {
    @InjectMocks
    private ClientService clientService;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @Test
    public void getClientById_Success() {
        Long id = 1L;
        Client client = new Client();
        client.setId(id);
        when(clientRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.of(client));

        ClientResponse clientResponse = ClientResponse.builder()
                .id(id)
                .build();

        when(clientMapper.toResponse(any(Client.class))).thenReturn(clientResponse);

        ClientResponse result = clientService.getById(id);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    public void getClientById_NotFound_ThrowsException() {
        Long id = 1L;
        when(clientRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            clientService.getById(id);
        });
    }

    @Test
    public void createClient_Success() {
        ClientRequest clientRequest = ClientRequest.builder()
                .firstName("Name")
                .lastName("Surname")
                .email("name@gmail.com")
                .phone("+375291234567")
                .build();

        Client client = Client.builder()
                .id(1L)
                .firstName("Name")
                .lastName("Surname")
                .email("name@gmail.com")
                .phone("+375291234567")
                .build();

        ClientResponse expectedResponse = ClientResponse.builder()
                .id(1L)
                .firstName("Name")
                .lastName("Surname")
                .email("name@gmail.com")
                .phone("+375291234567")
                .build();

        when(clientMapper.toEntity(any(ClientRequest.class))).thenReturn(client);
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(clientMapper.toResponse(any(Client.class))).thenReturn(expectedResponse);


        ClientResponse actualResponse = clientService.create(clientRequest);
        assertNotNull(actualResponse);
        assertEquals(1L, actualResponse.getId());
        assertEquals("Name", actualResponse.getFirstName());
        assertEquals("+375291234567", actualResponse.getPhone());
    }

    @Test
    public void updateClient_Success() {
        Long id = 1L;
        ClientUpdateRequest clientUpdateRequest = ClientUpdateRequest.builder()
                .firstName("New Name")
                .lastName("New Surname")
                .build();
        Client existingClient = Client.builder()
                .id(id)
                .firstName("Old Name")
                .lastName("Old Surname")
                .build();
        ClientResponse clientResponse = ClientResponse.builder()
                .id(id)
                .firstName("New Name")
                .lastName("New Surname")
                .build();
        when(clientRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.of(existingClient));
        when(clientMapper.toResponse(any(Client.class))).thenReturn(clientResponse);

        ClientResponse updated = clientService.update(id, clientUpdateRequest);
        assertNotNull(updated);
        assertEquals("New Name", updated.getFirstName());
        assertEquals("New Surname", updated.getLastName());
    }

    @Test
    public void updateClient_NotFound_ThrowsException() {
        Long id = 1L;
        ClientUpdateRequest clientUpdateRequest = ClientUpdateRequest.builder()
                .firstName("New Name")
                .lastName("New Surname")
                .build();
        when(clientRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> {
            clientService.update(id, clientUpdateRequest);
        });
    }

    @Test
    public void updateClientStatus_Success() {
        Long id = 1L;
        DealStatus newStatus = DealStatus.IN_PROGRESS;
        ClientStatusUpdateRequest clientStatusUpdateRequest = ClientStatusUpdateRequest.builder()
                .dealStatus(newStatus)
                .comment("Comment")
                .build();
        Client existingClient = Client.builder()
                .id(id)
                .dealStatus(DealStatus.NEW)
                .build();
        ClientResponse clientResponse = ClientResponse.builder()
                .id(id)
                .dealStatus(newStatus)
                .build();
        when(clientRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.of(existingClient));
        when(clientMapper.toResponse(any(Client.class))).thenReturn(clientResponse);

        ClientResponse updated = clientService.updateStatus(id, clientStatusUpdateRequest);
        assertNotNull(updated);
        assertEquals(newStatus, clientResponse.getDealStatus());
    }

    @Test
    public void updateClientStatus_NotFound_ThrowsException() {
        Long id = 1L;
        ClientStatusUpdateRequest clientStatusUpdateRequest = ClientStatusUpdateRequest.builder()
                .dealStatus(DealStatus.IN_PROGRESS)
                .comment("Comment")
                .build();

        when(clientRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            clientService.updateStatus(id, clientStatusUpdateRequest);
        });
    }
}
