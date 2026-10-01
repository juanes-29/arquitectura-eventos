package com.ingsoft2.eventos.servicio;

import com.ingsoft2.eventos.bus.BusDeEventos;
import com.ingsoft2.eventos.evento.PagoAprobado;

/**
 * CONSUMIDOR puro: genera la factura cuando un pago es aprobado.
 *
 * No publica nada, solo reacciona. ServicioPagos no sabe que esta clase existe;
 * si se elimina del sistema, los pagos siguen funcionando igual.
 */
public class ServicioFacturacion {

    private int consecutivo = 0;

    public ServicioFacturacion(BusDeEventos bus) {
        bus.suscribir(PagoAprobado.class, this::alAprobarsePago);
    }

    private void alAprobarsePago(PagoAprobado evento) {
        consecutivo++;
        System.out.printf("  [Facturacion] Factura F-%03d emitida a %s: %d x %s = $%,d (pedido %s).%n",
                consecutivo, evento.cliente(), evento.cantidad(), evento.producto(),
                evento.total(), evento.idPedido());
    }
}
