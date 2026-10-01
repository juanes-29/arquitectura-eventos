package com.ingsoft2.eventos.servicio;

import com.ingsoft2.eventos.bus.BusDeEventos;
import com.ingsoft2.eventos.evento.PagoAprobado;
import com.ingsoft2.eventos.evento.PagoRechazado;
import com.ingsoft2.eventos.evento.PedidoCreado;
import com.ingsoft2.eventos.evento.StockInsuficiente;

/**
 * CONSUMIDOR puro: le envía correos al cliente (aquí solo los imprime).
 *
 * Es el servicio que más eventos escucha, y aun así ningún otro servicio depende de él.
 * Muestra bien la ventaja del patrón: para avisarle algo al cliente, ninguna clase
 * tiene que llamar a "enviarCorreo()"; basta con que publique lo que pasó.
 */
public class ServicioNotificaciones {

    public ServicioNotificaciones(BusDeEventos bus) {
        bus.suscribir(PedidoCreado.class, e ->
                enviarCorreo(e.cliente(), "Recibimos tu pedido " + e.idPedido() + ", lo estamos procesando."));

        bus.suscribir(StockInsuficiente.class, e ->
                enviarCorreo(e.cliente(), "Lo sentimos, solo tenemos " + e.disponible() + " unidades de "
                        + e.producto() + " y pediste " + e.solicitado() + ". Tu pedido " + e.idPedido()
                        + " fue cancelado."));

        bus.suscribir(PagoAprobado.class, e ->
                enviarCorreo(e.cliente(), "Tu pago fue aprobado. El pedido " + e.idPedido() + " va en camino."));

        bus.suscribir(PagoRechazado.class, e ->
                enviarCorreo(e.cliente(), "No pudimos cobrar el pedido " + e.idPedido() + ": " + e.motivo() + "."));
    }

    private void enviarCorreo(String cliente, String mensaje) {
        System.out.printf("  [Notificaciones] Correo a %s: \"%s\"%n", cliente, mensaje);
    }
}
