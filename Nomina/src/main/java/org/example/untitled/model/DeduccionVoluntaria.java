package org.example.untitled.model;

import lombok.*;
import org.openxava.annotations.*;
import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class DeduccionVoluntaria extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @Required
    @DescriptionsList(descriptionProperties="nombreCompleto")
    private Empleado empleado;

    @Required
    @Column(length = 50)
    private String concepto;

    @Required
    @Stereotype("MONEY")
    private Double montoTotal;

    @Stereotype("MONEY")
    private Double cuotaMensual;

    @Required
    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    private Boolean activo = true;
}
