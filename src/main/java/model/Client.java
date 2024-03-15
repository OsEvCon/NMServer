package model;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

@Entity
@Table(name = "client")
public class Client extends User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "telegram_id")
    private String telegram_id;

    @Column(name = "chat_id")
    private String chat_id;

    @ManyToMany(mappedBy = "clients", fetch = FetchType.EAGER)
    private List<Master> masters;

    @OneToMany(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT) // Добавляем аннотацию @Fetch с указанием стратегии загрузки
    @JoinColumn(name = "client_id")
    List<Visit> visits;

    public ArrayList<Visit> nextVisits(){
        ArrayList<Visit> nextVisits = new ArrayList<>();
        GregorianCalendar currentDate = (GregorianCalendar) GregorianCalendar.getInstance();
        for (Visit visit: visits){
            if (visit.getVisitDate().after(currentDate)){
                nextVisits.add(visit);
            }
        }
        return nextVisits;
    }

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

    public String getTelegram_id() {
        return telegram_id;
    }

    public void setTelegram_id(String telegram_id) {
        this.telegram_id = telegram_id;
    }

    public String getChat_id() {
        return chat_id;
    }

    public void setChat_id(String chat_id) {
        this.chat_id = chat_id;
    }

    public List<Master> getMasters() {
        return masters;
    }

    public void setMasters(List<Master> masters) {
        this.masters = masters;
    }

    public List<Visit> getVisits() {
        return visits;
    }

    public void setVisits(List<Visit> visits) {
        this.visits = visits;
    }
}
