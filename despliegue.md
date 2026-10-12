# Despliegue en un entorno de nube

El sistema no se instala servicio por servicio en una máquina. Cada componente ya tiene una imagen en el `Dockerfile` y lee su configuración por variables de entorno. En la nube se usan esas mismas imágenes. Lo que cambia es la red, el registro donde se guardan y las direcciones que dejan de ser `localhost`.

Este documento usa AWS como ejemplo, porque el balanceador, el registro de imágenes y un clúster de contenedores cubren lo que el proyecto necesita. Las mismas imágenes sirven en otro proveedor si ofrece contenedores, una red privada y un balanceador.

## Qué deja listo el repositorio

- Una etapa del `Dockerfile` por servicio: `discovery`, `auth`, `config`, `accounts`, `transactions`, `notification`, `customers`, `payments`, `batch`, `gateway`, `bff-web`, `bff-mobile` y `bff-atm`. La base de ejecución es Eclipse Temurin 21 y el proceso parte con `-Xmx256m`.
- `GET /actuator/health` en cada servicio, para que el balanceador sepa si el contenedor está vivo. Dentro de la imagen hay `curl`, que es lo que usa el healthcheck de Compose.
- Eureka para encontrar al resto. El gateway no tiene IP escritas: las rutas son `lb://accounts-service`, `lb://transactions-service`, `lb://customers-service`, `lb://payments-service`, `lb://notification-service` y `lb://batch-service`.
- Config Server en el puerto 8888, perfil `native`, con la configuración común en `classpath:/config`. El puerto de cada servicio no va en ese archivo.
- Kafka con 3 brokers, 3 particiones y replicación 3. Los topics son `bank.transacciones`, `bank.pagos` y `bank.alertas`.
- Los CSV de `data/semana_3` viajan dentro del jar. No hay que montar un disco para leerlos.

Hay dos límites que hay que tener presentes antes de tratar esto como producción. El batch guarda su historial en H2 en memoria, y los pagos viven en memoria del proceso. Si el contenedor se reinicia, ese estado se pierde. Las cuentas, los clientes y las transacciones de la semana 3 vuelven a cargarse desde el CSV. Para dejar el batch y los pagos entre reinicios haría falta una base externa. Este despliegue no la agrega.

## Herramientas

- Docker, para construir las imágenes en la máquina de quien despliega.
- AWS CLI, autenticada en la cuenta (`aws sts get-caller-identity` tiene que responder).
- Amazon ECR, un repositorio por imagen.
- Una VPC con subredes privadas para Kafka, Eureka, Config Server, OAuth y los microservicios, y subredes públicas solo para el balanceador.
- Amazon ECS con Fargate, o un host donde corra Docker. Aquí se describe ECS. Un solo servidor con `docker compose` también sirve para una prueba en la nube, pero no separa la red.
- Un Application Load Balancer con HTTPS hacia afuera.
- AWS Certificate Manager, para el certificado del balanceador. El `keystore.p12` de los BFF tiene CN `localhost` y solo vale en local.
- Security groups: el balanceador recibe 443 desde internet. El resto solo acepta tráfico desde el security group del propio sistema.

No hace falta cambiar código para apuntar a otro host. Las direcciones salen de estas variables, que ya lee cada `application.yml`:

| Variable | Quién la usa | Valor en la nube |
| --- | --- | --- |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | todos los servicios Java | `http://discovery.banco.interno:8761/eureka/` |
| `SPRING_CONFIG_IMPORT` | servicios de negocio, gateway y BFF | `optional:configserver:http://config.banco.interno:8888` |
| `APP_JWT_ISSUER` | auth y todos los que validan el JWT | la URL pública de auth, la misma en todos |
| `APP_JWT_JWK_SET_URI` | quienes validan el token | `http://auth.banco.interno:9000/oauth2/jwks` |
| `APP_AUTH_TOKEN_URI` | transacciones y los tres BFF | `http://auth.banco.interno:9000/oauth2/token` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | transacciones, pagos y notificaciones | `kafka-1.banco.interno:9092,kafka-2.banco.interno:9092,kafka-3.banco.interno:9092` |
| `SPRING_KAFKA_PRODUCER_ACKS` | transacciones y pagos | `all` |
| `APP_DATA_SEMANA` | cuentas, transacciones, clientes y batch | `semana_3` |

`APP_JWT_ISSUER` tiene que ser idéntico en el auth y en el resto. El token sale con ese emisor y los demás lo rechazan si no coincide. La URL de las llaves (`APP_JWT_JWK_SET_URI`) sí puede ser la interna, porque los contenedores se hablan por la red privada. En local el emisor es `http://localhost:9000`. En la nube esa dirección no sirve.

Los nombres `*.banco.interno` son los de Cloud Map o del servicio de ECS. Pueden ser otros, siempre que el contenedor los resuelva dentro de la VPC.

## 1. Construir y publicar las imágenes

En la raíz del repositorio, con la sesión de ECR abierta:

```bash
AWS_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=us-east-1
REGISTRY=$AWS_ACCOUNT.dkr.ecr.$AWS_REGION.amazonaws.com

aws ecr get-login-password --region $AWS_REGION \
  | docker login --username AWS --password-stdin $REGISTRY
```

Hay que crear un repositorio ECR por target. Los nombres usados abajo son los del `Dockerfile`:

```bash
for NAME in discovery auth config accounts transactions notification customers payments batch gateway bff-web bff-mobile bff-atm; do
  aws ecr create-repository --repository-name banco/$NAME --region $AWS_REGION
done
```

Si el repositorio ya existe, ese comando falla y se sigue con el build. Cada imagen se construye con el target que le corresponde:

```bash
for NAME in discovery auth config accounts transactions notification customers payments batch gateway bff-web bff-mobile bff-atm; do
  docker build --target $NAME -t $REGISTRY/banco/$NAME:1.0.0 .
  docker push $REGISTRY/banco/$NAME:1.0.0
done
```

Kafka no sale de este `Dockerfile`. En ECS se usan tres tareas con la imagen `apache/kafka:3.9.1`. La interfaz de Kafka, si se quiere ver los topics, es `provectuslabs/kafka-ui:v0.7.2` y no debe quedar publicada a internet.

## 2. Red

Dentro de la VPC:

- El balanceador queda en las subredes públicas y escucha 443.
- Eureka, Config Server, auth, Kafka y los microservicios quedan en las subredes privadas, sin IP pública.
- Un namespace privado de Cloud Map, por ejemplo `banco.interno`, registra `discovery`, `config`, `auth`, `kafka-1`, `kafka-2` y `kafka-3`.

Security groups:

- Balanceador: entra 443 desde `0.0.0.0/0`. Sale hacia los puertos de gateway y de los BFF.
- Servicios Java: entran solo desde el security group del balanceador y desde el security group de los propios servicios. Puertos 8761, 8888, 9000, 8080 a 8086 y 8091 a 8093.
- Kafka: entra 9092 y 9093 solo desde los servicios y desde los otros brokers.

Eureka, Config Server, auth y Kafka no se publican en el balanceador.

## 3. Kafka

Tres tareas, una por broker, con la misma configuración que `docker-compose.yml`:

- `KAFKA_PROCESS_ROLES=broker,controller`
- `KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka-1.banco.interno:9093,2@kafka-2.banco.interno:9093,3@kafka-3.banco.interno:9093`
- `KAFKA_NODE_ID` distinto en cada una: 1, 2 y 3
- `KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka-1.banco.interno:9092` (y el hostname que corresponda en cada broker)
- `KAFKA_NUM_PARTITIONS=3`
- `KAFKA_DEFAULT_REPLICATION_FACTOR=3`
- `KAFKA_MIN_INSYNC_REPLICAS=2`
- `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=3`
- `CLUSTER_ID` igual en los tres. El de Compose es `4L6g3nShT-eMCtK--X86sw`

Cada tarea pide al menos 512 MB, porque el heap del broker ya usa 256 MB. El servicio de ECS debe tener un nombre estable por broker. Si los tres comparten un solo nombre, el productor no arma el conjunto de réplicas.

Los topics se crean solos la primera vez que alguien publica (`KAFKA_AUTO_CREATE_TOPICS_ENABLE=true`).

## 4. Orden en que se levantan los servicios

ECS no espera como Compose. Hay que crear los servicios en este orden y pasar al siguiente cuando el health responde UP:

1. Los tres brokers de Kafka.
2. `discovery` (8761). Sin Eureka el resto no se registra.
3. `auth` (9000) y `config` (8888).
4. `accounts` (8081), `customers` (8084), `transactions` (8082), `payments` (8085), `notification` (8083) y `batch` (8086).
5. `gateway` (8080) y los BFF (8091, 8092, 8093).

En cada tarea Java van las variables de la tabla de arriba. Transacciones, pagos y notificaciones llevan además los brokers. Transacciones y los BFF llevan `APP_AUTH_TOKEN_URI`. El healthcheck del balanceador y de ECS es `GET /actuator/health`, con un margen de arranque de unos 40 segundos. En los BFF el proceso escucha HTTPS, así que el chequeo interno usa el puerto del contenedor y acepta el certificado local. Hacia afuera el TLS lo termina el balanceador.

Memoria de la tarea: 512 MB alcanza para el heap de 256 MB más el proceso. Kafka, como se dijo, también 512 MB como mínimo.

Config Server arranca con `SPRING_PROFILES_ACTIVE=native` ya definido en su `application.yml`. No hay que pasarle los CSV ni un repositorio git. La configuración común es el patrón de log y la exposición de `health`, `info` y `prometheus`.

## 5. Qué queda público

El balanceador reparte así:

| Ruta o puerto público | Destino | Quién lo usa |
| --- | --- | --- |
| `/api/cuentas`, `/api/transacciones`, `/api/eventos`, `/api/clientes`, `/api/pagos`, `/api/batch` | gateway, 8080 | API común del núcleo |
| el host del canal web | bff-web, 8091 | panel |
| el host del canal móvil | bff-mobile, 8092 | saldo resumido |
| el host del canal cajero | bff-atm, 8093 | saldo y retiro |
| la URL de auth | auth, 9000, solo `POST /oauth2/token` y `GET /oauth2/jwks` | quien pide el JWT desde fuera |

El emisor del token (`APP_JWT_ISSUER`) es esa URL pública de auth, por ejemplo `https://auth.ejemplo.com`. Los BFF, que están dentro de la VPC, piden su token de servicio a `http://auth.banco.interno:9000/oauth2/token` y no salen a internet para eso.

El certificado de ACM va en el listener 443. El balanceador habla con el gateway por HTTP. Con los BFF puede hablar HTTPS contra el certificado de localhost del contenedor, marcando ese target group para que no valide el nombre, o se reemplaza `keystore.p12` por uno emitido para el dominio antes de construir la imagen. El archivo que viene en el repositorio no sirve para un dominio real.

## 6. Comprobar el despliegue

Cuando Eureka muestre las instancias UP, desde una máquina que llegue al balanceador:

```bash
TOKEN=$(curl -s -u bank-client:bank-secret \
  -d grant_type=client_credentials \
  -d scope="accounts.read transactions.read" \
  https://api.ejemplo.com/oauth2/token | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])')

curl -s -H "Authorization: Bearer $TOKEN" https://api.ejemplo.com/api/cuentas/resumen
curl -s -H "Authorization: Bearer $TOKEN" https://api.ejemplo.com/api/transacciones/informe
```

La URL del token y la de la API dependen de cómo se hayan creado las reglas del balanceador. Si auth está en otro host, el `curl` del token apunta a ese host. El resultado de la semana 3 tiene que ser el mismo que en local: 1.000 cuentas leídas, 17 válidas y saldo 162.000; 1.000 transacciones leídas, 401 válidas y monto 527.000.

Si la primera llamada dice que el servicio no está disponible, el gateway todavía no vio la instancia en Eureka. Se repite a los pocos segundos.

Los canales se prueban con su cliente. El certificado lo presenta el balanceador, así que aquí no se usa `-k`:

```bash
WEB=$(curl -s -u web-client:web-secret -d grant_type=client_credentials -d scope="bff.web" \
  https://auth.ejemplo.com/oauth2/token | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])')
curl -s -H "Authorization: Bearer $WEB" https://web.ejemplo.com/api/panel
```

El móvil (`mobile-client`, scope `bff.mobile`, `GET /api/resumen`) y el cajero (`atm-client`, scope `bff.atm`, `GET /api/saldo/106` y `POST /api/retiros`) se prueban igual, cada uno contra su host.

El batch se lanza por el gateway, con un token que incluya `batch.run`:

```bash
curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  https://api.ejemplo.com/api/batch/jobs/reporteTransaccionesDiarias
```

En el reporte se esperan 1000 leídas, 401 válidas y monto 527000.

## 7. Más de una instancia

Para sumar capacidad se sube el número de tareas de un servicio en ECS, con la misma imagen y las mismas variables. Eureka registra cada tarea porque `prefer-ip-address` está activo y el gateway reparte con `lb://`.

Conviene dejar en una sola tarea, al menos al principio:

- Eureka. El cliente apunta a una URL. Para tener más de un discovery hay que cambiar esa URL a un clúster, y este proyecto no lo trae.
- Auth y Config Server. El estado de los clientes OAuth está en memoria.
- Batch. El historial está en H2 de ese proceso. Dos tareas no comparten las ejecuciones.
- Cada broker de Kafka. Ahí la réplica es el otro broker, no una segunda copia de la misma tarea.

Cuentas, transacciones, clientes, pagos, notificaciones, el gateway y los BFF sí pueden tener más de una tarea. Pagos no comparte la lista entre tareas: un `GET /api/pagos` responde lo que guardó esa instancia. El evento en Kafka sí llega a notificaciones desde cualquiera de ellas.

## 8. Qué mirar si no responde

- El token es rechazado: `APP_JWT_ISSUER` no es el mismo en auth y en el servicio que validó, o el cliente no incluye el scope del endpoint.
- El gateway responde el fallback: el servicio no está en Eureka o su health no está UP. Se revisa el log de esa tarea y `GET /actuator/health` desde dentro de la red.
- No se publican eventos: `SPRING_KAFKA_BOOTSTRAP_SERVERS` no resuelve los tres brokers, o un broker anuncia `localhost` en `KAFKA_ADVERTISED_LISTENERS`.
- Config Server no cambia el puerto ni apaga un servicio. Si un servicio no lee la config, arranca igual, porque el import es `optional`. El síntoma es solo que no aparece el patrón de log común.
