package com.agentflow.managerservice.controller;

import com.agentflow.managerservice.dto.request.ManagerRequest;
import com.agentflow.managerservice.dto.request.ManagerStatusUpdateRequest;
import com.agentflow.managerservice.dto.request.ManagerUpdateRequest;
import com.agentflow.managerservice.dto.response.ManagerResponse;
import com.agentflow.managerservice.service.ManagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/managers")
@RequiredArgsConstructor
@Tag(name = "Manager service controller")
public class ManagerController {

    private final ManagerService managerService;

    @Operation(summary = "Create manager")
    @PostMapping
    public ResponseEntity<ManagerResponse> create(@Valid @RequestBody ManagerRequest managerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managerService.create(managerRequest));
    }

    @Operation(summary = "Get manager by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ManagerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(managerService.getById(id));
    }

    @Operation(summary = "Get all managers with pagination")
    @GetMapping
    public ResponseEntity<Page<ManagerResponse>> getAll(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(managerService.getAll(pageable));
    }

    @Operation(summary = "Update manager profile")
    @PutMapping("/{id}")
    public ResponseEntity<ManagerResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody ManagerUpdateRequest managerUpdateRequest) {
        return ResponseEntity.ok(managerService.update(id, managerUpdateRequest));
    }

    @Operation(summary = "Update manager status")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ManagerResponse> updateStatus(@PathVariable Long id,
                                                        @Valid @RequestBody ManagerStatusUpdateRequest managerStatusUpdateRequest) {
        return ResponseEntity.ok(managerService.updateStatus(id, managerStatusUpdateRequest));
    }

    @Operation(summary = "Delete manager")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        managerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
