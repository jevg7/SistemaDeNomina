package org.example.untitled.model;

import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.*;
import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;

@Entity
@Getter @Setter
@View(members =
        "descripcion;" +
                "fechaInicio, fechaFin;" +
                "estado;" +
                "resultados"
)
public class LoteNomina extends BaseEntity {

    @Required
    @Column(length = 100)
    private String descripcion;

    @Required
    private LocalDate fechaInicio;

    @Required
    private LocalDate fechaFin;

    @Required
    @Enumerated(EnumType.STRING)
    private EstadoLote estado = EstadoLote.ABIERTO;

    @OneToMany(mappedBy = "loteNomina", cascade = CascadeType.REMOVE)

    @ListProperties("empleado.nombreCompleto, totalDevengado, totalDeducciones, totalPagar")

    @ReadOnly
    private Collection<NominaCalculada> resultados =new ArrayList<>();
}