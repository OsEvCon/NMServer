package service;

import DTO.ClientDTO;
import DTO.request.CreateClientRequest;
import DTO.request.DeleteClientsRequest;
import Service.ClientService;
import Service.SecurityUtils;
import exception.BusinessException;
import exception.ClientDataAccessException;
import exception.ClientSaveException;
import mapper.ClientMapper;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;


import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientServiceTest {

    @Mock
    ClientRepository clientRepository;

    @Mock
    VisitRepository visitRepository;

    @Mock
    SimpMessagingTemplate simpMessagingTemplate;

    @Mock
    ClientMapper clientMapper;

    @InjectMocks
    ClientService clientService;

    @BeforeEach
    void setUpLogging() {
        // Включаем DEBUG для конкретного логгера
        Logger logger = (Logger) LoggerFactory.getLogger(ClientService.class);
        logger.setLevel(Level.DEBUG);
    }

    //Тест на пустой список клиентов. Должен возвращать пустой список.
    @Test
    void getClientsTestWithEmptyClientList() {
        //Получение текущего мастера через mock SecurityUtils
        withSecurityUtilsMock(testMaster -> {
            when(clientRepository.findByMastersContaining(any()))
                    .thenReturn(List.of());

            List<ClientDTO> result = clientService.getClientsForCurrentMaster();

            assertThat(result.isEmpty());
        });
    }

    //Тест на список с одним клиентом
    @Test
    void getClientsTestWithOneClient() {
        withSecurityUtilsMock(testMaster -> {
            ClientDTO clientDTO1 = ClientDTO.builder().id(1).name("testClient1").build();

            when(clientRepository.findByMastersContaining(testMaster))
                    .thenReturn(getClients(1));

            when(clientMapper.toDTO(getClients(1)))
                    .thenReturn(List.of(clientDTO1));

            List<ClientDTO> result = clientService.getClientsForCurrentMaster();

            verify(clientRepository).findByMastersContaining(testMaster);
            verify(clientMapper).toDTO(getClients(1));

            assertThat(result.size()).isEqualTo(1);
            assertThat(result).containsExactly(clientDTO1);
        });
    }

    //Тест на список с несколькими клиентами. Должен возвращать заданных клиентов в правильном количестве
    @Test
    void getClientsTestWithClients() {

            withSecurityUtilsMock(testMaster -> {
                ClientDTO clientDTO1 = ClientDTO.builder().id(1).name("testClient1").build();
                ClientDTO clientDTO2 = ClientDTO.builder().id(2).name("testClient2").build();

                when(clientRepository.findByMastersContaining(testMaster))
                        .thenReturn(getClients(2));

                when(clientMapper.toDTO(getClients(2)))
                        .thenReturn(List.of(clientDTO1, clientDTO2));

                List<ClientDTO> result = clientService.getClientsForCurrentMaster();

                verify(clientRepository).findByMastersContaining(testMaster);
                verify(clientMapper).toDTO(getClients(2));

                assertThat(result.size()).isEqualTo(2);
                assertThat(result).containsExactly(clientDTO1, clientDTO2);
            });
    }

    //Тест на обработку исключения при ошибке БД
    @Test
    void getClientsTestWithRepositoryError() {
        withSecurityUtilsMock(testMaster -> {
            when(clientRepository.findByMastersContaining(any()))
                    .thenThrow(new DataAccessException("Database connection lost") {});

            assertThatThrownBy(() -> clientService.getClientsForCurrentMaster())
                    .isInstanceOf(ClientDataAccessException.class) // ← сервис должен бросить это
                    .hasMessageContaining("Не удалось получить данные клиентов") // ← сообщение кастомного исключения
                    .hasCauseInstanceOf(DataAccessException.class) // ← причина сохраняется
                    .hasRootCauseMessage("Database connection lost"); // ← оригинальное сообщение

            verify(clientRepository).findByMastersContaining(testMaster);
        });
    }

    /**
     * Тест на создание и сохранение клиента
     */
    @Test
    void createClientTest() {
        withSecurityUtilsMock(testMaster -> {
            CreateClientRequest request = CreateClientRequest.builder()
                    .name("testClient")
                    .phoneNumber("+71234567890")
                    .email("testClient@testMail.com")
                    .build();

            Client testClient = new Client();
            testClient.setId(1);
            testClient.setName(request.getName());
            testClient.setPhoneNumber(request.getPhoneNumber());
            testClient.setEmail(request.getEmail());
            testClient.setMasters(List.of(testMaster));

            when(clientRepository.existsByMastersContainingAndPhoneNumber(testMaster, request.getPhoneNumber()))
                    .thenReturn(false);

            when(clientMapper.toClient(request,  testMaster))
                    .thenReturn(testClient);

            when(clientRepository.save(testClient)).thenReturn(testClient);

            when(clientMapper.toDTO(testClient)).thenReturn(ClientDTO.builder().id(1).name("testClient")
                    .phoneNumber("+71234567890").email("testClient@testMail.com").build());

            ClientDTO savedClientDto = clientService.createClient(request);

            verify(clientRepository, times(1))
                    .existsByMastersContainingAndPhoneNumber(testMaster, request.getPhoneNumber());

            verify(clientMapper).toClient(request, testMaster);

            verify(clientRepository, times(1)).save(testClient);

            verify(simpMessagingTemplate).convertAndSend(
                    eq("/topic/clients.update"),
                    any(Map.class)
            );

            verify(clientMapper).toDTO(testClient);

            assertThat(savedClientDto.getId()).isEqualTo(1);
            assertThat(savedClientDto.getPhoneNumber()).isEqualTo(request.getPhoneNumber());
            assertThat(savedClientDto.getName()).isEqualTo(request.getName());
            assertThat(savedClientDto.getEmail()).isEqualTo(request.getEmail());
        });
    }

    /**
     * Тест на создание клиента с уже существующим номером телефона. Должно быть исключение
     */
    @Test
    void createClientWithDuplicatePhoneNumberTest() {
        withSecurityUtilsMock(testMaster -> {
            CreateClientRequest request = CreateClientRequest.builder()
                    .name("testClient")
                    .phoneNumber("+71234567890")
                    .build();

            when(clientRepository.existsByMastersContainingAndPhoneNumber(testMaster, request.getPhoneNumber()))
                    .thenReturn(true);

            assertThatThrownBy(() -> clientService.createClient(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("У вас уже есть клиент с телефоном ");
        });
    }

    /**
     * Тест на обработку ошибки БД при сохранении клиента
     */
    @Test
    void createClientWithRepositoryErrorTest() {
        withSecurityUtilsMock(testMaster -> {
            CreateClientRequest request = CreateClientRequest.builder()
                    .name("testClient")
                    .phoneNumber("+71234567890")
                    .build();

            Client testClient = new Client();
            testClient.setName(request.getName());
            testClient.setPhoneNumber(request.getPhoneNumber());

            when(clientRepository.save(any(Client.class)))
                    .thenThrow(new DataAccessException("Database connection lost") {});

            when(clientMapper.toClient(request,  testMaster))
                    .thenReturn(testClient);

            assertThatThrownBy(() -> clientService.createClient(request))
                    .isInstanceOf(ClientSaveException.class)
                    .hasMessageContaining("Не удалось сохранить клиента")
                    .hasCauseInstanceOf(DataAccessException.class) // ← причина сохраняется
                    .hasRootCauseMessage("Database connection lost"); // ← оригинальное сообщение
        });
    }

    /**
     * Тест на удаление клиентов
     */
    @Test
    void deleteMultipleClientsTest() {
        withSecurityUtilsMock(testMaster -> {
            DeleteClientsRequest deleteClientsRequest = new DeleteClientsRequest();
            deleteClientsRequest.setClientIds(List.of(0, 1, 2, 3, 4));

            List<Client> clientsForRemove = getClients(5);

            Map<Client, List<Visit>> clientToVisitsMap = new HashMap<>();
            for(int i = 0; i < 4; i++) {
                Client client = clientsForRemove.get(i);
                client.setMasters(new ArrayList<>(List.of(testMaster)));
                testMaster.addClient(client);

                List<Visit> visits = client.getVisits();
                clientToVisitsMap.put(client, visits);

                when(visitRepository.findVisitsByClient(client))
                        .thenReturn(Optional.of(visits));
            }

            when(clientRepository.findAllById(deleteClientsRequest.getClientIds()))
                    .thenReturn(clientsForRemove);

            clientService.deleteMultipleClients(deleteClientsRequest);

            // Проверка, что клиенты удалены из коллекции мастера
            assertThat(testMaster.getClients()).isEmpty();

            // Проверка, что clientRepository.delete вызывался 4 раза и что 5-го клиента не удаляли
            verify(clientRepository, times(4)).delete(any(Client.class));
            verify(clientRepository, never()).delete(clientsForRemove.get(4));

            // Проверка, что messagingTemplate вызывался
            verify(simpMessagingTemplate).convertAndSend(eq("/topic/clients.update"), any(Map.class));

            // Проверка, что визиты отвязаны у 4‑х клиентов
            for (int i = 0; i < 4; i++){
                Client client = clientsForRemove.get(i);
                List<Visit> visits = client.getVisits();

                // У всех визитов клиент должен быть Null
                visits.forEach(visit -> assertThat(visit.getClient()).isNull());
            }

            //Проверка, что у 5-го клиента визит не отвязан
            Client client5 =  clientsForRemove.get(4);
            client5.getVisits().forEach(visit -> assertThat(visit.getClient()).isEqualTo(client5));

            // Проверка вызова saveAll для 4х клиентов
            verify(visitRepository, times(4)).saveAll(anyList());

            // Проверка, что clientRepository.save() не вызывался т.к. клиенты удаляются полностью
            verify(clientRepository, never()).save(any(Client.class));
        });
    }


    List<Client> getClients(int count){
        List<Client> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Client client = new Client();
            client.setId(i);
            client.setName("testClient" + i);
            Visit visit = new Visit();
            visit.setId(i);
            visit.setClient(client);
            client.getVisits().add(visit);
            result.add(client);
        }
        return result;
    }

    /**
     * Метод для тестов с SecurityUtils
     */
    private void withSecurityUtilsMock(Consumer<Master> testLogic){
        try(MockedStatic<SecurityUtils> securityUtilsMockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            Master testMaster = new Master();
            testMaster.setId(1);
            testMaster.setName("testMaster");

            securityUtilsMockedStatic.when(SecurityUtils::getCurrentMaster).thenReturn(testMaster);

            testLogic.accept(testMaster);
        }
    }
}
