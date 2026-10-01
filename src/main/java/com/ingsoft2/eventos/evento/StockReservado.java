package com.ingsoft2.eventos.evento;

/**
 * Se publica cuando el inventario logró apartar las unidades de un pedido.
 *
 * Productor: ServicioInventario.
 * Consumidor: ServicioPagos (ya puede cobrar, porque hay producto).
 */
public record StockReservado(String idPedido, String cliente, String producto,
                             int cantidad, long total) implements Evento {
}
