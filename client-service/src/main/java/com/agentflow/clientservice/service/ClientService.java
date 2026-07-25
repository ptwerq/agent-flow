package com.agentflow.clientservice.service;

import com.agentflow.clientservice.dto.ClientRequest;
import com.agentflow.clientservice.dto.ClientResponse;
import com.agentflow.clientservice.dto.ClientStatusUpdateRequest;
import com.agentflow.clientservice.dto.ClientUpdateRequest;
import com.agentflow.clientservice.entity.Client;
import com.agentflow.clientservice.exception.NotFoundException;
import com.agentflow.clientservice.mapper.ClientMapper;
import com.agentflow.clientservice.repository.ClientRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientService {
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Transactional
    public ClientResponse create(ClientRequest clientRequest) {
        Client client = clientMapper.toEntity(clientRequest);
        Client savedClient = clientRepository.save(client);
        return clientMapper.toResponse(savedClient);
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
