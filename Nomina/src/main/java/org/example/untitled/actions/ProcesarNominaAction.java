package org.example.untitled.actions;

import org.example.untitled.model.*;
import org.example.untitled.Calculators.CalculadoraImpuestos;
import org.openxava.actions.ViewBaseAction;
import org.openxava.jpa.XPersistence;
import org.openxava.util.Is;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ProcesarNominaAction extends ViewBaseAction {

    @Override
    public void execute() throws Exception {

        Map key = getView().getKeyValues();
        String loteId = (String) key.get("id");

        if (Is.empty(loteId)) {
            addError("Primero debes guardar el Lote de Nómina.");
            return;
        }

        EntityManager em = XPersistence.getManager();
        LoteNomina lote = em.find(LoteNomina.class, loteId);

        if (lote.getEstado() == EstadoLote.PAGADO || lote.getEstado() == EstadoLote.CERRADO) {
            addError("Este lote ya está cerrado o pagado.");
            return;
        }

        // Buscamos empleados activos
        String jpql = "FROM Empleado e WHERE e.estado = true";
        TypedQuery<Empleado> query = em.createQuery(jpql, Empleado.class);
        List<Empleado> empleados = query.getResultList();

        int procesados = 0;


        for (Empleado emp : empleados) {

            // Buscar contrato
            String contratoJpql = "FROM Contrato c WHERE c.empleado.id = :empId";
            List<Contrato> contratos = em.createQuery(contratoJpql, Contrato.class)
                    .setParameter("empId", emp.getId())
                    .getResultList();

            if (contratos.isEmpty()) continue;
            Contrato contrato = contratos.get(0);

            // --- 1. Cálculos de Ingresos y Ley ---
            BigDecimal salarioBase = contrato.getSalarioMensual();
            if (salarioBase == null) salarioBase = BigDecimal.ZERO;

            BigDecimal inss = CalculadoraImpuestos.calcularINSS(salarioBase);
            BigDecimal ir = CalculadoraImpuestos.calcularIR(salarioBase);

            // --- 2. NUEVO: Buscar Deducciones Voluntarias ---
            BigDecimal totalDeduccionesVoluntarias = BigDecimal.ZERO;

            // Nota: Usamos 'empId' para mantener consistencia con tu código de contratos
            String dedJpql = "FROM DeduccionVoluntaria d WHERE d.empleado.id = :empId AND d.activo = true";
            List<DeduccionVoluntaria> deducciones = em.createQuery(dedJpql, DeduccionVoluntaria.class)
                    .setParameter("empId", emp.getId())
                    .getResultList();

            // --- 3. Calcular Totales Finales ---
            // Sumamos: INSS + IR + VOLUNTARIAS
            BigDecimal granTotalDeducciones = inss.add(ir);

            // Ciclo preliminar para sumar las deducciones voluntarias al total antes de guardar
            for (DeduccionVoluntaria ded : deducciones) {
                Double cuotaVal = ded.getCuotaMensual();
                if (cuotaVal == null) cuotaVal = 0.0;
                granTotalDeducciones = granTotalDeducciones.add(BigDecimal.valueOf(cuotaVal));
            }

            BigDecimal totalPagar = salarioBase.subtract(granTotalDeducciones);

            // --- 4. Guardar Cabecera (NominaCalculada) ---
            NominaCalculada nomina = new NominaCalculada();
            nomina.setLoteNomina(lote);
            nomina.setEmpleado(emp);
            nomina.setTotalDevengado(salarioBase.doubleValue());
            nomina.setTotalDeducciones(granTotalDeducciones.doubleValue()); // Total actualizado
            nomina.setTotalPagar(totalPagar.doubleValue());

            em.persist(nomina);

            // --- 5. Guardar Líneas (Detalle) ---

            // Líneas de Ley
            crearLinea(em, nomina, "Salario Base", salarioBase, TipoRegla.INGRESO);
            crearLinea(em, nomina, "INSS Laboral", inss, TipoRegla.DEDUCCION);
            crearLinea(em, nomina, "IR (Impuesto Renta)", ir, TipoRegla.DEDUCCION);

            // Líneas Voluntarias
            for (DeduccionVoluntaria ded : deducciones) {
                Double cuotaVal = ded.getCuotaMensual();
                if (cuotaVal == null) cuotaVal = 0.0;
                BigDecimal monto = BigDecimal.valueOf(cuotaVal);

                crearLinea(em, nomina, ded.getConcepto(), monto, TipoRegla.DEDUCCION);
            }

            procesados++;
        }

        lote.setEstado(EstadoLote.CALCULADO);
        em.merge(lote);
        em.flush();
        getView().refresh();
        addMessage("Nómina procesada exitosamente para " + procesados + " empleados.");
    }


    private void crearLinea(EntityManager em, NominaCalculada nomina, String descripcion, BigDecimal monto, TipoRegla tipo) {
        // Validación para no guardar líneas en 0
        if (monto.compareTo(BigDecimal.ZERO) > 0) {
            LineaNomina linea = new LineaNomina();
            linea.setNominaCalculada(nomina);
            linea.setDescripcion(descripcion);
            linea.setMonto(monto.doubleValue());
            // Nota: Aquí recibes 'tipo' pero no lo estás guardando en LineaNomina en tu código original.
            // Si LineaNomina tiene un campo 'tipo', deberías agregar: linea.setTipo(tipo);
            em.persist(linea);
        }
    }
}
