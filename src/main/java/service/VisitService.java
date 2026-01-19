package service;

import DTO.VisitDTO;
import exception.VisitDataAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mapper.VisitMapper;
import model.Master;
import model.Visit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class VisitService {
    private final VisitMapper visitMapper;

    public List<VisitDTO> getVisits() {
        log.info("Получение списка визитов для текущего мастера");

        try {
            Master master = getCurrentMaster();
            List<Visit> visits = master.getVisits();
            log.debug("Найдено {} визитов для мастера c ID: {}", visits.size(), master.getId());
            return visitMapper.toDTO(visits);
        } catch (Exception e) {
            log.error("Ошибка получения визитов", e);
            throw new VisitDataAccessException(e);
        }
    }

    private Master getCurrentMaster() {
        return SecurityUtils.getCurrentMaster();
    }
}
