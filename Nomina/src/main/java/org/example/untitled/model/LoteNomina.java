package org.example.untitled.model;

import org.openxava.annotations.Required;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDate;

@Entity
public class LoteNomina extends BaseEntity {

    @Required
    private String descripcion;

    @Required
    private LocalDate fechaInicio;

    @Required
    private LocalDate fechaFin;

    @Required
    @Enumerated(EnumType.STRING)
    private EstadoLote estado = EstadoLote.ABIERTO;


}
