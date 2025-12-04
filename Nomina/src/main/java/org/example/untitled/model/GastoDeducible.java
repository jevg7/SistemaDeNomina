package org.example.untitled.model;

import lombok.*;
import org.openxava.annotations.*;
import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class GastoDeducible extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @Required
    @DescriptionsList
    private Empleado empleado;

    @Required
    private LocalDate fechaFactura;

    @Required
    @Column(length = 100)
    private String proveedor;

    @Required
    @Column(length = 50)
    private String numeroFactura;

    @Required
    @Stereotype("MONEY")
    private Double monto;

    @Required
    @Enumerated(EnumType.STRING)
    private TipoGasto tipo;

    @Stereotype("FILE") // Longitud recomendada para guardar el ID del archivo
    private String soporteDigital; // Aquí se sube la factura escaneada

}
