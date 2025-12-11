package org.example.untitled.actions;

import org.example.untitled.model.*;
import org.example.untitled.Calculators.CalculadoraImpuestos; // Asegúrate que este import sea correcto
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
            addError("Primero debes guardar el Lote de Nómina (dale al botón Save antes de calcular).");
            return;
        }

        EntityManager em = XPersistence.getManager();
        LoteNomina lote = em.find(LoteNomina.class, loteId);

        if (lote == null) {
            addError("No se encontró el lote.");
            return;
        }

        if (lote.getEstado() == EstadoLote.PAGADO || lote.getEstado() == EstadoLote.CERRADO) {
            addError("Este lote ya está cerrado o pagado y no se puede recalcular.");
            return;
        }


        // 0. LIMPIEZA PREVIA: Borrar cálculos anteriores de este lote para evitar duplicados


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

            if (contratos.isEmpty()) continue; // Si no tiene contrato, saltamos al siguiente empleado

            Contrato contrato = contratos.get(0);

            // --- Cálculos ---
            BigDecimal salarioBase = contrato.getSalarioMensual();
            if (salarioBase == null) salarioBase = BigDecimal.ZERO;

            BigDecimal inss = CalculadoraImpuestos.calcularINSS(salarioBase);
            if (inss == null) inss = BigDecimal.ZERO;

            BigDecimal ir = CalculadoraImpuestos.calcularIR(salarioBase);
            if (ir == null) ir = BigDecimal.ZERO;

            // --- Deducciones Voluntarias ---
            BigDecimal totalDeduccionesVoluntarias = BigDecimal.ZERO;


            String dedJpql = "FROM DeduccionVoluntaria d WHERE d.empleado.id = :empId";

            List deducciones = em.createQuery(dedJpql).setParameter("empId", emp.getId()).getResultList();

            // --- Calcular Totales Finales ---
            BigDecimal granTotalDeducciones = inss.add(ir);

            // Sumar voluntarias (usando casting seguro)
            for (Object obj : deducciones) {

            }

            BigDecimal totalPagar = salarioBase.subtract(granTotalDeducciones);

            // --- Guardar Cabecera (NominaCalculada) ---
            NominaCalculada nomina = new NominaCalculada();
            nomina.setLoteNomina(lote);
            nomina.setEmpleado(emp);
            nomina.setTotalDevengado(salarioBase.doubleValue());
            nomina.setTotalDeducciones(granTotalDeducciones.doubleValue());
            nomina.setTotalPagar(totalPagar.doubleValue());

            em.persist(nomina);

            // --- Guardar Líneas (Detalle) ---
            crearLinea(em, nomina, "Salario Base", salarioBase, TipoRegla.INGRESO); // Corregido el TipoRegla
            crearLinea(em, nomina, "INSS Laboral", inss, TipoRegla.DEDUCCION);
            crearLinea(em, nomina, "IR (Impuesto Renta)", ir, TipoRegla.DEDUCCION);



            procesados++;
        }

        // Actualizar estado del lote
        lote.setEstado(EstadoLote.CERRADO);
        em.merge(lote);


        em.flush();
        getView().refresh();
        addMessage("Nómina recalculada correctamente. Se procesaron " + procesados + " empleados.");
    }


    private void crearLinea(EntityManager em, NominaCalculada nomina, String descripcion, BigDecimal monto, TipoRegla tipo) {
        if (monto != null && monto.compareTo(BigDecimal.ZERO) > 0) {
            LineaNomina linea = new LineaNomina();
            linea.setNominaCalculada(nomina);
            linea.setDescripcion(descripcion);

            linea.setMonto(monto.doubleValue());
            linea.setReglaSalarial(null);


            em.persist(linea);
        }
    }
}
