# Arquitectura orientada a eventos

Actividad 2 de Ingeniería de Software 2: ejemplo en Java de una arquitectura orientada a eventos (EDA, *Event-Driven Architecture*).

## ¿Qué es?

Es un estilo de arquitectura en el que los componentes **no se llaman entre sí**. En lugar de eso:

- Un **productor** publica un **evento**: un hecho que ya ocurrió, como `PedidoCreado`.
- Un **bus de eventos** (broker) lo recibe y lo reparte.
- Los **consumidores** que se suscribieron a ese tipo de evento reaccionan a él y, si hace falta, publican nuevos eventos.

El productor no sabe quién consume sus eventos ni cuántos consumidores hay. Eso es lo que hace al sistema **desacoplado**.

## El ejemplo: una tienda en línea

```
ServicioPedidos ──PedidoCreado──► ServicioInventario ──StockReservado──► ServicioPagos
                       │                   │                               │
                       │            StockInsuficiente            PagoAprobado / PagoRechazado
                       ▼                   ▼                               ▼
               ServicioNotificaciones   ServicioNotificaciones   ServicioFacturacion
                                                                 ServicioNotificaciones
                                                                 ServicioInventario (si se rechaza)
```

Todas las flechas pasan por el `BusDeEventos`. Ningún servicio tiene referencia a otro.

| Servicio | Escucha | Publica |
|---|---|---|
| `ServicioPedidos` | — | `PedidoCreado` |
| `ServicioInventario` | `PedidoCreado`, `PagoRechazado` | `StockReservado`, `StockInsuficiente` |
| `ServicioPagos` | `StockReservado` | `PagoAprobado`, `PagoRechazado` |
| `ServicioFacturacion` | `PagoAprobado` | — |
| `ServicioNotificaciones` | `PedidoCreado`, `StockInsuficiente`, `PagoAprobado`, `PagoRechazado` | — |

`Main` ejecuta tres escenarios:

1. **Compra exitosa**: el pedido recorre toda la cadena hasta la factura.
2. **Sin stock**: el flujo se detiene en inventario y nunca se cobra, porque pagos solo escucha `StockReservado`.
3. **Pago rechazado**: inventario recibe `PagoRechazado` y devuelve las unidades. Esto es una **acción compensatoria**.

## Estructura del código

```
src/main/java/com/ingsoft2/eventos/
├── Main.java                  Arma el sistema y ejecuta los escenarios
├── bus/
│   ├── BusDeEventos.java      Suscripciones, publicación y entrega asíncrona
│   └── ManejadorDeEvento.java Interfaz que implementa cada reacción a un evento
├── evento/                    Los eventos (records inmutables, nombrados en pasado)
│   ├── Evento.java
│   ├── PedidoCreado.java
│   ├── StockReservado.java
│   ├── StockInsuficiente.java
│   ├── PagoAprobado.java
│   └── PagoRechazado.java
└── servicio/                  Productores y consumidores
    ├── ServicioPedidos.java
    ├── ServicioInventario.java
    ├── ServicioPagos.java
    ├── ServicioFacturacion.java
    └── ServicioNotificaciones.java
```

## Conceptos que muestra el código

- **Desacoplamiento**: cada servicio recibe solo el bus en su constructor, nunca otro servicio.
- **Asincronía**: `publicar()` deja el evento en una cola y retorna de inmediato. Por eso `[Pedidos] Le respondo al cliente` aparece antes de que inventario procese el pedido.
- **Coreografía**: no hay un coordinador central. El orden del proceso sale de qué evento escucha cada servicio.
- **Extensibilidad**: para agregar una funcionalidad nueva basta con crear un servicio que se suscriba. No se modifica ninguno de los existentes.
- **Aislamiento de fallos**: si un consumidor lanza una excepción, el bus la registra y sigue entregando el evento a los demás.
- **Consistencia eventual**: durante unos instantes el pedido existe pero el stock todavía no se ha descontado. El sistema llega a un estado consistente un poco después.

## Ventajas y desventajas

| Ventajas | Desventajas |
|---|---|
| Bajo acoplamiento entre componentes | Más difícil seguir y depurar el flujo completo |
| Fácil de extender con nuevos consumidores | Consistencia eventual, no inmediata |
| Cada servicio escala por separado | Hay que manejar eventos duplicados o fuera de orden |
| Respuesta rápida al usuario (asíncrono) | Requiere infraestructura extra (broker) |

## En un sistema real

`BusDeEventos` funciona en memoria para explicar el patrón. En producción se usa un broker externo como **Apache Kafka**, **RabbitMQ** o **AWS SNS/SQS**, que guarda los eventos en disco, reintenta entregas fallidas y conecta servicios que corren en máquinas distintas.

## Requisitos

- Java 17 o superior
- Maven 3.9 o superior

## Compilar y ejecutar

```bash
mvn compile
java -cp target/classes com.ingsoft2.eventos.Main
```
