package com.agentflow.clientservice.service;

import com.agentflow.clientservice.dto.event.ClientCreatedEvent;
import com.agentflow.clientservice.dto.request.ClientRequest;
import com.agentflow.clientservice.dto.response.ClientResponse;
import com.agentflow.clientservice.dto.request.ClientStatusUpdateRequest;
import com.agentflow.clientservice.dto.request.ClientUpdateRequest;
import com.agentflow.clientservice.entity.Client;
import com.agentflow.clientservice.entity.DealStatus;
import com.agentflow.clientservice.entity.outbox.OutboxEvent;
import com.agentflow.clientservice.exception.NotFoundException;
import com.agentflow.clientservice.mapper.ClientMapper;
import com.agentflow.clientservice.mapper.OutboxMapper;
import com.agentflow.clientservice.repository.ClientRepository;
import com.agentflow.clientservice.repository.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientService {
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final OutboxMapper outboxMapper;
    private final OutboxRepository outboxRepository;

    @Transactional
    public ClientResponse create(ClientRequest clientRequest) {
        Client client = clientMapper.toEntity(clientRequest);
        Client savedClient = clientRepository.save(client);

        ClientCreatedEvent clientCreatedEvent = clientMapper.toClientCreatedEvent(savedClient);
        OutboxEvent outboxEvent = outboxMapper.toEntity(clientCreatedEvent, savedClient.getId());
        outboxRepository.save(outboxEvent);

        return clientMapper.toResponse(savedClient);
    }

    @Transactional
    public void assignManager(Long clientId, Long managerId) {
        Client client = getEntityById(clientId);
        client.setManagerId(managerId);
        client.setDealStatus(DealStatus.IN_PROGRESS);
    }

    @Transactional
    public void releaseManager(Long clientId) {
        Client client = getEntityById(clientId);
        client.setManagerId(null);
        client.setDealStatus(DealStatus.SUCCESS);
    }

    public ClientResponse getById(Long id) {
        Client client = getEntityById(id);
        return clientMapper.toResponse(client);
    }

    public List<ClientResponse> getAll() {
        return clientRepository.findAllByIsDeletedFalse()
                .stream()
                .map(clientMapper::toResponse)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Client client = getEntityById(id);
        client.setDeleted(true);
    }

    @Transactional
    public ClientResponse update(Long id,
                                 ClientUpdateRequest clientUpdateRequest) {
        Client client = getEntityById(id);
        clientMapper.updateEntityFromUpdateRequest(clientUpdateRequest, client);
        return clientMapper.toResponse(client);
    }

    @Transactional
    public ClientResponse updateStatus(Long id,
                                       ClientStatusUpdateRequest clientStatusUpdateRequest) {
        Client client = getEntityById(id);
        clientMapper.updateEntityFromStatusRequest(clientStatusUpdateRequest, client);
        return clientMapper.toResponse(client);
    }

    private Client getEntityById(Long id) {
        return clientRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Client not found: " + id));
    }
}
