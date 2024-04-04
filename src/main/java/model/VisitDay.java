package model;

import jakarta.persistence.*;


import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "visit_day")
public class VisitDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column
    private LocalDate date;

    @Column
    private Integer scheduleId;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "visitDayId")
    private List<VisitTime> visitTimes;

    public Integer getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Integer scheduleId) {
        this.scheduleId = scheduleId;
    }

    public List<VisitTime> getVisitTimes() {
        return visitTimes;
    }

    public void setVisitTimes(List<VisitTime> visitTimes) {
        this.visitTimes = visitTimes;
    }
}
