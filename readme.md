
```markdown
# Durable Logger (MVP)

A lightweight, durable logging library for Java that guarantees **at-least-once delivery** of log events.  
It uses a **Write-Ahead Log (WAL)** with checkpoints to survive crashes and provides a simple query API.

---

## ✨ Features (MVP)

- **Log API** with levels: `INFO`, `WARN`, `ERROR`, `DEBUG`
- **Durability**: logs are first written to a WAL before flushing to the final store
- **Crash recovery**: unflushed logs are replayed on restart
- **Checkpointing**: tracks last successfully flushed log
- **File-based storage** (`store.log`) using JSON lines
- **Queryable**: search logs by time range, text, and log level
- **At-least-once delivery** semantics
- **Simple API** via `LogManager.getLogger(Class<?>)`

---

## 📂 Project Structure

```



---

## 🚀 Usage

### 1. Initialize logger
```java
File wal = new File("data/wal.log");
File checkpoint = new File("data/wal.check");
File store = new File("data/store.log");

var adapter = new FileStorageAdapter(store);
var durable = new DurableLogger(adapter, wal, checkpoint,
        10000,   // queue capacity
        true,    // fsync on WAL append
        500,     // max batch size
        200);    // max batch millis

LogManager.init(adapter, durable);
Logger logger = LogManager.get().getLogger(MyService.class);
````

### 2. Write logs

```java
logger.info("Service started");
logger.warn("This is a warning");
logger.error("Something failed", new RuntimeException("oops"));
```

### 3. Query logs

```java
QueryRequest req = new QueryRequest(
        Instant.now().minusSeconds(300),
        Instant.now(),
        Optional.of(LogLevel.ERROR), // filter by level
        null,                        // no text filter
        200                          // limit
);

QueryResult result = adapter.query(req);
result.getEntries().forEach(e ->
    System.out.println(Instant.ofEpochMilli(e.getTimestamp())
            + " [" + e.getLevel() + "] " + e.getMessage())
);
```

---

## 🧪 Demo

Run the demo `Main` class:

```bash
# normal run (writes + queries)
mvn clean compile exec:java -Dexec.mainClass="com.yourorg.logging.Main"

# query-only run
mvn -q compile exec:java -Dexec.mainClass="com.yourorg.logging.Main" -Dexec.args="query"
```

Artifacts produced:

* `data/wal.log` → Write-Ahead Log (binary)
* `data/wal.check` → Checkpoint file
* `data/store.log` → Final durable log store (JSON lines)

---

## 🔮 Future Enhancements

Some features we plan to add:

* **Log rotation** (e.g., roll logs after 100MB or 2GB)
* **Retention policies** (delete/archive logs older than X days)
* **Multiple adapters** (PostgreSQL, Elasticsearch, Kafka)
* **Cross-service tracing** (traceId propagation across microservices)
* **Middleware integration** (HTTP request/response logging, e.g. Spring Boot filter)
* **Better query engine**: pagination, advanced filters, full-text search
* **Performance tuning**: async I/O, memory-mapped WAL, backpressure policies
* **Monitoring hooks**: metrics (queue depth, flush latency)

---

## ⚡ At-least-once Guarantee

* Logs are **always written to WAL before ack**.
* On crash, unflushed WAL entries are replayed on restart.
* Guarantees no loss, but duplicates are possible (consumer must be idempotent).

---

## 📜 License

MIT (for MVP demo). Adapt for production use with caution.


