package DAO;

import model.Procedure;
import model.Visit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VisitDao {
    private Integer visitId;
    private Integer clientId;
    private List<Integer> proceduresId = new ArrayList<>();
    private LocalDateTime localDateTime;

    public VisitDao(Visit visit){
        this.visitId = visit.getId();
        this.clientId = visit.getClient().getId();
        addProceduresId(visit.getProcedures());
        this.localDateTime = visit.getVisitDateTime();
    }

    public VisitDao(){}

    public int getVisitId() {
        return visitId;
    }

    public void setVisitId(int visitId) {
        this.visitId = visitId;
    }

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public List<Integer> getProceduresId() {
        return proceduresId;
    }

    public void setProceduresId(List<Integer> proceduresId) {
        this.proceduresId = proceduresId;
    }

    public void addProceduresId(List<Procedure> procedures){
        proceduresId.addAll(procedures.stream().map(procedure -> procedure.getId())
                .collect(Collectors.toList()));
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

}
