package model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.*;

@Entity
@Table(name = "schedule")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "master_id")
    private Integer masterId;

    @Column(name = "month_and_year")
    private GregorianCalendar monthAndYear;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "schedule_id")
    private List<Visit> visits;

   /* public Map<String, ArrayList<String>> getDateAndTimeMap(){
        Map<String,ArrayList<String>> result = new LinkedHashMap<>();
        for (Visit visit :visits) {
            String date = visit.getVisitDateAndTime().split(" ")[0];
            String time = visit.getVisitDateAndTime().split(" ")[1];
            if (!result.containsKey(date)){
                result.put(date, new ArrayList<>());
            }
                result.get(date).add(time);
        }
        return result;
    }*/

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMasterId() {
        return masterId;
    }

    public void setMasterId(Integer masterId) {
        this.masterId = masterId;
    }

    public List<Visit> getVisits() {
        return visits;
    }

    public void setVisits(List<Visit> visits) {
        this.visits = visits;
    }

    public GregorianCalendar getMonthAndYear() {
        return monthAndYear;
    }

    public void setMonthAndYear(GregorianCalendar monthAndYear) {
        this.monthAndYear = monthAndYear;
    }
    @JsonIgnore
    public String getMonthAndYearString(){
        return monthAndYear.get(Calendar.MONTH) + 1 + "." + monthAndYear.get(Calendar.YEAR);
    }
}
