package org.example.untitled.model;

import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.DescriptionsList;
import org.openxava.annotations.Money;
import org.openxava.annotations.Required;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Getter @Setter
public class IngresoAdicional extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties="nombreCompleto")
    @Required
    private Empleado empleado;

    @Required
    @Column(length = 50)
    private String concepto; // Ej: "Horas Extras", "Bono de Producción"

    @Money
    @Required
    private BigDecimal monto;

    private Boolean activo = true; // Para que se sume automáticamente mes a mes

    @Override
    public String toString() {
        return concepto;
    }
}