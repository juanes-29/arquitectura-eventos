package com.ingsoft2.eventos.evento;

/**
 * Se publica cuando el cobro de un pedido fue exitoso.
 *
 * Productor: ServicioPagos.
 * Consumidores: ServicioFacturacion (emite la factura) y ServicioNotificaciones
 * (confirma la compra al cliente). Ambos reaccionan al mismo evento sin conocerse.
 */
public record PagoAprobado(String idPedido, String cliente, String producto,
                           int cantidad, long total) implements Evento {
}
