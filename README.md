<div align="center">
  <img src="logo.png" width="150" alt="JSNDB Logo"/>
  <h1>JSNDB</h1>
  <p><b>JavaScript Object Notation Database</b></p>
</div>

![JSNDB Banner](banner.png)

> **⚠️ MIGRATION NOTICE**
> The official repository for JSNDB has moved from [SourceForge](https://sourceforge.net/projects/jsdbase/) to GitHub! This is now the main repository for future development, issues, and releases.

JSNDB is a **fully embedded, 100% serverless NoSQL Document Database** written purely in Java. It allows you to persist, query, update, and delete Java objects (POJOs) natively to disk using a lightweight JSON structure combined with an extremely optimized binary B-Tree indexing engine.

It eliminates the need for strict schemas, ORMs, and heavy connection protocols by working directly with your memory objects.

## Key Features
- **No Dependencies, No Servers:** Runs inside your JVM.
- **POJO Native:** Save your Java Objects directly. No mappers or DTO conversions needed.
- **Blazing Fast Binary Indices:** Properties are mapped into a binary B-Tree that avoids parsing JSON during operations.
- **Circular Reference Support:** Handles cyclic relationships (e.g. `Owner -> Pet -> Owner`) gracefully without StackOverflow crashes, using lazy loading proxies.
- **Modern & Lightweight:** Compatible with Java 17 / Java 21 LTS.

---

## 🚀 Quick Start Example

### 1. Define your POJOs
Just use your normal Java objects. Use `@jsndbObjectId` to mark the ID.

```java
public class Persona {
    @jsndbObjectId
    private Long id;
    private String name;
    private int age;
    private List<Animal> pets;
    // constructors, getters, setters...
}
```

### 2. Initialize and Persist
```java
// Open or create the database directory
jsndb db = new jsndb("my_database_dir");

// Create your complex nested objects
Persona user = new Persona("Alice", 28, new ArrayList<>());
Animal dog = new Animal("Buddy", "Golden Retriever", user);
user.getPets().add(dog);

// Persist the objects natively (JSNDB handles the relations automatically!)
db.persist(dog);
db.persist(user);
db.commit(); // Writes gracefully to disk
```

### 3. Query the Database
```java
// Builder for searching
qwery q = qwery.create(Persona.class, "Alice", "name", comparators.equal);

List<Persona> results = db.select(Persona.class, q);
for (Persona p : results) {
    System.out.println("Found " + p.getName() + " with " + p.getPets().size() + " pets!");
}
```

---

## ⚡ Performance Benchmarks

JSNDB was heavily optimized and benchmarked directly against industry leviathans: **SQLite** (C-based relational DB) and **Nitrite** (Java embedded NoSQL DB).

The test involved inserting, selecting, and modifying **400,000 extreme relational objects** (`100,000 Persons` each containing `3 nested Pets`).

### Results (100k Complex Entities)

| Operation | JSNDB (NoSQL) | Nitrite Database (NoSQL) | SQLite (RDBMS) |
| :--- | :--- | :--- | :--- |
| **Insert** | **4.566 ms** | 10.259 ms | *2.073 ms* |
| **Select (Indexed)** | **2.165 ms** | 7.683 ms | *369 ms* |
| **Update (Bulk)** | **13 ms 🔥** | 10.436 ms | 104 ms |
| **Delete (Bulk)** | **0 ms (Instant) 🔥** | 12.790 ms | 13 ms |

### Key Takeaway
- **Obliterates NoSQL Competition:** JSNDB performs inserts **2.2x faster** and selects **3.5x faster** than Nitrite Database. 
- **Bulk Dominance:** In mass modifications (Updates and Deletes), JSNDB resolves logical operations seamlessly in under 15 milliseconds, beating even the raw C engine of SQLite (which took ~100ms) and destroying Nitrite which took more than 10-12 seconds.
- **Unhackable Circularity:** Nitrite crashed (`StackOverflowError`) during the test due to cyclic POJO references via Jackson. JSNDB's `lazyload` proxy matrix resolved the references natively without requiring dirty `@JsonIgnore` decorators.

---

## Download (Beta 1)
Download the `jsndb-beta1.jar` from the release section to try it directly in your project.

### Compiling manually
```bash
javac -d bin src/**/*.java
jar cvf jsndb-beta1.jar -C bin/ .
```
