package com.agentflow.clientservice.unit;
import com.agentflow.clientservice.dto.event.ClientCreatedEvent;
import com.agentflow.clientservice.dto.request.ClientRequest;
import com.agentflow.clientservice.dto.request.ClientStatusUpdateRequest;
import com.agentflow.clientservice.dto.request.ClientUpdateRequest;
import com.agentflow.clientservice.dto.response.ClientResponse;
import com.agentflow.clientservice.entity.Client;
import com.agentflow.clientservice.entity.DealStatus;
import com.agentflow.clientservice.entity.outbox.OutboxEvent;
import com.agentflow.clientservice.exception.NotFoundException;
import com.agentflow.clientservice.mapper.ClientMapper;
import com.agentflow.clientservice.mapper.OutboxMapper;
import com.agentflow.clientservice.repository.ClientRepository;
import com.agentflow.clientservice.repository.OutboxRepository;
import com.agentflow.clientservice.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) class ClientServiceTest {
    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @Mock
    private OutboxMapper outboxMapper;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private ClientService clientService;

    private Client client;

    @BeforeEach
    void setUp() {
        client = Client.builder()
                .id(1L)
                .firstName("Ivan")
                .lastName("Ivanov")
                .email("ivan@test.com")
                .phone("+375291234567")
                .dealStatus(DealStatus.NEW)
                .build();
    }

    @Test
    void shouldCreateClientAndOutboxEvent() {
        // given
        ClientRequest request = mock(ClientRequest.class);
        ClientResponse response = mock(ClientResponse.class);
        ClientCreatedEvent event = mock(ClientCreatedEvent.class);
        OutboxEvent outboxEvent = mock(OutboxEvent.class);

        when(clientMapper.toEntity(request))
                .thenReturn(client);

        when(clientRepository.save(client))
                .thenReturn(client);

        when(clientMapper.toClientCreatedEvent(client))
                .thenReturn(event);

        when(outboxMapper.toEntity(event, client.getId()))
                .thenReturn(outboxEvent);

        when(clientMapper.toResponse(client))
                .thenReturn(response);

        // when
        ClientResponse result = clientService.create(request);

        // then
        assertSame(response, result);

        verify(clientMapper).toEntity(request);
        verify(clientRepository).save(client);
        verify(clientMapper).toClientCreatedEvent(client);
        verify(outboxMapper).toEntity(event, client.getId());
        verify(outboxRepository).save(outboxEvent);
        verify(clientMapper).toResponse(client);
    }

    @Test
    void shouldAssignManager() {
        // given
        Long clientId = 1L;
        Long managerId = 10L;

        when(clientRepository.findByIdAndIsDeletedFalse(clientId))
                .thenReturn(Optional.of(client));

        // when
        clientService.assignManager(clientId, managerId);

        // then
        assertEquals(managerId, client.getManagerId());
        assertEquals(DealStatus.IN_PROGRESS, client.getDealStatus());
    }

    @Test
    void shouldThrowExceptionWhenAssigningManagerToNonExistingClient() {
        // given
        Long clientId = 1L;
        Long managerId = 10L;

        when(clientRepository.findByIdAndIsDeletedFalse(clientId))
                .thenReturn(Optional.empty());

        // when + then
        assertThrows(
                NotFoundException.class,
                () -> clientService.assignManager(clientId, managerId)
        );

        verify(clientRepository).findByIdAndIsDeletedFalse(clientId);
    }

    @Test
    void shouldReleaseManager() {
        // given
        client.setManagerId(10L);
        client.setDealStatus(DealStatus.IN_PROGRESS);

        when(clientRepository.findByIdAndIsDeletedFalse(client.getId()))
                .thenReturn(Optional.of(client));

        // when
        clientService.


                releaseManager(client.getId());

        // then
        assertNull(client.getManagerId());
        assertEquals(DealStatus.SUCCESS, client.getDealStatus());
    }

    @Test
    void shouldGetClientById() {
        // given
        ClientResponse response = mock(ClientResponse.class);

        when(clientRepository.findByIdAndIsDeletedFalse(client.getId()))
                .thenReturn(Optional.of(client));

        when(clientMapper.toResponse(client))
                .thenReturn(response);

        // when
        ClientResponse result = clientService.getById(client.getId());

        // then
        assertSame(response, result);
        verify(clientMapper).toResponse(client);
    }

    @Test
    void shouldThrowExceptionWhenClientNotFoundById() {
        // given
        Long clientId = 1L;

        when(clientRepository.findByIdAndIsDeletedFalse(clientId))
                .thenReturn(Optional.empty());

        // when + then
        assertThrows(
                NotFoundException.class,
                () -> clientService.getById(clientId)
        );
    }

    @Test
    void shouldGetAllClients() {
        // given
        Client secondClient = Client.builder()
                .id(2L)
                .firstName("Petr")
                .lastName("Petrov")
                .email("petr@test.com")
                .phone("+375291111111")
                .dealStatus(DealStatus.NEW)
                .build();

        ClientResponse firstResponse = mock(ClientResponse.class);
        ClientResponse secondResponse = mock(ClientResponse.class);

        when(clientRepository.findAllByIsDeletedFalse())
                .thenReturn(List.of(client, secondClient));

        when(clientMapper.toResponse(client))
                .thenReturn(firstResponse);

        when(clientMapper.toResponse(secondClient))
                .thenReturn(secondResponse);

        // when
        List<ClientResponse> result = clientService.getAll();

        // then
        assertEquals(2, result.size());
        assertSame(firstResponse, result.get(0));
        assertSame(secondResponse, result.get(1));

        verify(clientRepository).findAllByIsDeletedFalse();
        verify(clientMapper).toResponse(client);
        verify(clientMapper).toResponse(secondClient);
    }

    @Test
    void shouldDeleteClient() {
        // given
        when(clientRepository.findByIdAndIsDeletedFalse(client.getId()))
                .thenReturn(Optional.of(client));

        // when
        clientService.delete(client.getId());

        // then
        assertTrue(client.isDeleted());
    }

    @Test
    void shouldUpdateClient() {
        // given
        ClientUpdateRequest request = mock(ClientUpdateRequest.class);
        ClientResponse response = mock(ClientResponse.class);

        when(clientRepository.findByIdAndIsDeletedFalse(client.getId()))
                .thenReturn(Optional.of(client));

        when(clientMapper.toResponse(client))
                .thenReturn(response);

        // when
        ClientResponse result = clientService.update(client.getId(), request);

        // then
        assertSame(response, result);

        verify(clientMapper)
                .updateEntityFromUpdateRequest(request, client);

        verify(clientMapper)
                .toResponse(client);
    }

    @Test
    void shouldUpdateClientStatus() {
        // given
        ClientStatusUpdateRequest request = mock(ClientStatusUpdateRequest.class);
        ClientResponse response = mock(ClientResponse.class);

        when(clientRepository.findByIdAndIsDeletedFalse(client.getId()))
                .thenReturn(Optional.of(client));

        when(clientMapper.toResponse(client))
                .thenReturn(response);

        // when
        ClientResponse result =
                clientService.updateStatus(client.getId(), request);

        // then
        assertSame(response, result);

        verify(clientMapper)
                .updateEntityFromStatusRequest(request, client);

        verify(clientMapper)
                .toResponse(client);
    }
}