package org.example.untitled.Calculators;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculadoraImpuestos {

    public static final double TASA_INSS_LABORAL = 0.07; // 7%
    public static final double TASA_INSS_PATRONAL = 0.225; // 22.5% (Variable según tamaño, usamos estándar)

    /**
     * Calcula el IR Mensual basado en la proyección anual (Ley 822)
     */
    public static BigDecimal calcularIR(BigDecimal salarioMensualBruto) {
        // 1. Calcular INSS
        BigDecimal inss = salarioMensualBruto.multiply(new BigDecimal(TASA_INSS_LABORAL));

        // 2. Salario Neto Mensual (Base Imponible)
        BigDecimal salarioNeto = salarioMensualBruto.subtract(inss);

        // 3. Proyección Anual
        BigDecimal rentaNetaAnual = salarioNeto.multiply(new BigDecimal(12));

        // 4. Aplicar Tabla Progresiva Anual (Valores oficiales NI)
        BigDecimal impuestoBase = BigDecimal.ZERO;
        BigDecimal porcentajeAplicable = BigDecimal.ZERO;
        BigDecimal sobreExceso = BigDecimal.ZERO;

        double renta = rentaNetaAnual.doubleValue();

        if (renta <= 100000) {
            return BigDecimal.ZERO; // Exento
        } else if (renta <= 200000) {
            sobreExceso = new BigDecimal(100000);
            porcentajeAplicable = new BigDecimal(0.15);
            impuestoBase = BigDecimal.ZERO;
        } else if (renta <= 350000) {
            sobreExceso = new BigDecimal(200000);
            porcentajeAplicable = new BigDecimal(0.20);
            impuestoBase = new BigDecimal(15000);
        } else if (renta <= 500000) {
            sobreExceso = new BigDecimal(350000);
            porcentajeAplicable = new BigDecimal(0.25);
            impuestoBase = new BigDecimal(45000);
        } else {
            sobreExceso = new BigDecimal(500000);
            porcentajeAplicable = new BigDecimal(0.30);
            impuestoBase = new BigDecimal(82500);
        }

        // 5. Cálculo del IR Anual
        BigDecimal irAnual = rentaNetaAnual.subtract(sobreExceso)
                .multiply(porcentajeAplicable)
                .add(impuestoBase);

        // 6. Retornar IR Mensual
        return irAnual.divide(new BigDecimal(12), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calcularINSS(BigDecimal salarioBruto) {
        return salarioBruto.multiply(new BigDecimal(TASA_INSS_LABORAL))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
