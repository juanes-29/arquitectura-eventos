package com.ingsoft2.eventos.evento;

/**
 * Se publica cuando no hay unidades suficientes para atender un pedido.
 *
 * Productor: ServicioInventario.
 * Consumidor: ServicioNotificaciones (le explica al cliente por qué no se procesó).
 *
 */
public record StockInsuficiente(String idPedido, String cliente, String producto,
                                int solicitado, int disponible) implements Evento {
}
