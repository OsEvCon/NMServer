package model;
import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Getter // Геттеры для всех полей
@Setter // Сеттеры для всех не-final полей
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // equals/hashCode ТОЛЬКО для включенных полей
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "client")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")

public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @JsonProperty("id")
    @EqualsAndHashCode.Include
    private Integer id;

    @EqualsAndHashCode.Include
    @Column(name = "name")
    private String name;

    @Column(name = "phone_number")
    String phoneNumber;

    @Column(name = "email")
    private String email;

    @ManyToMany(mappedBy = "clients", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Master> masters = new HashSet<>();

    @JsonManagedReference("client-visits")
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "client")
    private List<Visit> visits = new ArrayList<>();

    public void addMasters(Master... mastersToAdd) {
        Collections.addAll(this.masters, mastersToAdd);
    }

    public void addVisit(Visit visitToAdd) {
        visits.add(visitToAdd);
        visitToAdd.setClient(this);
    }

    public void removeVisit(Visit visitToRemove) {
        visits.remove(visitToRemove);
        visitToRemove.setClient(null);
    }
}
