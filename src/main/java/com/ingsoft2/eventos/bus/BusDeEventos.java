package com.ingsoft2.eventos.bus;

import com.ingsoft2.eventos.evento.Evento;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * El BUS DE EVENTOS es la pieza central de la arquitectura.
 *
 * Su trabajo es muy simple:
 *   Los consumidores le dicen "avísame cuando ocurra un evento de tipo X".
 *   Los productores le dicen "ocurrió este evento".
 *   El bus le entrega el evento a todos los suscritos a ese tipo.
 *
 *
 * <p>Gracias a esto, el productor no sabe quién consume sus eventos ni cuántos consumidores hay, ese es el desacoplamiento que caracteriza a la arquitectura orientada a eventos.</p>
 *
 * Asincronía
 * no ejecuta a los consumidores: deja el evento en una cola y retorna
 * inmediatamente. Un hilo aparte (el "despachador") va sacando los eventos de la cola y
 * entregándolos. Así el productor sigue con su trabajo sin esperar a que los demás terminen.
 *
 * Usamos un solo hilo despachador para que los eventos se entreguen en el mismo orden en que
 * se publicaron. Esto hace que la salida en consola sea fácil de seguir.
 *
 * ¿Y en un sistema real?
 * Esta clase vive en memoria y sirve para entender el patrón. En producción se usa un broker
 * externo como Apache Kafka, RabbitMQ o AWS SNS/SQS, que además guarda los eventos en disco,
 * los reintenta si un consumidor falla y conecta servicios que corren en máquinas distintas.
 * La idea es la misma: publicar, suscribirse y entregar.
 */
public class BusDeEventos {

    /**
     * Registro de suscripciones: para cada tipo de evento, la lista de manejadores interesados.
     * Ejemplo: PedidoCreado.class -> [inventario, notificaciones]
     */
    private final Map<Class<? extends Evento>, List<ManejadorDeEvento<? extends Evento>>> suscriptores =
            new ConcurrentHashMap<>();

    /**
     * Cola + hilo que entrega los eventos. Un "single thread executor" procesa las tareas
     * una por una, en orden de llegada (FIFO).
     */
    private final ExecutorService despachador = Executors.newSingleThreadExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "despachador-de-eventos");
        hilo.setDaemon(true);
        return hilo;
    });

    /** Eventos publicados que todavía no se han terminado de entregar.*/
    private int pendientes = 0;

    /**
     * Registra a un consumidor para que reciba todos los eventos de un tipo.
     *
     * @param tipo      clase del evento que le interesa (por ejemplo {@code PagoAprobado.class})
     * @param manejador código a ejecutar cuando llegue ese evento
     */
    public <T extends Evento> void suscribir(Class<T> tipo, ManejadorDeEvento<T> manejador) {
        suscriptores.computeIfAbsent(tipo, t -> new CopyOnWriteArrayList<>()).add(manejador);
    }

    /**
     * Publica un evento. El método retorna de inmediato; la entrega ocurre después,
     * en el hilo despachador.
     */
    public void publicar(Evento evento) {
        synchronized (this) {
            pendientes++;
        }
        System.out.println("      [BUS] >> " + evento);
        despachador.submit(() -> entregar(evento));
    }

    /**
     * Entrega el evento a cada manejador suscrito a su tipo.
     *
     * Si un consumidor lanza una excepción, se registra y se sigue con los demás:
     * el fallo de un servicio no debe tumbar a los otros (aislamiento de fallos).
     */
    @SuppressWarnings("unchecked")
    private void entregar(Evento evento) {
        try {
            List<ManejadorDeEvento<? extends Evento>> interesados =
                    suscriptores.getOrDefault(evento.getClass(), List.of());

            if (interesados.isEmpty()) {
                System.out.println("      [BUS] (nadie escucha " + evento.nombre() + ")");
            }

            for (ManejadorDeEvento<? extends Evento> manejador : interesados) {
                try {
                    // El cast es seguro: solo se guardan manejadores bajo la clase de evento que aceptan.
                    ((ManejadorDeEvento<Evento>) manejador).manejar(evento);
                } catch (RuntimeException e) {
                    System.out.println("      [BUS] error en un consumidor de " + evento.nombre()
                            + ": " + e.getMessage());
                }
            }
        } finally {
            synchronized (this) {
                pendientes--;
                if (pendientes == 0) {
                    notifyAll();
                }
            }
        }
    }

    /**
     * Bloquea hasta que todos los eventos publicados (incluidos los que se publiquen
     * en cadena mientras tanto) se hayan entregado.
     *
     * En un sistema real nadie espera así; aquí lo usamos solo para que la demo
     * imprima cada escenario completo antes de empezar el siguiente.
     */
    public synchronized void esperarAQueTermine() throws InterruptedException {
        while (pendientes > 0) {
            wait();
        }
    }

    /** Detiene el hilo despachador. */
    public void cerrar() {
        despachador.shutdown();
    }
}
