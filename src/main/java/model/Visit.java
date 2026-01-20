package model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter // Геттеры для всех полей
@Setter // Сеттеры для всех не-final полей
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // equals/hashCode ТОЛЬКО для включенных полей
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "visit")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Visit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime visitDateTime;

    @JsonBackReference("client-visits")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @JsonBackReference("master-visits")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id")
    private Master master;

    @ManyToMany
    @JoinTable(
            name = "visit_procedure",
            joinColumns = @JoinColumn(name = "visit_id"),
            inverseJoinColumns = @JoinColumn(name = "procedure_id")
    )
    @Builder.Default
    private List<Procedure> procedures = new ArrayList<>();

    public void addProcedure(Procedure procedure) {
        procedures.add(procedure);
    }

    public void setVisitDateTime(String visitDateTime) {
        this.visitDateTime = LocalDateTime.parse(visitDateTime);
    }

    public void setVisitDateTime(LocalDateTime visitDateTime) {
        this.visitDateTime = visitDateTime;
    }

}
