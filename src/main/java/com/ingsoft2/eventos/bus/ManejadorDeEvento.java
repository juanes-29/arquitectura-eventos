package com.ingsoft2.eventos.bus;

import com.ingsoft2.eventos.evento.Evento;

/**
 * Un MANEJADOR es el código que un consumidor ejecuta cuando le llega un evento.
 *
 * Es una interfaz funcional, así que se puede pasar como lambda o como referencia
 * a método. Por ejemplo:
 *   bus.suscribir(PedidoCreado.class, this::alCrearsePedido);
 *
 * @param <T> tipo de evento que sabe manejar
 */
@FunctionalInterface
public interface ManejadorDeEvento<T extends Evento> {

    void manejar(T evento);
}
