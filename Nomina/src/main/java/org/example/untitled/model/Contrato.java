package org.example.untitled.model;

import org.openxava.annotations.Money;
import org.openxava.annotations.Required;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import java.util.Date;

@Entity
public class Contrato extends BaseEntity {

    @Required
    @Money
    private Double salarioBase;

    @Required
    private Date fechaContrato;

    private Date fechaFinContrato;

    @Required
    @Enumerated(EnumType.STRING)
    private TipoContrato tipoContrato;

    @ManyToOne
    private EstructuraSalarial estructura;

}
