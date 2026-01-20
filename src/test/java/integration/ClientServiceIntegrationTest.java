package integration;

import DTO.ClientDTO;
import DTO.request.CreateClientRequest;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import service.ClientService;
import service.SecurityService;


import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

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
            Set<Client> masterClients = testMaster.getClients();
            assertThat(masterClients).hasSize(1);
            assertThat(masterClients.iterator().next().getName()).isEqualTo("testClient");
        }
    }


}
