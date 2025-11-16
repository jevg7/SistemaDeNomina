package org.example.untitled.model;

import org.openxava.annotations.Required;

import javax.persistence.Column;
import javax.persistence.OneToMany;

public class EstructuraSalarial extends BaseEntity {

    @Required
    @Column(length = 30)
    private String nombre;

    @OneToMany(mappedBy = "estructura", cascade = CascadeType.ALL)
    private List<ReglaSalarial> reglas;


}
