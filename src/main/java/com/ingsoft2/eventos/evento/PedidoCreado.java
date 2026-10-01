package com.ingsoft2.eventos.evento;

/**
 * Se publica cuando un cliente hace un pedido.
 *
 * Productor: ServicioPedidos.
 * Consumidores: ServicioInventario (reserva el stock) y ServicioNotificaciones
 * (avisa al cliente que su pedido fue recibido).
 */
public record PedidoCreado(String idPedido, String cliente, String producto,
                           int cantidad, long total) implements Evento {
}
