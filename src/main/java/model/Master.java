package model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "master")
public class Master {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @JsonProperty("id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @EqualsAndHashCode.Include
    @Column(name = "email")
    private String email;

    @JsonIgnore
    @Column
    private String password;

    @JsonIgnore
    @Column
    private String secretKey;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "master_client",
            joinColumns = @JoinColumn(name = "master_id"),
            inverseJoinColumns = @JoinColumn(name = "client_id")
    )
    @JsonIgnoreProperties("masters")
    private Set<Client> clients = new HashSet<>();

    @JsonManagedReference("master-visits")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "master")
    List<Visit> visits = new ArrayList<>();

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "master_procedure",
            joinColumns = @JoinColumn(name = "master_id"),
            inverseJoinColumns = @JoinColumn(name = "procedure_id")
    )
    private List<Procedure> procedures = new ArrayList<>();

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

    public void addClient(Client client) {
        clients.add(client);
        client.getMasters().add(this);
    }

    public void removeClient(Client client) {
        clients.remove(client);
        client.getMasters().remove(this);
    }

    public void addVisit(Visit visit) {
        visits.add(visit);
        visit.setMaster(this); // просто set!
    }

    public void removeVisit(Visit visit) {
        visits.remove(visit);
        visit.setMaster(null);
    }


}
