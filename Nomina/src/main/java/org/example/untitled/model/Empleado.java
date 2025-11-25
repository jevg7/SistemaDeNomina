package org.example.untitled.model;


import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.Money;
import org.openxava.annotations.Required;

import javax.persistence.*;
import javax.validation.constraints.Min;
import java.util.Date;

@Entity
@Getter
@Setter
public class Empleado extends BaseEntity {

    @Required
    @Column(length = 14, unique = true)
    private String cedula;

    @Required
    @Column(length = 20, unique = true)
    private String numSeguroSocial;

    @Required
    private String nombreCompleto;

    @Required
    @Temporal(TemporalType.DATE)
    private Date fechIngreso;

    @Required
    private Boolean estado;





}
