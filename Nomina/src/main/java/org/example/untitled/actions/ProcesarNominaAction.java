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

        // 1. Obtener el ID del Lote actual desde la vista
        Map key = getView().getKeyValues();
        String loteId = (String) key.get("id");

        if (Is.empty(loteId)) {
            addError("Primero debes guardar el Lote de Nómina.");
            return;
        }

        EntityManager em = XPersistence.getManager();
        LoteNomina lote = em.find(LoteNomina.class, loteId);

        if (lote == null) {
            addError("No se encontró el lote.");
            return;
        }

        // Validación: No permitir recalcular si ya está pagado
        if (lote.getEstado() == EstadoLote.PAGADO || lote.getEstado() == EstadoLote.DEVENGADO) {
            addError("Este lote ya está procesado (Devengado/Pagado) y no se puede modificar.");
            return;
        }

        // =================================================================================
        // 2. LIMPIEZA: Borrar cálculos anteriores de este lote (para evitar duplicados)
        // =================================================================================
        String deleteLineas = "DELETE FROM LineaNomina l WHERE l.nominaCalculada.id IN " +
                "(SELECT n.id FROM NominaCalculada n WHERE n.loteNomina.id = :loteId)";
        em.createQuery(deleteLineas).setParameter("loteId", loteId).executeUpdate();

        String deleteNominas = "DELETE FROM NominaCalculada n WHERE n.loteNomina.id = :loteId";
        em.createQuery(deleteNominas).setParameter("loteId", loteId).executeUpdate();

        em.flush(); // Aplicar borrado

        // =================================================================================
        // 3. PROCESAMIENTO
        // =================================================================================

        // Buscamos TODOS los empleados (sin filtrar por estado, como pediste)
        String jpql = "FROM Empleado e";
        TypedQuery<Empleado> query = em.createQuery(jpql, Empleado.class);
        List<Empleado> empleados = query.getResultList();

        int procesados = 0;

        for (Empleado emp : empleados) {

            // A. Buscar contrato vigente
            // Seleccionamos el contrato del empleado. Si hay varios, toma el primero (idealmente filtrar por activo)
            String contratoJpql = "FROM Contrato c WHERE c.empleado.id = :empId";
            List<Contrato> contratos = em.createQuery(contratoJpql, Contrato.class)
                    .setParameter("empId", emp.getId())
                    .getResultList();

            if (contratos.isEmpty()) continue; // Si no tiene contrato, no se le paga

            Contrato contrato = contratos.get(0);

            // B. Obtener Salario Base
            BigDecimal salarioBase = contrato.getSalarioMensual();
            if (salarioBase == null) salarioBase = BigDecimal.ZERO;

            // C. Calcular Impuestos de Ley (INSS e IR)
            BigDecimal inss = CalculadoraImpuestos.calcularINSS(salarioBase);
            if (inss == null) inss = BigDecimal.ZERO;

            BigDecimal ir = CalculadoraImpuestos.calcularIR(salarioBase);
            if (ir == null) ir = BigDecimal.ZERO;

            // D. Calcular Deducciones Voluntarias (Préstamos, etc.)
            BigDecimal totalDeduccionesVoluntarias = BigDecimal.ZERO;

            // Asumiendo que DeduccionVoluntaria tiene campo 'activo'
            String dedJpql = "FROM DeduccionVoluntaria d WHERE d.empleado.id = :empId AND d.activo = true";
            List<DeduccionVoluntaria> deducciones = em.createQuery(dedJpql, DeduccionVoluntaria.class)
                    .setParameter("empId", emp.getId())
                    .getResultList();

            // E. Totales
            BigDecimal granTotalDeducciones = inss.add(ir);

            // F. Persistir Cabecera (NominaCalculada)
            NominaCalculada nomina = new NominaCalculada();
            nomina.setLoteNomina(lote);
            nomina.setEmpleado(emp);

            // Guardamos la entidad primero para poder asignarle líneas hijas
            em.persist(nomina);

            // G. Crear Líneas de Detalle (Ingresos)
            crearLinea(em, nomina, "Salario Base", salarioBase, TipoRegla.INGRESO);

            // H. Crear Líneas de Detalle (Deducciones de Ley)
            crearLinea(em, nomina, "INSS Laboral (" + (CalculadoraImpuestos.TASA_INSS_LABORAL * 100) + "%)", inss, TipoRegla.DEDUCCION);
            crearLinea(em, nomina, "IR (Impuesto Renta)", ir, TipoRegla.DEDUCCION);

            // I. Procesar y guardar líneas de Deducciones Voluntarias
            for (DeduccionVoluntaria ded : deducciones) {
                if (ded.getCuotaMensual() != null && ded.getCuotaMensual() > 0) {
                    BigDecimal cuota = BigDecimal.valueOf(ded.getCuotaMensual());

                    // Sumar al total
                    granTotalDeducciones = granTotalDeducciones.add(cuota);
                    totalDeduccionesVoluntarias = totalDeduccionesVoluntarias.add(cuota);

                    // Crear línea en la colilla
                    crearLinea(em, nomina, ded.getConcepto(), cuota, TipoRegla.DEDUCCION);
                }
            }

            // J. Actualizar Totales Finales en la Cabecera
            BigDecimal totalPagar = salarioBase.subtract(granTotalDeducciones);

            nomina.setTotalDevengado(salarioBase.doubleValue());
            nomina.setTotalDeducciones(granTotalDeducciones.doubleValue());
            nomina.setTotalPagar(totalPagar.doubleValue());

            // Actualizamos la nómina con los valores finales
            em.merge(nomina);

            procesados++;
        }

        // 4. Actualizar estado del lote
        lote.setEstado(EstadoLote.CALCULADO); // Marcamos como calculado
        em.merge(lote);

        // 5. Finalizar
        em.flush();
        getView().refresh(); // Refresca la vista para ver los nuevos datos
        addMessage("Nómina procesada con éxito. Se calcularon " + procesados + " empleados.");
    }

    // Método auxiliar para crear líneas limpiamente
    private void crearLinea(EntityManager em, NominaCalculada nomina, String descripcion, BigDecimal monto, TipoRegla tipo) {
        if (monto != null && monto.compareTo(BigDecimal.ZERO) > 0) {
            LineaNomina linea = new LineaNomina();
            linea.setNominaCalculada(nomina);
            linea.setDescripcion(descripcion);
            linea.setMonto(monto.doubleValue());
            // linea.setReglaSalarial(null);
            em.persist(linea);
        }
    }
}