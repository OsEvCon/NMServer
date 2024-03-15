package model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "master")
public class Master extends User {
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

    @ManyToMany(fetch = FetchType.EAGER)
    @JsonIgnoreProperties("masters")
    private List<Client> clients;

    @OneToMany(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT) // Добавляем аннотацию @Fetch с указанием стратегии загрузки
    @JoinColumn(name = "master_id")
    List<Schedule> schedules;

    @OneToMany(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT) // Добавляем аннотацию @Fetch с указанием стратегии загрузки
    @JoinColumn(name = "master_id")
    List<Visit> visits;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
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

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }



    public List<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Schedule> schedules) {
        this.schedules = schedules;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Master master = (Master) o;
        return Objects.equals(id, master.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
