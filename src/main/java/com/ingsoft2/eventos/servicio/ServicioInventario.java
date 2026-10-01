package com.ingsoft2.eventos.servicio;

import com.ingsoft2.eventos.bus.BusDeEventos;
import com.ingsoft2.eventos.evento.PagoRechazado;
import com.ingsoft2.eventos.evento.PedidoCreado;
import com.ingsoft2.eventos.evento.StockInsuficiente;
import com.ingsoft2.eventos.evento.StockReservado;

import java.util.HashMap;
import java.util.Map;

/**
 * CONSUMIDOR y PRODUCTOR a la vez: maneja las existencias de cada producto.
 *
 * Escucha:
 *
 *   PedidoCreado: intenta apartar las unidades.
 *   PagoRechazado: devuelve las unidades apartadas (acción compensatoria).
 *
 * Publica:
 *   StockReservado si había unidades suficientes.
 *   StockInsuficiente si no las había.
 *
 *
 * Que un servicio reaccione a un evento publicando otro es lo que forma la cadena
 * del proceso. A este estilo, donde no hay un "jefe" que coordine y cada servicio sabe
 * qué hacer cuando ocurre algo, se le llama coreografía.
 */
public class ServicioInventario {

    private final BusDeEventos bus;

    /**
     * Unidades disponibles por producto. Un HashMap simple basta porque el bus entrega
     * todos los eventos desde un único hilo.
     */
    private final Map<String, Integer> existencias = new HashMap<>();

    public ServicioInventario(BusDeEventos bus, Map<String, Integer> existenciasIniciales) {
        this.bus = bus;
        this.existencias.putAll(existenciasIniciales);

        // Aquí el servicio declara a qué eventos reacciona. Esto es todo el "cableado".
        bus.suscribir(PedidoCreado.class, this::alCrearsePedido);
        bus.suscribir(PagoRechazado.class, this::alRechazarsePago);
    }

    private void alCrearsePedido(PedidoCreado evento) {
        int disponible = existencias.getOrDefault(evento.producto(), 0);

        if (disponible < evento.cantidad()) {
            System.out.printf("  [Inventario] Solo hay %d %s, el pedido %s pide %d. No se puede reservar.%n",
                    disponible, evento.producto(), evento.idPedido(), evento.cantidad());
            bus.publicar(new StockInsuficiente(evento.idPedido(), evento.cliente(), evento.producto(),
                    evento.cantidad(), disponible));
            return;
        }

        existencias.put(evento.producto(), disponible - evento.cantidad());
        System.out.printf("  [Inventario] Reservo %d %s para %s. Quedan %d.%n",
                evento.cantidad(), evento.producto(), evento.idPedido(), disponible - evento.cantidad());
        bus.publicar(new StockReservado(evento.idPedido(), evento.cliente(), evento.producto(),
                evento.cantidad(), evento.total()));
    }

    private void alRechazarsePago(PagoRechazado evento) {
        int nuevoTotal = existencias.getOrDefault(evento.producto(), 0) + evento.cantidad();
        existencias.put(evento.producto(), nuevoTotal);
        System.out.printf("  [Inventario] El pago de %s fue rechazado: devuelvo %d %s al stock. Quedan %d.%n",
                evento.idPedido(), evento.cantidad(), evento.producto(), nuevoTotal);
    }

    /** Consulta de solo lectura, usada al final de la demo para mostrar el estado. */
    public Map<String, Integer> existencias() {
        return Map.copyOf(existencias);
    }
}
