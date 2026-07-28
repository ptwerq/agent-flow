package com.agentflow.clientservice.e2e;

import com.agentflow.clientservice.dto.request.ClientRequest;
import com.agentflow.clientservice.dto.response.ClientResponse;
import com.agentflow.clientservice.dto.request.ClientStatusUpdateRequest;
import com.agentflow.clientservice.dto.request.ClientUpdateRequest;
import com.agentflow.clientservice.entity.DealStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;


import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class ClientE2ETest {
    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void clientLifecycle_Success() {
        ClientRequest clientRequest = ClientRequest.builder()
                .firstName("Name")
                .lastName("Surname")
                .email("name@gmail.com")
                .phone("+375291234567")
                .build();

        ClientResponse createdClient = webTestClient.post()
                .uri("/api/v1/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(clientRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ClientResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(createdClient).isNotNull();
        assertThat(createdClient.getId()).isNotNull();
        assertThat(createdClient.getFirstName()).isEqualTo("Name");

        Long clientId = createdClient.getId();

        webTestClient.get()
                .uri("/api/v1/clients/{id}", clientId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(clientId)
                .jsonPath("$.email").isEqualTo("name@gmail.com");

        ClientStatusUpdateRequest clientStatusUpdateRequest = ClientStatusUpdateRequest.builder()
                .dealStatus(DealStatus.IN_PROGRESS)
                .build();

        ClientResponse clientWithUpdatedStatus = webTestClient.patch()
                .uri("/api/v1/clients/{id}/status", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(clientStatusUpdateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ClientResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(clientWithUpdatedStatus).isNotNull();
        assertThat(clientWithUpdatedStatus.getDealStatus()).isEqualTo(DealStatus.IN_PROGRESS);

        ClientUpdateRequest clientUpdateRequest = ClientUpdateRequest.builder()
                .firstName("New Name")
                .lastName("New Surname")
                .email("email2@gmail.com")
                .phone("+375123456789")
                .build();

        ClientResponse updatedClient = webTestClient.put()
                .uri("/api/v1/clients/{id}", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(clientUpdateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ClientResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(updatedClient).isNotNull();
        assertThat(updatedClient.getId()).isEqualTo(clientId);
        assertThat(updatedClient.getFirstName()).isEqualTo("New Name");
        assertThat(updatedClient.getLastName()).isEqualTo("New Surname");
    }
}
