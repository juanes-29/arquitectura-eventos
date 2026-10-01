package com.ingsoft2.eventos.servicio;

import com.ingsoft2.eventos.bus.BusDeEventos;
import com.ingsoft2.eventos.evento.PagoAprobado;
import com.ingsoft2.eventos.evento.PagoRechazado;
import com.ingsoft2.eventos.evento.StockReservado;

/**
 * CONSUMIDOR y PRODUCTOR: cobra los pedidos.
 *
 * Escucha StockReservado, no PedidoCreado. Así solo se cobra cuando ya se sabe
 * que hay producto para entregar. El orden del proceso lo definen los eventos a los que
 * se suscribe cada servicio, no un método que llame a los demás en secuencia.
 *
 * Publica PagoAprobado o PagoRechazado.
 */
public class ServicioPagos {

    /**
     * Regla simulada: la tarjeta de prueba tiene un cupo máximo por compra.
     * En la vida real aquí se llamaría a una pasarela de pagos.
     */
    private static final long CUPO_MAXIMO = 3_000_000;

    private final BusDeEventos bus;

    public ServicioPagos(BusDeEventos bus) {
        this.bus = bus;
        bus.suscribir(StockReservado.class, this::alReservarseStock);
    }

    private void alReservarseStock(StockReservado evento) {
        if (evento.total() > CUPO_MAXIMO) {
            System.out.printf("  [Pagos] Cobro de $%,d para %s RECHAZADO: supera el cupo.%n",
                    evento.total(), evento.idPedido());
            bus.publicar(new PagoRechazado(evento.idPedido(), evento.cliente(), evento.producto(),
                    evento.cantidad(), "Supera el cupo de la tarjeta"));
            return;
        }

        System.out.printf("  [Pagos] Cobro de $%,d para %s APROBADO.%n", evento.total(), evento.idPedido());
        bus.publicar(new PagoAprobado(evento.idPedido(), evento.cliente(), evento.producto(),
                evento.cantidad(), evento.total()));
    }
}
