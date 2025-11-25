package org.example.untitled.model;

import lombok.*;
import org.openxava.annotations.*;
import javax.persistence.*;

@Entity
@Getter
@Setter
public class CuentaBancaria extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @Required
    @DescriptionsList
    private Empleado empleado;

    @Required
    @Column(length = 50)
    private String banco;

    @Required
    @Column(length = 30)
    private String numeroCuenta;

    @Required
    @Enumerated(EnumType.STRING)
    private TipoCuentaBancaria tipo;

    private Boolean esCuentaPrincipal = true;
}
