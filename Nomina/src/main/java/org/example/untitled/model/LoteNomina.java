package org.example.untitled.model;

import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.*;
import javax.persistence.*;
import javax.validation.constraints.AssertTrue; // Importante
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
    private Collection<NominaCalculada> resultados = new ArrayList<>();

    // VALIDACIÓN LÓGICA
    @AssertTrue(message = "La fecha fin debe ser posterior a la fecha de inicio")
    private boolean isFechasValidas() {
        if (fechaInicio == null || fechaFin == null) return true;
        return !fechaFin.isBefore(fechaInicio); // Fin no puede ser antes de Inicio
    }
}