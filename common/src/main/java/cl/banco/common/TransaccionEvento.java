package cl.banco.common;

import java.math.BigDecimal;

public class TransaccionEvento {

    private String eventoId;
    private String tipoEvento;
    private Long transaccionId;
    private String tipoMovimiento;
    private BigDecimal monto;
    private String fecha;
    private String ocurridoEn;
    private String detalle;

    public TransaccionEvento() {
    }

    public String getEventoId() {
        return eventoId;
    }

    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public Long getTransaccionId() {
        return transaccionId;
    }

    public void setTransaccionId(Long transaccionId) {
        this.transaccionId = transaccionId;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getOcurridoEn() {
        return ocurridoEn;
    }

    public void setOcurridoEn(String ocurridoEn) {
        this.ocurridoEn = ocurridoEn;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}
