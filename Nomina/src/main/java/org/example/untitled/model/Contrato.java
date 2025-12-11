package org.example.untitled.model;


import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.DescriptionsList;
import org.openxava.annotations.Money;
import org.openxava.annotations.Required;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Entity
@Getter
@Setter
public class Contrato extends BaseEntity {

    @ManyToOne
    @DescriptionsList(descriptionProperties = "nombreCompleto")
    private Empleado empleado;

    @Required
    @Money
    private BigDecimal salarioBase;

    @Required
    private LocalDate fechaContrato;

    private Date fechaFinContrato;

    @Required
    @Enumerated(EnumType.STRING)
    private TipoContrato tipoContrato;

    public BigDecimal getSalarioMensual() {
        return this.salarioBase;
    }
    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList
    private EstructuraSalarial estructura;


}
