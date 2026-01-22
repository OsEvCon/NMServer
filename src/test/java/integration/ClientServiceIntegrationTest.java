package integration;

import DTO.ClientDTO;
import DTO.request.CreateClientRequest;
import exception.BusinessException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import service.ClientService;
import service.SecurityService;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = MySpringBootApplication.class)
@ActiveProfiles("test")
@Transactional  // Каждый тест в транзакции, откат после теста
public class ClientServiceIntegrationTest {

    @Autowired
    private ClientService clientService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private MasterRepository masterRepository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @MockBean
    private SecurityService securityService;

    private Master testMaster;

    @BeforeEach
    public void setUp() {
        //Очистка БД
        clientRepository.deleteAll();
        masterRepository.deleteAll();

        //Создание тестового мастера
        testMaster = new Master();
        testMaster.setName("testMaster");
        testMaster.setEmail("testMaster@mail.ru");
        testMaster = masterRepository.save(testMaster);


        // Мокаем SecurityUtils.getCurrentMaster()
        when(securityService.getCurrentMasterOrThrow()).thenReturn(testMaster);

        // Мокаем отправку сообщений WebSocket
        doNothing().when(messagingTemplate).convertAndSend(any(String.class), any(Object.class));
    }

    @Nested
    @DisplayName("Create Client Tests")
    class CreateClientTests {

        @Test
        void createClientTest_shouldCreateClient() {
            CreateClientRequest request = CreateClientRequest.builder()
                    .name("testClient")
                    .phoneNumber("+71234567890")
                    .email("testClient@mail.ru")
                    .build();

            ClientDTO result = clientService.createClient(request);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("testClient");
            assertThat(result.getPhoneNumber()).isEqualTo("+71234567890");
            assertThat(result.getEmail()).isEqualTo("testClient@mail.ru");

            //Проверка, что клиент сохранен в БД
            List<Client> clients = (List<Client>) clientRepository.findAll();
            assertThat(clients).hasSize(1);
            assertThat(clients.get(0).getName()).isEqualTo("testClient");
            assertThat(clients.get(0).getMasters().contains(testMaster)).isTrue();

            // Проверка, что клиент появился у мастера при повторном получении мастера из БД
            Master master = masterRepository.findByEmail("testMaster@mail.ru").orElseThrow();
            Set<Client> masterClients = master.getClients();
            assertThat(masterClients).hasSize(1);
            assertThat(masterClients.iterator().next().getName()).isEqualTo("testClient");
        }

        /**
         * Тест сохранения клиента с уже существующим номером.
         * Должно возникать BusinessException
         * Клиент не должен сохраняться
         */
        @Test
        void createClientTest_DuplicatePhoneNumber() {
            //Создание запроса
            CreateClientRequest request = CreateClientRequest.builder()
                    .name("testClient")
                    .phoneNumber("+71234567890")
                    .email("testClient@mail.ru")
                    .build();

            //Создание и сохранение у мастера клиента с номером телефона из запроса
            Client client = Client.builder()
                    .name("testClient2")
                    .phoneNumber("+71234567890")
                    .email("testClient2@mail.ru")
                    .build();

            testMaster.addClient(client);
            clientRepository.save(client);
            masterRepository.save(testMaster);

            //Проверка, что метод падает с ошибкой
            assertThatThrownBy(()-> clientService.createClient(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("У вас уже есть клиент с телефоном +71234567890");

            //Проверка, что клиент из запроса не сохраняется
            assertThat(clientRepository.findAll()).hasSize(1);
            assertThat(clientRepository.findAll().iterator().next().getName()).isEqualTo("testClient2");
        }

        /**
         * Тест на Создание клиента с тем же телефоном для ДРУГОГО мастера (разные мастера могут иметь клиентов с одинаковыми телефонами)
         * Оба клиента с одинаковым номером телефона должны быть сохранены у разных мастеров
         */
        @Test
        void createClient_samePhoneDifferentMaster(){
            //Создание запроса
            CreateClientRequest request = CreateClientRequest.builder()
                    .name("testClient")
                    .phoneNumber("+71234567890")
                    .email("testClient@mail.ru")
                    .build();

            //Создание и сохранение второго мастера
            Master master = new Master();
            master.setName("testMaster2");
            master.setEmail("testMaster2@mail.ru");
            master = masterRepository.save(master);


            //Сохранение клиента из запроса для testMaster
            clientService.createClient(request);

            //Мок SecurityService для возврата второго мастера
            when(securityService.getCurrentMasterOrThrow()).thenReturn(master);

            //Сохранение клиента из запроса для второго мастера
            clientService.createClient(request);

            //Проверка, что в БД один клиент
            assertThat(clientRepository.findAll()).hasSize(1);

            // 4. Проверяем что клиент привязан к двум мастерам
            Client client = clientRepository.findClientByPhoneNumber("+71234567890").orElseThrow();
            assertThat(client.getMasters()).hasSize(2);
            assertThat(client.getMasters()).contains(testMaster, master);
        }
    }

}
