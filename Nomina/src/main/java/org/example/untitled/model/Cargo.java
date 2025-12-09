package org.example.untitled.model;
import lombok.Getter;
import lombok.Setter;
import org.openxava.annotations.*;
import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter

public class Cargo extends BaseEntity {

    @Required
    @Column(length = 50)
    private String nombre;

    @Stereotype("MEMO")
    private String descripcion;

    @Money
    private BigDecimal salarioMinimoReferencia;
    @Money
    private BigDecimal salarioMaximoReferencia;

    @Override
    public String toString() {
        return nombre;
    }
}
