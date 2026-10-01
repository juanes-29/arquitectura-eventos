package com.ingsoft2.eventos.evento;

/**
 * Se publica cuando el cobro de un pedido falla.
 *
 * Productor: ServicioPagos.
 * Consumidores:
 *
 *   ServicioInventario: devuelve al stock las unidades que había reservado.
 *       Esto se llama acción compensatoria: como no hay una transacción global
 *       entre servicios, cada uno "deshace" su parte cuando se entera de un fallo.
 *   ServicioNotificaciones: avisa al cliente que el pago no pasó.
 *
 */
public record PagoRechazado(String idPedido, String cliente, String producto,
                            int cantidad, String motivo) implements Evento {
}
