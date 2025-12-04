package org.example.untitled.model;


import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.Money;
import org.openxava.annotations.Required;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Entity
@Getter
@Setter
public class Contrato extends BaseEntity {

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
    @ManyToOne
    private EstructuraSalarial estructura;
    @ManyToOne
    private Empleado empleado;

}
