# Arquitectura orientada a eventos

Actividad 2 de Ingeniería de Software 2: ejemplo en Java de una arquitectura orientada a eventos (EDA, *Event-Driven Architecture*).

## Resumen

Es una tienda en línea con cinco servicios: pedidos, inventario, pagos, facturación y notificaciones. Los servicios **nunca se llaman entre sí**. Cuando algo pasa, el servicio publica un **evento** en un **bus de eventos**, y los servicios que están suscritos a ese evento reaccionan. Así el sistema queda **desacoplado**: se pueden agregar o quitar servicios sin modificar los demás.

## ¿Cómo funciona?

La arquitectura tiene tres piezas:

| Pieza | Qué es | Dónde está |
|---|---|---|
| **Evento** | Un hecho que ya ocurrió. Se nombra en pasado y no se puede modificar | Carpeta `evento/` |
| **Bus de eventos** | El intermediario que recibe los eventos y los entrega a quien esté suscrito | `bus/BusDeEventos.java` |
| **Productores y consumidores** | Los servicios que publican eventos y los que reaccionan a ellos | Carpeta `servicio/` |

### Los eventos

Son `record` de Java, así que sus datos no se pueden modificar después de creados. Llevan toda la información que necesita quien los recibe (cliente, producto, cantidad, total), para que el consumidor no tenga que preguntarle nada al servicio que lo publicó.

| Evento | Significa |
|---|---|
| `PedidoCreado` | Un cliente hizo un pedido |
| `StockReservado` | Había unidades y quedaron apartadas para el pedido |
| `StockInsuficiente` | No había unidades suficientes |
| `PagoAprobado` | El cobro fue exitoso |
| `PagoRechazado` | El cobro falló |

### El bus de eventos

Solo tiene dos métodos importantes:

- `suscribir(TipoDeEvento, manejador)`: un servicio dice *"avísame cuando pase esto"*.
- `publicar(evento)`: un servicio dice *"pasó esto"*. El bus busca quién está suscrito a ese tipo de evento y se lo entrega.

`publicar()` **no espera** a que los consumidores terminen: deja el evento en una cola y retorna de inmediato. Un hilo aparte (el despachador) va sacando los eventos de la cola y entregándolos.

### Los servicios

En `Main.java` cada servicio recibe **solo el bus**, nunca otro servicio:

```java
ServicioPedidos pedidos = new ServicioPedidos(bus);
new ServicioPagos(bus);
```

Y en su constructor, cada servicio declara a qué eventos reacciona:

```java
bus.suscribir(PedidoCreado.class, this::alCrearsePedido);
```

| Servicio | Escucha | Publica | Rol |
|---|---|---|---|
| `ServicioPedidos` | — | `PedidoCreado` | Solo productor; inicia todo |
| `ServicioInventario` | `PedidoCreado`, `PagoRechazado` | `StockReservado` o `StockInsuficiente` | Consumidor y productor |
| `ServicioPagos` | `StockReservado` | `PagoAprobado` o `PagoRechazado` | Consumidor y productor |
| `ServicioFacturacion` | `PagoAprobado` | — | Solo consumidor |
| `ServicioNotificaciones` | `PedidoCreado`, `StockInsuficiente`, `PagoAprobado`, `PagoRechazado` | — | Solo consumidor |

### Flujo completo

```
ServicioPedidos ──PedidoCreado──► ServicioInventario ──StockReservado──► ServicioPagos
                       │                   │                               │
                       │            StockInsuficiente            PagoAprobado / PagoRechazado
                       ▼                   ▼                               ▼
               ServicioNotificaciones   ServicioNotificaciones   ServicioFacturacion
                                                                 ServicioNotificaciones
                                                                 ServicioInventario (si se rechaza)
```

Todas las flechas pasan por el `BusDeEventos`.

## Escenarios

El programa empieza con 5 portátiles en inventario y una tarjeta con cupo máximo de $3.000.000 por compra. `Main` ejecuta tres escenarios:

### Escenario 1: compra exitosa (Ana compra 1 portátil por $2.500.000)

1. Pedidos publica `PedidoCreado` y **le responde al cliente de inmediato**.
2. Inventario escucha `PedidoCreado`, aparta 1 unidad (quedan 4) y publica `StockReservado`.
3. Notificaciones también escuchó `PedidoCreado` y envía el correo *"recibimos tu pedido"*.
4. Pagos escucha `StockReservado`, cobra y publica `PagoAprobado`.
5. Facturación y Notificaciones escuchan `PagoAprobado`: una emite la factura y la otra envía el correo de confirmación.

### Escenario 2: no hay stock (Luis pide 10 portátiles y solo hay 4)

1. Inventario escucha `PedidoCreado`, ve que no alcanza y publica `StockInsuficiente`.
2. Notificaciones escucha ese evento y le explica al cliente que su pedido fue cancelado.
3. **Nunca se cobra**, porque Pagos solo escucha `StockReservado`. No hizo falta escribir un `if` para impedir el cobro: el flujo lo definen las suscripciones.

### Escenario 3: pago rechazado (Marta compra 2 portátiles por $5.000.000)

1. Inventario aparta 2 unidades (quedan 2) y publica `StockReservado`.
2. Pagos intenta cobrar, pero el total supera el cupo, así que publica `PagoRechazado`.
3. Inventario escucha `PagoRechazado` y **devuelve las 2 unidades al stock** (vuelven a ser 4). Esto se llama **acción compensatoria**: como no hay una transacción global entre servicios, cada uno deshace su parte cuando se entera de un fallo.
4. Notificaciones le avisa al cliente que el pago no pasó.

Al final el inventario queda en `{Portatil=4, Mouse=50}`, que es lo correcto.

## Conceptos que muestra el código

- **Desacoplamiento:** `ServicioPedidos` no importa ni conoce a ningún otro servicio.
- **Asincronía:** el productor no espera a los consumidores. Por eso en consola aparece *"Le respondo al cliente"* **antes** de que Inventario procese el pedido.
- **Coreografía:** no hay un "jefe" que coordine el proceso. El orden sale de qué evento escucha cada servicio. La alternativa es la *orquestación*, donde un servicio central dirige todo.
- **Un evento, varios consumidores:** `PagoAprobado` lo reciben Facturación y Notificaciones al mismo tiempo, sin conocerse entre ellas.
- **Aislamiento de fallos:** si un consumidor lanza una excepción, el bus la captura y sigue entregando el evento a los demás.
- **Consistencia eventual:** por un instante el pedido existe pero el stock aún no se ha descontado. El sistema queda consistente un momento después.
- **Extensibilidad:** para agregar, por ejemplo, un servicio de puntos de fidelidad, basta con crear la clase, suscribirla a `PagoAprobado` y agregar una línea en `Main`. **Ningún servicio existente cambia.**

## Ventajas y desventajas

| Ventajas | Desventajas |
|---|---|
| Bajo acoplamiento entre componentes | Es más difícil seguir y depurar el flujo completo |
| Fácil de extender con nuevos servicios | La consistencia es eventual, no inmediata |
| Cada servicio puede escalar por separado | Pueden llegar eventos duplicados o en desorden |
| Respuesta rápida al usuario (asíncrono) | Requiere infraestructura extra (el broker) |

No conviene en aplicaciones pequeñas o sencillas, ni cuando se necesita una respuesta inmediata con consistencia fuerte, como una transferencia bancaria que debe confirmarse en el momento.

## En un sistema real

`BusDeEventos` funciona en memoria para mostrar la idea. En producción se usa un broker externo como **Apache Kafka**, **RabbitMQ** o **AWS SNS/SQS**, que guarda los eventos en disco, reintenta entregas fallidas y conecta servicios que corren en servidores distintos.

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

## Cómo ejecutarlo

Requisitos: Java 17 o superior y Maven 3.9 o superior.

```bash
mvn compile
java -cp target/classes com.ingsoft2.eventos.Main
```
