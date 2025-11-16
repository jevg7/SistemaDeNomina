package org.example.untitled.model;

import jdk.vm.ci.meta.Local;
import org.openxava.annotations.Money;
import org.openxava.annotations.Required;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import java.time.LocalDate;
import java.util.Date;

@Entity
public class Contrato extends BaseEntity {

    @Required
    @Money
    private Double salarioBase;

    @Required
    private LocalDate fechaContrato;

    private Date fechaFinContrato;

    @Required
    @Enumerated(EnumType.STRING)
    private TipoContrato tipoContrato;

    @ManyToOne
    private EstructuraSalarial estructura;

}
