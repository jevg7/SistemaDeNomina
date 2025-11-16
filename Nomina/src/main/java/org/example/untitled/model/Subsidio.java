package org.example.untitled.model;

import org.dom4j.tree.AbstractEntity;
import org.openxava.annotations.Required;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import java.time.LocalDate;

@Entity
public class Subsidio extends BaseEntity {

    @ManyToOne @Required
    private Empleado empleado;

    @Required
    @Enumerated(EnumType.STRING)
    private TipoSubsidio tipo;

    @Required
    private LocalDate fechaInicio;

    @Required
    private LocalDate fechaFin;
}
