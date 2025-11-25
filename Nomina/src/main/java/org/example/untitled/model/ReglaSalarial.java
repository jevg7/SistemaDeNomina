package org.example.untitled.model;


import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.Required;
import org.openxava.annotations.Stereotype;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;

@Entity
@Getter
@Setter
public class ReglaSalarial extends BaseEntity {

    @Required
    @Column(length = 30)
    private String  codigo;

    @Required
    @Column(length = 200)
    private String descripcion;

    @Required
    @Column(length = 300)
    @Stereotype("MEMO") //Texto Plano Multilínea Crea una caja de texto grande (un textarea de HTML).
    private String formula;

    @Required
    @Enumerated(EnumType.STRING)
    private TipoRegla tipo;
}
