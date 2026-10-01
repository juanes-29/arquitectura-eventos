package com.ingsoft2.eventos.evento;

/**
 * Un EVENTO es un hecho que ya ocurrió en el sistema y que puede interesarle a otros.
 *
 * Reglas que siguen todos los eventos de este ejemplo:
 *
 *   Se nombran en pasado: PedidoCreado, PagoAprobado... No son órdenes
 *       ("crea un pedido"), son avisos ("se creó un pedido").
 *   Son inmutables: una vez publicados no cambian. Por eso se modelan
 *       como {@code record} de Java, cuyos campos son finales.
 *   Llevan todos los datos necesarios para que el consumidor pueda
 *       reaccionar sin tener que preguntarle nada al servicio que lo publicó.
 *       Así se evita volver a acoplar los servicios.
 *
 *
 * Esta interfaz es el "contrato" común. El bus de eventos solo conoce este tipo,
 * no los servicios que publican o consumen.
 */
public interface Evento {

    /** Nombre legible del evento, usado para mostrarlo en consola. */
    default String nombre() {
        return getClass().getSimpleName();
    }
}
