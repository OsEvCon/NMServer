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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id")
    private Client client;

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

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}
