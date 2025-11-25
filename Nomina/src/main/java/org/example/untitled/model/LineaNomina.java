package org.example.untitled.model;

import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.Money;
import org.openxava.annotations.Required;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
@Getter
@Setter
public class LineaNomina extends BaseEntity {

    @ManyToOne @Required
    private NominaCalculada nominaCalculada;

    @ManyToOne @Required
    private ReglaSalarial reglaSalarial;

    @Money
    private Double monto;
}
