# Развёртывание на WildFly (подключение к БД через сервер)

Приложение **не знает** адреса БД, логина и пароля. Оно берёт готовый `DataSource` у сервера приложений по JNDI-имени
`java:jboss/datasources/StudyGroupsDS` (`application.yml`, профиль `server`, включён по умолчанию).
Адрес, учётные данные и **пул соединений** (сколько соединений держать, лимит, таймауты, проверка живости)
настраиваются на WildFly.

```
браузер → WildFly (Undertow) → study-groups.war → JNDI: StudyGroupsDS → пул IronJacamar → PostgreSQL (pg:5432/studs)
```

> Нужен **WildFly 27+** (Jakarta EE 10): Spring Boot 3 использует пакеты `jakarta.*`. На WildFly 26 и старше
> (`javax.*`) приложение не запустится.

## 1. Сборка

```bash
cd frontend && npm install && npm run build && cd ..
./gradlew war
```
Результат: `build/libs/study-groups.war`. В нём нет встроенного Tomcat и драйвера PostgreSQL — их даёт сервер.

## 2. Порты на helios

Порты 8080/9990 обычно заняты другими студентами — запускать со смещением:
```bash
./bin/standalone.sh -Djboss.socket.binding.port-offset=<N> &
```
HTTP будет на `8080 + N`, консоль управления на `9990 + N`.

## 3. Драйвер и DataSource (jboss-cli)

```bash
./bin/jboss-cli.sh --connect --controller=localhost:$((9990 + N))
```

```
deploy ~/postgresql-42.7.4.jar

data-source add --name=StudyGroupsDS \
  --jndi-name=java:jboss/datasources/StudyGroupsDS \
  --driver-name=postgresql-42.7.4.jar \
  --connection-url=jdbc:postgresql://pg:5432/studs \
  --user-name=sXXXXXX \
  --password=******** \
  --jta=false \
  --min-pool-size=2 \
  --max-pool-size=10 \
  --pool-prefill=true \
  --idle-timeout-minutes=5 \
  --blocking-timeout-wait-millis=5000 \
  --validate-on-match=true \
  --background-validation=false \
  --valid-connection-checker-class-name=org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker \
  --exception-sorter-class-name=org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter

/subsystem=datasources/data-source=StudyGroupsDS:test-connection-in-pool
```

| Параметр | Зачем |
|---|---|
| `min-pool-size` / `max-pool-size` | сколько соединений держать открытыми и **максимум** одновременных подключений к БД |
| `pool-prefill` | открыть `min-pool-size` соединений сразу при старте |
| `idle-timeout-minutes` | закрывать простаивающие соединения |
| `blocking-timeout-wait-millis` | сколько запрос ждёт свободное соединение, если пул исчерпан |
| `validate-on-match` + `valid-connection-checker` | проверять соединение перед выдачей (БД могла его закрыть) |
| `exception-sorter` | выкидывать из пула соединения, сломанные фатальной ошибкой |
| `jta=false` | транзакциями управляет Spring (`JpaTransactionManager`), а не JTA сервера |

Состояние пула во время работы:
```
/subsystem=datasources/data-source=StudyGroupsDS/statistics=pool:read-resource(include-runtime=true)
```
(перед этим включить статистику: `/subsystem=datasources/data-source=StudyGroupsDS:write-attribute(name=statistics-enabled,value=true)`).

Пароль можно не хранить открытым текстом в `standalone.xml`, а положить в хранилище Elytron (`credential-store`)
и указать в DataSource через `--credential-reference`.

## 4. Развёртывание

```
deploy build/libs/study-groups.war
```
- `WEB-INF/jboss-web.xml` публикует приложение в корне (`/`), поэтому фронтенд обращается к `/api` и `/ws` без префикса.
- `WEB-INF/jboss-deployment-structure.xml` отключает JPA-подсистему WildFly: JPA (EclipseLink) поднимает Spring.
- При старте Flyway сам создаёт/обновляет схему через тот же DataSource.

Открыть: `http://localhost:<8080 + N>/` (с ноутбука — через `ssh -L`).

## Другие варианты

- **Tomcat 10.1+.** DataSource в `conf/context.xml`:
  ```xml
  <Resource name="jdbc/StudyGroupsDS" auth="Container" type="javax.sql.DataSource"
            driverClassName="org.postgresql.Driver" url="jdbc:postgresql://pg:5432/studs"
            username="sXXXXXX" password="********"
            maxTotal="10" maxIdle="5" minIdle="2" maxWaitMillis="5000"
            validationQuery="SELECT 1" testOnBorrow="true"/>
  ```
  драйвер — в `lib/`, war переименовать в `ROOT.war`, при запуске задать JNDI-имя:
  `-DDATASOURCE_JNDI=java:comp/env/jdbc/StudyGroupsDS`.
- **Локально без сервера:** `./gradlew bootTestRun` — встроенный PostgreSQL, профиль `dev` (не JNDI).
