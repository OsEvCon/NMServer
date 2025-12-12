package model;
import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.Objects;
@Getter // Геттеры для всех полей
@Setter // Сеттеры для всех не-final полей
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // equals/hashCode ТОЛЬКО для включенных полей
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

    @ManyToMany(mappedBy = "clients", fetch = FetchType.EAGER)
    @JsonIgnore
    private List<Master> masters;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "client")
    private List<Visit> visits;
}
