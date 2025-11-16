package org.example.untitled.model;

import org.openxava.annotations.ListAction;
import org.openxava.annotations.Required;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class EstructuraSalarial extends BaseEntity {

    @Required
    @Column(length = 30)
    private String nombre;

    @OneToMany(mappedBy = "estructura", cascade = CascadeType.ALL)
    @ListAction("GenerarReglasEstructura")
    private List<ReglaSalarial> reglas;


}
