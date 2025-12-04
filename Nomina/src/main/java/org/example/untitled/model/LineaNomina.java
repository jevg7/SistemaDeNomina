package org.example.untitled.model;

import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.Money;
import org.openxava.annotations.Required;

import javax.persistence.*;

@Entity
@Getter @Setter
public class LineaNomina extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @Required
    private NominaCalculada nominaCalculada;

    @ManyToOne(fetch = FetchType.LAZY)

    private ReglaSalarial reglaSalarial;


    // Aquí guardaremos textos como "Salario Base", "INSS", etc.
    @Column(length = 100)
    private String descripcion;

    @Money
    private Double monto;
}
