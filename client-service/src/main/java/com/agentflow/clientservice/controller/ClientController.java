package com.agentflow.clientservice.controller;

import com.agentflow.clientservice.dto.request.ClientRequest;
import com.agentflow.clientservice.dto.response.ClientResponse;
import com.agentflow.clientservice.dto.request.ClientStatusUpdateRequest;
import com.agentflow.clientservice.dto.request.ClientUpdateRequest;
import com.agentflow.clientservice.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Client service controller")
public class ClientController {
    private final ClientService clientService;

    @Operation(summary = "Create client")
    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest clientRequest) {
        ClientResponse clientResponse = clientService.create(clientRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(clientResponse);
    }

    @Operation(summary = "Get client by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getById(@PathVariable Long id) {
        ClientResponse clientResponse = clientService.getById(id);
        return ResponseEntity.ok(clientResponse);
    }

    @Operation(summary = "Get all clients")
    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAll() {
        List<ClientResponse> clientResponseList = clientService.getAll();
        return ResponseEntity.ok(clientResponseList);
    }

    @Operation(summary = "Update client profile")
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody ClientUpdateRequest clientUpdateRequest) {
        ClientResponse clientResponse = clientService.update(id, clientUpdateRequest);
        return ResponseEntity.ok(clientResponse);
    }

    @Operation(summary = "Update client status")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ClientResponse> updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody ClientStatusUpdateRequest clientStatusUpdateRequest) {
        ClientResponse clientResponse = clientService.updateStatus(id, clientStatusUpdateRequest);
        return ResponseEntity.ok(clientResponse);
    }

    @Operation(summary = "Delete client")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
