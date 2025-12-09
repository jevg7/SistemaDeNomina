package org.example.untitled.model;

import lombok.*;
import org.openxava.annotations.*;
import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Vacaciones extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @Required
    @DescriptionsList(descriptionProperties="nombreCompleto")
    private Empleado empleado;

    @Required
    private LocalDate fechaInicio;

    @Required
    private LocalDate fechaFin;

    @Required
    private Integer diasTomados;

    @Stereotype("MEMO")
    private String observaciones;

    private Boolean procesado = false;
}
