package org.example.untitled.model;

import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.Money;
import org.openxava.annotations.Required;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
@Getter
@Setter
public class NominaCalculada extends BaseEntity{

    @ManyToOne @Required
    private LoteNomina loteNomina;

    @Money
    private Double totalDevengado;

    @Money
    private Double totalDeducciones;

    @Money
    private Double totalPagar;
}
