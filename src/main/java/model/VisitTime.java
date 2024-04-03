package model;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "visit_time")
public class VisitTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    @Column
    private LocalTime time;
    @Column
    private int visitDayId;

    @Column(name = "client_id")
    Integer clientId;

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public Integer getId() {
        return id;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public int getVisitDayId() {
        return visitDayId;
    }

    public void setVisitDayId(int visitDayId) {
        this.visitDayId = visitDayId;
    }
}
