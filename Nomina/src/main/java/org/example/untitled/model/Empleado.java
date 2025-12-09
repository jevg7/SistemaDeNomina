package org.example.untitled.model;

import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.DescriptionsList;
import org.openxava.annotations.Required;
import javax.persistence.*;
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
    private Date fechaIngreso;

    @ManyToOne(fetch = FetchType.LAZY)
    @DescriptionsList(descriptionProperties="nombre")
    private Cargo cargo;

    @Column(length = 50)
    private String banco;

    @Column(length = 30)
    private String numeroCuenta;

    // Se elimino el boton de Estado

    @Override
    public String toString() {
        return nombreCompleto;
    }
}