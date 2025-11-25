package org.example.untitled.model;

public enum EstadoLote {
    ABIERTO,        // En proceso de carga de datos
    CALCULADO,      // Nómina procesada pero no confirmada
    COMPROMETIDO,   // Aprobado presupuestariamente (Sector Público) [cite: 176]
    DEVENGADO,      // Obligación de pago reconocida [cite: 177]
    PAGADO,         // Transferencia realizada y cerrada [cite: 179]
    ANULADO
}
