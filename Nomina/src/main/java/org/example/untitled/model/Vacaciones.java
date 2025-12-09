package org.example.untitled.model;

import lombok.*;
import org.openxava.annotations.*;
import javax.persistence.*;
import javax.validation.constraints.Min; // Importante
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
    @Min(value = 1, message = "Debes tomar al menos 1 día") // NUEVO
    private Integer diasTomados;

    @Stereotype("MEMO")
    private String observaciones;

    private Boolean procesado = false;
}