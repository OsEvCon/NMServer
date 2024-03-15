package model;

import jakarta.persistence.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;

@Entity
@Table(name = "visit")
public class Visit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "date")
    private GregorianCalendar visitDate;

    @Column(name = "schedule_id")
    private Integer scheduleId;

    @Column(name = "client_id")
    Integer clientId;

    @Column(name = "master_id")
    Integer masterId;

    public Integer getMasterId() {
        return masterId;
    }

    public void setMasterId(Integer masterId) {
        this.masterId = masterId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public GregorianCalendar getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(GregorianCalendar visitDate) {
        this.visitDate = visitDate;
    }

    public Integer getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Integer scheduleId) {
        this.scheduleId = scheduleId;
    }

    public void setVisitDate(String year, String month, String day){
        int iYear = Integer.parseInt(year);
        int iMonth = Integer.parseInt(month) - 1;
        int iDay = Integer.parseInt(day);
        GregorianCalendar visitDate = new GregorianCalendar(iYear, iMonth, iDay);
        setVisitDate(visitDate);
    }

    public void setVisitTime(String hour, String minute){
        int iHour = Integer.parseInt(hour);
        int iMinute = Integer.parseInt(minute);
        getVisitDate().set(Calendar.HOUR_OF_DAY, iHour);
        getVisitDate().set(Calendar.MINUTE, iMinute);
    }

    /*public String getVisitDateAndTime(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        return simpleDateFormat.format(visitDate.getTime());
    }*/

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    /*@Override
    public String toString() {
        return getVisitDateAndTime();
    }*/

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Visit visit = (Visit) o;
        return id.equals(visit.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
