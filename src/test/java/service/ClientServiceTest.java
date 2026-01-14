package service;

import DTO.ClientDTO;
import DTO.request.CreateClientRequest;
import DTO.request.DeleteClientsRequest;
import DTO.request.UpdateClientRequest;
import Service.ClientService;
import Service.SecurityUtils;
import exception.BusinessException;
import exception.ClientDataAccessException;
import exception.ClientSaveException;
import exception.ResourceNotFoundException;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;


import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
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
     * Тест на удаление нескольких клиентов
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



    /**
     * Тест на удаление нескольких клиентов не принадлежащих текущему мастеру.
     *     Метод должен завершиться без ошибок.
     *     В логе должно быть сообщение "Попытка удалить клиентов, не принадлежащих мастеру".
     *     Клиенты не должны удаляться
     */
    @Test
    void deleteMultipleClientsTest_NoClientsBelongToMaster(CapturedOutput capturedOutput) {
        // Не добавлять клиентов мастеру
        DeleteClientsRequest deleteClientsRequest = new DeleteClientsRequest();
        deleteClientsRequest.setClientIds(List.of(0, 1, 2, 3, 4));

        List<Client> clientsForRemove = getClients(5);

        when(clientRepository.findAllById(deleteClientsRequest.getClientIds()))
                .thenReturn(clientsForRemove);

        clientService.deleteMultipleClients(deleteClientsRequest);

        // Проверка warning log
        assertThat(capturedOutput)
                .contains("Попытка удалить клиентов, не принадлежащих мастеру")
                .contains("WARN");

        // Проверка, что delete не вызывался
        verify(clientRepository, never()).delete(any(Client.class));

        // Проверка, что методы в detachClientsFromVisits() не вызывались
        verify(visitRepository, never()).findVisitsByClient(any(Client.class));
        verify(visitRepository, never()).saveAll(anyList());

        // Проверка, что визиты привязаны к клиентам и наоборот.
        clientsForRemove.forEach(client -> {
            assertThat(client.getVisits())
                    .isNotEmpty();

            client.getVisits().forEach(visit -> {
                assertThat(visit.getClient()).isEqualTo(client);
            });
        });
    }

    /**
     * Тест на удаление нескольких клиентов, которые принадлежат нескольким мастерам, у текущего мастера.
     * Клиент не должен удаляться полностью, а только у текущего мастера.
     */
    @Test
    void deleteMultipleClientsTest_ClientHasMultipleMasters() {
        withSecurityUtilsMock(testMaster -> {
            DeleteClientsRequest deleteClientsRequest = new DeleteClientsRequest();
            deleteClientsRequest.setClientIds(List.of(0, 1, 2, 3, 4));

            List<Client> clientsForRemove = getClients(5);

            // Создать второго мастера
            Master testMaster2 = new Master();
            testMaster2.setId(2);
            testMaster2.setName("testMaster2");

            // Добавить клиентам обоих мастеров и обоим мастерам всех клиентов
            for (Client client : clientsForRemove) {
                client.addMasters(testMaster, testMaster2);
                testMaster.addClient(client);
                testMaster2.addClient(client);
            }

            when(clientRepository.findAllById(deleteClientsRequest.getClientIds()))
                    .thenReturn(clientsForRemove);

            // Удалить клиентов у testMaster
            clientService.deleteMultipleClients(deleteClientsRequest);

            // Проверка, что clientRepository.save() вызывался, а не delete()
            verify(clientRepository, times(5)).save(any(Client.class));
            verify(clientRepository, never()).delete(any(Client.class));

            // Проверка, что messagingTemplate вызывался
            verify(simpMessagingTemplate).convertAndSend(eq("/topic/clients.update"), any(Map.class));

            // Проверка, что клиенты остались у второго мастера и мастер у клиентов
            clientsForRemove.forEach(client -> {
                assertThat(testMaster2.getClients()).contains(client);
                assertThat(client.getMasters()).contains(testMaster2);
            });

            //Проверка, что клиенты удалены у текущего мастера
            assertThat(testMaster.getClients()).isEmpty();

            //Проверка, что у клиентов нет текущего мастера
            clientsForRemove.forEach(client -> {
                assertThat(client.getMasters()).doesNotContain(testMaster);
            });
        });
    }

    /**
     * Тест на удаление несуществующих клиентов
     * Метод должен завершаться без ошибок
     * В логе должно быть сообщение "Клиенты с указанными ID не найдены"
     * Методы clientRepository.delete() и clientRepository.save() не должны вызываться
     */
    @Test
    void deleteMultipleClientsTest_NonExistentIds(CapturedOutput capturedOutput) {
        DeleteClientsRequest deleteClientsRequest = new DeleteClientsRequest();
        deleteClientsRequest.setClientIds(List.of(0, 1, 2, 3, 4));

        // findAllById вернет пустой список
        when(clientRepository.findAllById(deleteClientsRequest.getClientIds()))
                .thenReturn(Collections.emptyList());

        clientService.deleteMultipleClients(deleteClientsRequest);

        // Проверка warning log
        assertThat(capturedOutput)
                .contains("Клиенты с указанными ID не найдены")
                .contains("WARN");

        // Проверка, что delete и save не вызывались
        verify(clientRepository, never()).delete(any(Client.class));
        verify(clientRepository, never()).save(any(Client.class));
    }

    /**
     * Тест на удаление клиентов с пустым списком ID
     * Метод должен завершаться без ошибок
     * В репозитории не должно происходить изменений
     */
    @Test
    void deleteMultipleClientsTest_EmptyIdList() {
        withSecurityUtilsMock(testMaster -> {
            // Передать пустой список ID
            DeleteClientsRequest deleteClientsRequest = new DeleteClientsRequest();
            deleteClientsRequest.setClientIds(Collections.emptyList());

            clientService.deleteMultipleClients(deleteClientsRequest);

            // Проверка, что метод завершается без ошибок
            assertThatNoException();

            // Проверка, что в репозитории не было изменений
            verify(clientRepository, never()).save(any());
            verify(clientRepository, never()).delete(any());
        });

    }

    /**
     * Тест на обновление клиента с корректными данными (happy path)
     * Все поля должны обновляться корректно
     * Метод должен возвращать DTO с новыми данными
     * Должно отправляться корректное сообщение через websocket
     */
    @Test
    void updateClientTest_HappyPath() {
        withSecurityUtilsMock(testMaster -> {
            //Создать клиента для обновления
            Client clientForUpdate = getClients(1).get(0);
            clientForUpdate.setMasters(new ArrayList<>(List.of(testMaster)));

            //Создать запрос на обновление клиента
            UpdateClientRequest updateClientRequest = new UpdateClientRequest();
            updateClientRequest.setName("UpdatedTestClient0");
            updateClientRequest.setPhoneNumber("+71234567890");
            updateClientRequest.setEmail("testClient0@mail.ru");
            Integer clientId = 0;

            //Создать обновленного клиента
            Client updatedClientFromRepo = Client.builder()
                    .id(0)
                    .name("UpdatedTestClient0")
                    .phoneNumber("+71234567890")
                    .email("testClient0@mail.ru")
                    .masters(new  ArrayList<>(List.of(testMaster)))
                    .build();

            when(clientRepository.findByIdAndMastersContaining(0, testMaster))
                    .thenReturn(Optional.of(clientForUpdate));

            when(clientRepository.save(clientForUpdate))
                    .thenReturn(updatedClientFromRepo);

            when(clientMapper.toDTO(updatedClientFromRepo))
                    .thenReturn(ClientDTO.builder()
                            .id(updatedClientFromRepo.getId())
                            .name(updatedClientFromRepo.getName())
                            .phoneNumber(updatedClientFromRepo.getPhoneNumber())
                            .email(updatedClientFromRepo.getEmail())
                            .build());

            ClientDTO updatedClientDTO = clientService.updateClient(clientId, updateClientRequest);

            // Проверка, что messagingTemplate вызывался
            verify(simpMessagingTemplate).convertAndSend(eq("/topic/clients.update"), any(Map.class));

            // Проверка возвращаемого DTO
            assertThat(updatedClientDTO.getName()).isEqualTo("UpdatedTestClient0");
            assertThat(updatedClientDTO.getPhoneNumber()).isEqualTo("+71234567890");
            assertThat(updatedClientDTO.getEmail()).isEqualTo("testClient0@mail.ru");
        });
    }

    /**
     * Тест на обновление клиента который не существует в БД
     * Метод должен завершаться с ошибкой ResourceNotFoundException и сообщением.
     */
    @Test
    void updateClientTest_CLientNotFound() {
        withSecurityUtilsMock(testMaster -> {
            //Создать запрос на обновление клиента
            UpdateClientRequest updateClientRequest = new UpdateClientRequest();
            Integer clientId = 1234;

            when(clientRepository.findByIdAndMastersContaining(clientId, testMaster))
                    .thenReturn(Optional.empty());

            //Проверка, что метод падает с ошибкой и правильным сообщением
            assertThatThrownBy(() -> clientService.updateClient(clientId, updateClientRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(" не найден или не принадлежит вам");

            //Проверка, что save() не вызывался
            verify(clientRepository, never()).save(any(Client.class));

            //Проверка, что сообщение не отправляется
            verify(simpMessagingTemplate, never()).convertAndSend(anyString(), any(Map.class));
        });
    }

    /**
     * Тест на обновление клиента с номером телефона, который уже существует у другого клиента
     * Метод должен завершаться с ошибкой BusinessException и сообщением
     * Метод clientRepository.save() не должен вызываться
     * Сообщение не должно отправляться
     */
    @Test
    void updateClientTest_DublicatePhoneNumber() {
        withSecurityUtilsMock(testMaster -> {
            //Создать запрос на обновление клиента
            UpdateClientRequest updateClientRequest = new UpdateClientRequest();
            updateClientRequest.setPhoneNumber("+71234567890");
            Integer clientId = 1234;

            when(clientRepository.findByIdAndMastersContaining(clientId, testMaster))
                    .thenReturn(Optional.of(Client.builder()
                            .id(1234)
                            .phoneNumber("+71234567891")
                            .build()));

            when(clientRepository.existsByMastersAndPhoneNumberExcludingId(testMaster, updateClientRequest.getPhoneNumber(), clientId))
                    .thenReturn(true);

            //Проверка, что метод завершается с BusinessException и сообщением
            assertThatThrownBy(() -> clientService.updateClient(clientId, updateClientRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("У вас уже есть клиент с телефоном: ");

            //Проверка, что save() не вызывался
            verify(clientRepository, never()).save(any(Client.class));

            //Проверка, что сообщение не отправляется
            verify(simpMessagingTemplate, never()).convertAndSend(anyString(), any(Map.class));
        });
    }

    /**
     * Метод для создания тестовых клиентов
     * @param count - количество клиентов
     */
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
            Master testMaster1 = new Master();
            testMaster1.setId(1);
            testMaster1.setName("testMaster1");

            securityUtilsMockedStatic.when(SecurityUtils::getCurrentMaster).thenReturn(testMaster1);

            testLogic.accept(testMaster1);
        }
    }
}
