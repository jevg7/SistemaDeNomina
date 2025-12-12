package org.example.untitled.actions;

import org.example.untitled.model.*;
import org.example.untitled.Calculators.CalculadoraImpuestos;
import org.openxava.actions.ViewBaseAction;
import org.openxava.jpa.XPersistence;
import org.openxava.util.Is;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProcesarNominaAction extends ViewBaseAction {

    @Override
    public void execute() throws Exception {

        Map key = getView().getKeyValues();
        String loteId = (String) key.get("id");

        if (Is.empty(loteId)) {
            addError("Primero debes guardar el Lote de Nómina (dale al botón Save antes de calcular).");
            return;
        }

        EntityManager em = XPersistence.getManager();
        LoteNomina lote = em.find(LoteNomina.class, loteId);

        if (lote == null) {
            addError("No se encontró el lote.");
            return;
        }

        // Validación de estado (Descomentar si quieres bloquear recálculos en lotes cerrados)
        /*
        if (lote.getEstado() == EstadoLote.PAGADO || lote.getEstado() == EstadoLote.CERRADO) {
            addError("Este lote ya está cerrado o pagado y no se puede recalcular.");
            return;
        }
        */

        // 0. LIMPIEZA PREVIA: Borrar cálculos anteriores para evitar duplicados
        String deleteLineas = "DELETE FROM LineaNomina l WHERE l.nominaCalculada.id IN " +
                "(SELECT n.id FROM NominaCalculada n WHERE n.loteNomina.id = :loteId)";
        em.createQuery(deleteLineas).setParameter("loteId", loteId).executeUpdate();

        String deleteNominas = "DELETE FROM NominaCalculada n WHERE n.loteNomina.id = :loteId";
        em.createQuery(deleteNominas).setParameter("loteId", loteId).executeUpdate();

        em.flush();

        // 1. Buscamos empleados activos
        String jpql = "FROM Empleado e WHERE e.estado = true";
        TypedQuery<Empleado> query = em.createQuery(jpql, Empleado.class);
        List<Empleado> empleados = query.getResultList();

        int procesados = 0;

        for (Empleado emp : empleados) {

            // Buscar contrato vigente
            String contratoJpql = "FROM Contrato c WHERE c.empleado.id = :empId";
            List<Contrato> contratos = em.createQuery(contratoJpql, Contrato.class)
                    .setParameter("empId", emp.getId())
                    .getResultList();

            if (contratos.isEmpty()) continue; // Sin contrato no hay pago

            Contrato contrato = contratos.get(0);

            // =========================================================================
            // 1. CÁLCULO DE DEVENGACIONES (INGRESOS)
            // =========================================================================
            BigDecimal salarioBase = contrato.getSalarioMensual();
            if (salarioBase == null) salarioBase = BigDecimal.ZERO;

            BigDecimal totalIngresosImponible = salarioBase;

            // Lista para guardar los extras encontrados y luego imprimirlos en el detalle
            List<IngresoAdicional> listaIngresosExtras = new ArrayList<>();

            try {
                // Buscamos ingresos adicionales activos (Bonos, Comisiones, etc.)
                String ingresosJpql = "FROM IngresoAdicional i WHERE i.empleado.id = :empId AND i.activo = true";
                listaIngresosExtras = em.createQuery(ingresosJpql, IngresoAdicional.class)
                        .setParameter("empId", emp.getId())
                        .getResultList();

                for (IngresoAdicional extra : listaIngresosExtras) {
                    if (extra.getMonto() != null) {
                        // SUMA: Devengación extra se suma al total imponible
                        totalIngresosImponible = totalIngresosImponible.add(extra.getMonto());
                    }
                }
            } catch (Exception e) {
                // Si la tabla IngresoAdicional no existe aun, ignoramos el error y seguimos solo con salario base
                System.out.println("Aviso: No se pudieron cargar ingresos adicionales (Tabla no existe o vacía).");
            }

            // =========================================================================
            // 2. CÁLCULO DE DEDUCIBLES DE LEY (INSS + RENTA)
            // =========================================================================
            // IMPORTANTE: Calculamos los impuestos sobre el TOTAL devengado (Salario + Bonos)
            BigDecimal inss = CalculadoraImpuestos.calcularINSS(totalIngresosImponible);
            if (inss == null) inss = BigDecimal.ZERO;

            BigDecimal ir = CalculadoraImpuestos.calcularIR(totalIngresosImponible);
            if (ir == null) ir = BigDecimal.ZERO;

            // =========================================================================
            // 3. DEDUCCIONES VOLUNTARIAS (Préstamos, Adelantos)
            // =========================================================================
            BigDecimal totalDeduccionesVoluntarias = BigDecimal.ZERO;

            String dedJpql = "FROM DeduccionVoluntaria d WHERE d.empleado.id = :empId AND d.activo = true";
            List<DeduccionVoluntaria> deducciones = em.createQuery(dedJpql, DeduccionVoluntaria.class)
                    .setParameter("empId", emp.getId())
                    .getResultList();

            for (DeduccionVoluntaria ded : deducciones) {
                // CORRECCIÓN: Usamos getMontoTotal() porque así se llama en tu entidad
                if (ded.getMontoTotal() != null) {
                    BigDecimal montoDed = BigDecimal.valueOf(ded.getMontoTotal());
                    totalDeduccionesVoluntarias = totalDeduccionesVoluntarias.add(montoDed);
                }
            }

            // =========================================================================
            // 4. CÁLCULO FINAL (NETO A PAGAR)
            // =========================================================================
            // Formula: (Total Devengado) - (INSS + IR + Deducciones Voluntarias)
            BigDecimal granTotalDeducciones = inss.add(ir).add(totalDeduccionesVoluntarias);
            BigDecimal totalPagar = totalIngresosImponible.subtract(granTotalDeducciones);

            // =========================================================================
            // 5. GUARDAR EN BASE DE DATOS
            // =========================================================================

            // A. Cabecera (Resumen de Totales)
            NominaCalculada nomina = new NominaCalculada();
            nomina.setLoteNomina(lote);
            nomina.setEmpleado(emp);
            nomina.setTotalDevengado(totalIngresosImponible.doubleValue());
            nomina.setTotalDeducciones(granTotalDeducciones.doubleValue());
            nomina.setTotalPagar(totalPagar.doubleValue());

            em.persist(nomina);

            // B. Detalles (Líneas visibles en la tabla "Detalles")

            // 1. Ingresos
            crearLinea(em, nomina, "Salario Base", salarioBase);

            for (IngresoAdicional extra : listaIngresosExtras) {
                if (extra.getMonto() != null) {
                    crearLinea(em, nomina, extra.getConcepto(), extra.getMonto());
                }
            }

            // 2. Deducciones de Ley
            crearLinea(em, nomina, "INSS Laboral (7%)", inss);
            crearLinea(em, nomina, "IR (Impuesto Renta)", ir);

            // 3. Deducciones Voluntarias
            for (DeduccionVoluntaria ded : deducciones) {
                if (ded.getMontoTotal() != null) {
                    // CORRECCIÓN: Usamos getConcepto() y getMontoTotal()
                    crearLinea(em, nomina, ded.getConcepto(), BigDecimal.valueOf(ded.getMontoTotal()));
                }
            }

            procesados++;
        }

        // Actualizar estado del lote
        lote.setEstado(EstadoLote.CERRADO);
        em.merge(lote);

        em.flush();
        getView().refresh();
        addMessage("Nómina calculada con éxito. Ingresos y deducciones procesados para " + procesados + " empleados.");
    }

    private void crearLinea(EntityManager em, NominaCalculada nomina, String descripcion, BigDecimal monto) {
        if (monto != null && monto.compareTo(BigDecimal.ZERO) > 0) {
            LineaNomina linea = new LineaNomina();
            linea.setNominaCalculada(nomina);
            linea.setDescripcion(descripcion);
            linea.setMonto(monto.doubleValue());
            em.persist(linea);
        }
    }
}