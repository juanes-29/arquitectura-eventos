package com.ingsoft2.eventos;

import com.ingsoft2.eventos.bus.BusDeEventos;
import com.ingsoft2.eventos.servicio.ServicioFacturacion;
import com.ingsoft2.eventos.servicio.ServicioInventario;
import com.ingsoft2.eventos.servicio.ServicioNotificaciones;
import com.ingsoft2.eventos.servicio.ServicioPagos;
import com.ingsoft2.eventos.servicio.ServicioPedidos;

import java.util.Map;

/**
 * Demostración de una ARQUITECTURA ORIENTADA A EVENTOS (Event-Driven Architecture).
 *
 * Caso: una tienda en línea. Cuando un cliente hace un pedido ocurre esta cadena:
 * <pre>
 *  ServicioPedidos ──PedidoCreado──► ServicioInventario ──StockReservado──► ServicioPagos
 *                         │                   │                               │
 *                         │            StockInsuficiente            PagoAprobado / PagoRechazado
 *                         ▼                   ▼                               ▼
 *                 ServicioNotificaciones   ServicioNotificaciones   ServicioFacturacion
 *                                                                   ServicioNotificaciones
 *                                                                   ServicioInventario (si se rechaza)
 * </pre>
 *
 * Todas las flechas pasan por el BusDeEventos. Ningún servicio tiene una referencia
 * a otro servicio: solo conocen el bus y los eventos.
 *
 * Se ejecutan tres escenarios:
 *
 *   Compra exitosa: el pedido recorre toda la cadena.
 *   Sin stock: el flujo se detiene en inventario y nunca se cobra.
 *   Pago rechazado: inventario recibe el rechazo y devuelve las unidades (compensación).
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        BusDeEventos bus = new BusDeEventos();

        // 1. Se crean los servicios. Cada uno recibe SOLO el bus, nunca a otro servicio.
        //    Al construirse, cada consumidor se suscribe a los eventos que le interesan.
        ServicioPedidos pedidos = new ServicioPedidos(bus);
        ServicioInventario inventario = new ServicioInventario(bus, Map.of("Portatil", 5, "Mouse", 50));
        new ServicioPagos(bus);
        new ServicioFacturacion(bus);
        new ServicioNotificaciones(bus);
        // Para agregar una funcionalidad nueva (por ejemplo, analítica de ventas) bastaría con
        // otra línea como estas: crear el servicio y suscribirlo. Nada de lo anterior cambia.

        titulo("ESCENARIO 1: compra exitosa");
        pedidos.crearPedido("Ana", "Portatil", 1, 2_500_000);
        bus.esperarAQueTermine();

        titulo("ESCENARIO 2: no hay unidades suficientes");
        pedidos.crearPedido("Luis", "Portatil", 10, 2_500_000);
        bus.esperarAQueTermine();

        titulo("ESCENARIO 3: el pago es rechazado y se devuelve el stock");
        pedidos.crearPedido("Marta", "Portatil", 2, 2_500_000);
        bus.esperarAQueTermine();

        titulo("Inventario final");
        System.out.println(inventario.existencias());

        bus.cerrar();
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("==================== " + texto + " ====================");
    }
}
