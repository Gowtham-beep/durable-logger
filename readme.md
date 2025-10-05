

## 🧾 **README.md (Release Version)**

````markdown
# 🪶 Durable Logger — A Java At-Least-Once Persistent Logging Library

> “Started messy, stayed curious, built durable.”

**Durable Logger** is a lightweight, modular, fault-tolerant logging library built in pure Java.  
It guarantees **at-least-once log persistence** even during application crashes — using an internal **Write-Ahead Log (WAL)** and background flush mechanism.

This project was built from scratch to explore **systems-level durability**, **concurrency**, and **developer-friendly design** — now released as **Phase 1** (MVP).

---

## 🚀 Features

✅ **Write-Ahead Log (WAL)** — crash-safe persistence.  
✅ **At-Least-Once Delivery** — replay unflushed logs after restart.  
✅ **Pluggable Storage Adapters** — File (✅), PostgreSQL, Kafka, S3 (coming soon).  
✅ **Query API** — filter logs by time, level, or message text.  
✅ **Auto-Rotation + Retention** — rotate logs by size or age.  
✅ **YAML / Builder Configuration** — flexible initialization.  
✅ **Minimal Dependencies, High Performance.**

---

## ⚡️ Quick Start

### 🧩 Add Dependency

```xml
<dependency>
  <groupId>com.yourorg.logging</groupId>
  <artifactId>core-logger</artifactId>
  <version>1.0.0</version>
</dependency>
````

Add the GitHub Packages repository to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/your-github-username/durable-logger</url>
  </repository>
</repositories>
```

---

### ⚙️ Initialize the Logger

```java
DurableLoggerFactory.init("src/main/resources/logger.yml");
Logger logger = DurableLoggerFactory.getLogger(MyService.class);

logger.info("Service started successfully");
logger.warn("Low memory warning");
logger.error("Unexpected error", new RuntimeException("test failure"));
```

---

### 🔍 Query Your Logs

```java
QueryRequest req = new QueryRequest(
    Instant.now().minusSeconds(600),
    Instant.now(),
    Optional.of(LogLevel.ERROR),
    "exception",
    200
);

QueryResult result = DurableLoggerFactory.durableQuery(req);
System.out.println("Found " + result.getEntries().size() + " error logs");
```

---

## 🧾 Sample Configuration (`logger.yml`)

```yaml
serviceName: "durable-logger-demo"
level: INFO

queueCapacity: 10000
maxBatchSize: 500
maxBatchMillis: 200
fsyncOnWalAppend: true

wal:
  path: "data/wal.log"
  checkpoint: "data/wal.check"

storage:
  type: file
  file:
    path: "data/store.log"

retention:
  rotateSizeMB: 100
  maxDays: 7
```

---

## 🧪 Testing & Verification

```bash
mvn clean test
```

Tests include:

* WAL replay verification
* File rotation and retention
* Query across rotated logs

✅ All tests passing — durable writes and replays confirmed.

---

## ❤️ A Note from the Author

This project started as a small experiment while I was still figuring out Java internals —
I wanted to understand how reliability systems like databases and Kafka guarantee durability.
It’s far from perfect, but it works — and it’s just the beginning.

If this inspires you or helps you build something reliable,
please ⭐ **star the repo** and share your thoughts — it means a lot. 🙏

---

**Author:** Gowtham N
**License:** MIT
**Repository:** [https://github.com/Gowtham-beep/durable-logger.git](https://github.com/Gowtham-beep/durable-logger.git)

---