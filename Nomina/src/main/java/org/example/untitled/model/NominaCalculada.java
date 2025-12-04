package org.example.untitled.model;

import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.Money;
import org.openxava.annotations.ReadOnly;
import org.openxava.annotations.Required;

import javax.persistence.*;

@Entity
@Getter @Setter
public class NominaCalculada extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @Required
    private LoteNomina loteNomina;

    // Esto es necesario para saber de quién es esta colilla de pago
    @ManyToOne(fetch = FetchType.LAZY)
    @Required
    private Empleado empleado;

    @Money
    @ReadOnly
    private Double totalDevengado;

    @Money
    @ReadOnly
    private Double totalDeducciones;

    @Money
    @ReadOnly
    private Double totalPagar;
}
