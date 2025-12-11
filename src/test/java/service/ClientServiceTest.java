package service;

import DTO.ClientDTO;
import Service.ClientService;
import Service.SecurityUtils;
import mapper.ClientMapper;
import model.Client;
import model.ClientRepository;
import model.Master;
import model.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;


import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            Master testMaster = new Master();
            testMaster.setId(1);
            testMaster.setName("getClientsTestMaster");
            securityUtilsMockedStatic.when(SecurityUtils::getCurrentMaster).thenReturn(testMaster);


            when(clientRepository.findByMastersContaining(any()))
                    .thenReturn(List.of());

            List<ClientDTO> result = clientService.getClientsForCurrentMaster();

            assertThat(result.isEmpty());
        }
    }

    //Тест на список с одним клиентом
    @Test
    void getClientsTestWithOneClient() {
        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            Master testMaster = new Master();
            testMaster.setId(1);
            testMaster.setName("getClientsTestMaster");

            Client client1 = new Client();
            client1.setId(1);
            client1.setName("testClient1");

            ClientDTO clientDTO1 = ClientDTO.builder().id(1).name("testClient1").build();

            securityUtilsMockedStatic.when(SecurityUtils::getCurrentMaster).thenReturn(testMaster);

            when(clientRepository.findByMastersContaining(testMaster))
                    .thenReturn(List.of(client1));

            when(clientMapper.toDTOList(List.of(client1)))
                    .thenReturn(List.of(clientDTO1));

            List<ClientDTO> result = clientService.getClientsForCurrentMaster();

            verify(clientRepository).findByMastersContaining(testMaster);
            verify(clientMapper).toDTOList(List.of(client1));

            assertThat(result.size()).isEqualTo(1);
            assertThat(result).containsExactly(clientDTO1);
        }
    }

    //Тест на список с несколькими клиентами. Должен возвращать заданных клиентов в правильном количестве
    @Test
    void getClientsTestWithClients() {
        try (MockedStatic<SecurityUtils> securityUtilsMockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            Master testMaster = new Master();
            testMaster.setId(1);
            testMaster.setName("getClientsTestMaster");

            Client client1 = new Client();
            Client client2 = new Client();
            client1.setId(1);
            client2.setId(2);
            client1.setName("testClient1");
            client2.setName("testClient2");

            ClientDTO clientDTO1 = ClientDTO.builder().id(1).name("testClient1").build();
            ClientDTO clientDTO2 = ClientDTO.builder().id(2).name("testClient2").build();

            securityUtilsMockedStatic.when(SecurityUtils::getCurrentMaster).thenReturn(testMaster);

            when(clientRepository.findByMastersContaining(testMaster))
                    .thenReturn(List.of(client1, client2));

            when(clientMapper.toDTOList(List.of(client1, client2)))
                    .thenReturn(List.of(clientDTO1, clientDTO2));

            List<ClientDTO> result = clientService.getClientsForCurrentMaster();

            verify(clientRepository).findByMastersContaining(testMaster);
            verify(clientMapper).toDTOList(List.of(client1, client2));

            assertThat(result.size()).isEqualTo(2);
            assertThat(result).containsExactly(clientDTO1, clientDTO2);
        }
    }
}
