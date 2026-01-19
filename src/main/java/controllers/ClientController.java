package controllers;

import DTO.ClientDTO;
import DTO.request.CreateClientRequest;
import DTO.request.DeleteClientsRequest;
import DTO.request.UpdateClientRequest;
import service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/clients")
@Slf4j
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping()
    public ResponseEntity<List<ClientDTO>> getClients() {
        log.info("GET /api/clients - запрос списка клиентов");

        List<ClientDTO> clients = clientService.getClientsForCurrentMaster();

        return ResponseEntity.ok(clients);
    }

    @PostMapping()
    public ResponseEntity<ClientDTO> createClient(@Valid @RequestBody CreateClientRequest request){
    log.info("Создание нового клиента {}", request.getName());

    ClientDTO createdClient = clientService.createClient(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdClient.getId())
                .toUri();

    return ResponseEntity.created(location).body(createdClient);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteClients(@Valid @RequestBody DeleteClientsRequest request) {
        log.info("Удаление клиентов: {}", request.getClientIds());

        clientService.deleteMultipleClients(request);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDTO> updateClient(@PathVariable Integer id, @Valid @RequestBody UpdateClientRequest request){
        log.info("PUT /api/clients/{} - обновление клиента", id);

        ClientDTO updatedClient = clientService.updateClient(id,request);

        return ResponseEntity.ok(updatedClient);
    }

}
