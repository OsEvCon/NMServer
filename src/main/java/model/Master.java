package model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@Table(name = "master")
public class Master extends User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @JsonProperty("id")
    private Integer id;
    @Column(name = "name")
    private String name;
    @Column(name = "telegram_id")
    private String telegram_id;
    @Column(name = "chat_id")
    private String chat_id;
    @JsonIgnore
    @Column(name = "email")
    private String email;

    @JsonIgnore
    @Column
    private String password;

    @JsonIgnore
    @Column
    private String secretKey;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "master_client",
            joinColumns = @JoinColumn(name = "master_id"),
            inverseJoinColumns = @JoinColumn(name = "client_id")
    )
    @JsonIgnoreProperties("masters")
    private List<Client> clients;
    @JsonManagedReference("master-visits")
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "master")
    @Fetch(FetchMode.SUBSELECT) // Добавляем аннотацию @Fetch с указанием стратегии загрузки
    List<Visit> visits;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "master_procedure",
            joinColumns = @JoinColumn(name = "master_id"),
            inverseJoinColumns = @JoinColumn(name = "procedure_id")
    )
    private List<Procedure> procedures;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "master-roles",
            joinColumns = @JoinColumn(name = "master_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles = new ArrayList<>();

    public Collection<? extends GrantedAuthority> getAuthorities(){
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }
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

    public List<Visit> getVisits() {
        return visits;
    }

    public void setVisits(List<Visit> visits) {
        this.visits = visits;
    }

    public List<Procedure> getProcedures() {
        return procedures;
    }

    public void setProcedures(List<Procedure> procedures) {
        this.procedures = procedures;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
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
