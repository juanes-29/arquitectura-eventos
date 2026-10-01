package com.ingsoft2.eventos.servicio;

import com.ingsoft2.eventos.bus.BusDeEventos;
import com.ingsoft2.eventos.evento.PedidoCreado;

/**
 * PRODUCTOR que inicia todo el flujo: recibe pedidos de los clientes.
 *
 * Lo importante es lo que esta clase NO hace: no llama al inventario,
 * ni a pagos, ni a facturación, ni a notificaciones. Ni siquiera los importa.
 * Solo registra el pedido y anuncia "se creó un pedido".
 *
 * Si mañana se agrega un servicio de puntos de fidelidad que también quiera
 * enterarse de los pedidos, esta clase no se modifica: el nuevo servicio
 * simplemente se suscribe a PedidoCreado.
 */
public class ServicioPedidos {

    private final BusDeEventos bus;
    private int consecutivo = 0;

    public ServicioPedidos(BusDeEventos bus) {
        this.bus = bus;
    }

    /**
     * Registra un pedido nuevo y publica el evento correspondiente.
     *
     * @return el identificador asignado al pedido
     */
    public String crearPedido(String cliente, String producto, int cantidad, long precioUnitario) {
        consecutivo++;
        String idPedido = String.format("P-%03d", consecutivo);
        long total = cantidad * precioUnitario;

        System.out.printf("[Pedidos] %s pide %d x %s (total $%,d). Pedido %s registrado.%n",
                cliente, cantidad, producto, total, idPedido);

        bus.publicar(new PedidoCreado(idPedido, cliente, producto, cantidad, total));

        // publicar() no espera a los consumidores, así que el cliente recibe su respuesta
        // de inmediato aunque el resto del proceso siga en segundo plano.
        System.out.printf("[Pedidos] Le respondo al cliente: \"Recibimos tu pedido %s\".%n", idPedido);
        return idPedido;
    }
}
