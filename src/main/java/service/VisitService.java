package service;

import DTO.VisitDTO;
import DTO.request.CreateVisitRequest;
import exception.ResourceNotFoundException;
import exception.VisitDataAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mapper.VisitMapper;
import model.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class VisitService {
    private final VisitMapper visitMapper;
    private final VisitRepository visitRepository;
    private final MasterRepository masterRepository;
    private final ClientRepository clientRepository;
    private final ProcedureRepository procedureRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<VisitDTO> getVisits() {
        log.info("Получение списка визитов для текущего мастера");

        try {
            Integer masterId = getCurrentMaster().getId();

            List<Visit> visits = visitRepository.findVisitsByMasterId(masterId);

            log.debug("Найдено {} визитов для мастера c ID: {}", visits.size(), masterId);
            return visitMapper.toDTO(visits);
        } catch (Exception e) {
            log.error("Ошибка получения визитов", e);
            throw new VisitDataAccessException(e);
        }
    }

    @Transactional
    public VisitDTO createVisit(CreateVisitRequest request) {
        Client clientForVisit = loadClientIfProvided(request.getClientId());
        List<Procedure> proceduresForVisit = loadProceduresIfProvided(request.getProcedureIds());

        Visit visitForCreate = visitMapper.toEntity(request, getCurrentMaster(), clientForVisit, proceduresForVisit);

        Visit savedVisit = visitRepository.save(visitForCreate);

        messagingTemplate.convertAndSend("/topic/visits.update",
                Map.of(
                        "type", "CREATED",
                        "visit", savedVisit
                ));

        return visitMapper.toDTO(savedVisit);
    }

    private Master getCurrentMaster() {
        return SecurityUtils.getCurrentMaster();
    }

    private Client loadClientIfProvided(Integer clientId) {
        if (clientId == null){
            return null;
        }

        return clientRepository.findClientById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Клиент с ID: %s не найден".formatted(clientId)));
    }

    private List<Procedure> loadProceduresIfProvided(List<Integer> procedureIds) {
        if (procedureIds == null || procedureIds.isEmpty()){
            return Collections.emptyList();
        }

        List<Procedure> procedureList = (List<Procedure>) procedureRepository.findAllById(procedureIds);

        //Проверка, что все процедуры найдены
        if (procedureList.size() != procedureIds.size()){
            Set<Integer> foundIds = procedureList.stream()
                    .map(Procedure::getId)
                    .collect(Collectors.toSet());

            List<Integer> missingIds = procedureIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            throw new ResourceNotFoundException("Процедуры с ID: %s не найдены у мастера с ID: %s".formatted(missingIds, getCurrentMaster().getId()));
        }

        return procedureList;
    }

}
