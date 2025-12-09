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

        // =================================================================================
        // 0. LIMPIEZA PREVIA: Borrar cálculos anteriores de este lote para evitar duplicados
        // =================================================================================
        // Primero borramos las líneas (detalle) asociadas a las nóminas de este lote
        String deleteLineas = "DELETE FROM LineaNomina l WHERE l.nominaCalculada.id IN " +
                "(SELECT n.id FROM NominaCalculada n WHERE n.loteNomina.id = :loteId)";
        em.createQuery(deleteLineas).setParameter("loteId", loteId).executeUpdate();

        // Luego borramos las cabeceras (nóminas) de este lote
        String deleteNominas = "DELETE FROM NominaCalculada n WHERE n.loteNomina.id = :loteId";
        em.createQuery(deleteNominas).setParameter("loteId", loteId).executeUpdate();

        em.flush(); // Forzamos el borrado inmediato en la BD
        // =================================================================================


        // 1. Buscamos empleados activos
        String jpql = "FROM Empleado e WHERE e.estado = true";
        TypedQuery<Empleado> query = em.createQuery(jpql, Empleado.class);
        List<Empleado> empleados = query.getResultList();

        int procesados = 0;

        for (Empleado emp : empleados) {

            // Buscar contrato vigente
            String contratoJpql = "FROM Contrato c WHERE c.empleado.id = :empId";
            // OJO: Si tienes muchos contratos, deberías filtrar por el activo o fecha más reciente
            List<Contrato> contratos = em.createQuery(contratoJpql, Contrato.class)
                    .setParameter("empId", emp.getId())
                    .getResultList();

            if (contratos.isEmpty()) continue; // Si no tiene contrato, saltamos al siguiente empleado

            Contrato contrato = contratos.get(0);

            // --- Cálculos ---
            BigDecimal salarioBase = contrato.getSalarioMensual(); // Asumiendo que en Contrato es Double
            if (salarioBase == null) salarioBase = BigDecimal.ZERO;

            // Asegurarnos que la calculadora no devuelva NULL
            BigDecimal inss = CalculadoraImpuestos.calcularINSS(salarioBase);
            if (inss == null) inss = BigDecimal.ZERO;

            BigDecimal ir = CalculadoraImpuestos.calcularIR(salarioBase);
            if (ir == null) ir = BigDecimal.ZERO;

            // --- Deducciones Voluntarias ---
            BigDecimal totalDeduccionesVoluntarias = BigDecimal.ZERO;

            // Usamos una consulta segura para evitar errores si no hay deducciones
            String dedJpql = "FROM DeduccionVoluntaria d WHERE d.empleado.id = :empId"; // Quitamos 'AND activo=true' si no tienes ese campo aun
            // Si tienes el campo 'activo' en DeduccionVoluntaria, descomenta la siguiente línea:
            // dedJpql = "FROM DeduccionVoluntaria d WHERE d.empleado.id = :empId AND d.activo = true";

            List deducciones = em.createQuery(dedJpql).setParameter("empId", emp.getId()).getResultList();

            // --- Calcular Totales Finales ---
            BigDecimal granTotalDeducciones = inss.add(ir);

            // Sumar voluntarias (usando casting seguro)
            for (Object obj : deducciones) {
                // Asumo que tu entidad es DeduccionVoluntaria, ajusta si es necesario
                // DeduccionVoluntaria ded = (DeduccionVoluntaria) obj;
                // Double cuotaVal = ded.getCuotaMensual();
                // if (cuotaVal != null) {
                //    granTotalDeducciones = granTotalDeducciones.add(BigDecimal.valueOf(cuotaVal));
                // }
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
        lote.setEstado(EstadoLote.CERRADO); // O el estado que prefieras, ej: CALCULADO
        em.merge(lote);

        // Confirmar cambios y refrescar pantalla
        em.flush();
        getView().refresh();
        addMessage("Nómina recalculada correctamente. Se procesaron " + procesados + " empleados.");
    }


    private void crearLinea(EntityManager em, NominaCalculada nomina, String descripcion, BigDecimal monto, TipoRegla tipo) {
        if (monto != null && monto.compareTo(BigDecimal.ZERO) > 0) {
            LineaNomina linea = new LineaNomina();
            linea.setNominaCalculada(nomina);
            linea.setDescripcion(descripcion); // Asegúrate que LineaNomina tenga este campo

            linea.setMonto(monto.doubleValue());
            linea.setReglaSalarial(null);


            em.persist(linea);
        }
    }
}
