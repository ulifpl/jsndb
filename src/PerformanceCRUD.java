import org.jsndb.jsndb;
import org.jsndb.test.persona;
import org.jsndb.test.animal;
import org.jsndb.qwery.qwery;
import org.jsndb.qwery.comparators;
import org.jsndb.kore.enums.crud;
import java.io.File;
import java.sql.*;
import java.util.*;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.objects.ObjectRepository;
import org.dizitart.no2.objects.filters.ObjectFilters;
import org.dizitart.no2.IndexOptions;
import org.dizitart.no2.IndexType;

public class PerformanceCRUD {
    private static final int COUNT = 100000;
    private static final String SQLITE_DB = "benchmark_sqlite.db";
    private static final String JSNDB_DIR = "benchmark_jsndb";
    private static final String NITRITE_DB = "benchmark_nitrite.db";

    public static void main(String[] args) throws Exception {
        System.out.println("=== JSNDB vs SQLite vs Nitrite Benchmark (" + COUNT + " items) ===");
        
        cleanup();
        
        // --- JSNDB ---
        runJSNDB();
        System.out.println("Cache Hits: " + org.jsndb.kore.dataBase.hitCount + "  Misses: " + org.jsndb.kore.dataBase.missCount);
        
        // --- SQLite ---
        runSQLite();
        
        // --- Nitrite ---
        runNitrite();
        
        System.out.println("\n=== Final Summary (ms) ===");
        System.out.printf("%-15s | %-10s | %-10s | %-10s\n", "Operation", "JSNDB", "SQLite", "Nitrite");
        System.out.println("-------------------------------------------------------");
        
        System.out.println("\n=== File Sizes ===");
        reportSizes();
    }

    private static void reportSizes() {
        System.out.println("JSNDB (Dir total): " + getDirSize(new File(JSNDB_DIR)) / 1024 + " KB");
        System.out.println("SQLite (File): " + new File(SQLITE_DB).length() / 1024 + " KB");
        System.out.println("Nitrite (File): " + new File(NITRITE_DB).length() / 1024 + " KB");
    }

    private static long getDirSize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) size += f.length();
                else size += getDirSize(f);
            }
        }
        return size;
    }

    private static void cleanup() {
        new File(SQLITE_DB).delete();
        new File(NITRITE_DB).delete();
        deleteDir(new File(JSNDB_DIR));
    }

    private static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) deleteDir(f);
        }
        file.delete();
    }

    private static void runJSNDB() throws Exception {
        System.out.println("\n[Testing JSNDB]");
        jsndb js = new jsndb(JSNDB_DIR);
        
        // Insert
        long start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
            persona p = new persona("User " + i, i, new ArrayList<>(), System.currentTimeMillis());
            for (int k=0; k < 3; k++) {
                animal a = new animal("Pet " + k + "-" + i, "Raza " + k, p);
                p.getMascota().add(a);
                js.persist(a);
            }
            js.persist(p);
        }
        js.commit();
        long insertTime = System.currentTimeMillis() - start;
        System.out.println("Insert: " + insertTime + "ms");

        // Select (Indexed)
        start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
            qwery q = qwery.create(persona.class, "User " + i, "name", comparators.equal);
            js.select(persona.class, q);
        }
        long selectTime = System.currentTimeMillis() - start;
        System.out.println("Select (Indexed): " + selectTime + "ms");

        // Update
        start = System.currentTimeMillis();
        qwery all = qwery.create(persona.class, "", "name", comparators.like); // Simulate all
        List<persona> results = js.select(persona.class, all);
        for (persona p : results) {
            p.setEdad(p.getEdad() + 1);
            js.persist(p);
        }
        js.commit();
        long updateTime = System.currentTimeMillis() - start;
        System.out.println("Update: " + updateTime + "ms");

        // Delete
        start = System.currentTimeMillis();
        results = js.select(persona.class, all);
        for (persona p : results) {
            js.delete(p);
        }
        js.commit();
        long deleteTime = System.currentTimeMillis() - start;
        System.out.println("Delete: " + deleteTime + "ms");
        
        js.close();
    }

    private static long runSQLite() throws Exception {
        System.out.println("\n[Testing SQLite]");
        Class.forName("org.sqlite.JDBC");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + SQLITE_DB);
        Statement st = conn.createStatement();
        st.execute("CREATE TABLE persona (id INTEGER PRIMARY KEY, name TEXT, age INTEGER, fecha INTEGER)");
        st.execute("CREATE TABLE animal (id INTEGER PRIMARY KEY, nombre TEXT, raza TEXT, owner_id INTEGER)");
        st.execute("CREATE INDEX idx_name ON persona(name)");
        st.execute("CREATE INDEX idx_owner ON animal(owner_id)");
        
        // Insert
        conn.setAutoCommit(false);
        long start = System.currentTimeMillis();
        PreparedStatement ps = conn.prepareStatement("INSERT INTO persona (id, name, age, fecha) VALUES (?, ?, ?, ?)");
        PreparedStatement psAnimal = conn.prepareStatement("INSERT INTO animal (nombre, raza, owner_id) VALUES (?, ?, ?)");
        for (int i = 0; i < COUNT; i++) {
            ps.setInt(1, i);
            ps.setString(2, "User " + i);
            ps.setInt(3, i);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
            
            for (int k=0; k < 3; k++) {
                psAnimal.setString(1, "Pet " + k + "-" + i);
                psAnimal.setString(2, "Raza " + k);
                psAnimal.setInt(3, i);
                psAnimal.executeUpdate();
            }
        }
        conn.commit();
        long insertTime = System.currentTimeMillis() - start;
        System.out.println("Insert: " + insertTime + "ms");

        // Select (Indexed)
        start = System.currentTimeMillis();
        PreparedStatement selPs = conn.prepareStatement("SELECT * FROM persona WHERE name = ?");
        for (int i = 0; i < COUNT; i++) {
            selPs.setString(1, "User " + i);
            ResultSet rs = selPs.executeQuery();
            while(rs.next()) { /* simulate reading */ }
            rs.close();
        }
        long selectTime = System.currentTimeMillis() - start;
        System.out.println("Select (Indexed): " + selectTime + "ms");

        // Update
        start = System.currentTimeMillis();
        st.execute("UPDATE persona SET age = age + 1");
        conn.commit();
        long updateTime = System.currentTimeMillis() - start;
        System.out.println("Update: " + updateTime + "ms");

        // Delete
        start = System.currentTimeMillis();
        st.execute("DELETE FROM persona");
        conn.commit();
        long deleteTime = System.currentTimeMillis() - start;
        System.out.println("Delete: " + deleteTime + "ms");
        
        conn.close();
        return 0;
    }

    private static void runNitrite() {
        System.out.println("\n[Testing Nitrite]");
        Nitrite db = Nitrite.builder().compressed().filePath(NITRITE_DB).openOrCreate();
        ObjectRepository<persona> repoPersona = db.getRepository(persona.class);
        ObjectRepository<animal> repoAnimal = db.getRepository(animal.class);
        
        repoPersona.createIndex("name", IndexOptions.indexOptions(IndexType.NonUnique));
        
        long idCounter = 1;
        long start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
            persona p = new persona("User " + i, i, new ArrayList<>(), System.currentTimeMillis());
            p.setPersonaId(idCounter++);
            for (int k = 0; k < 3; k++) {
                animal a = new animal("Pet " + k + "-" + i, "Raza " + k, p);
                a.setAnimalId(idCounter++);
                p.getMascota().add(a);
                repoAnimal.insert(a);
            }
            repoPersona.insert(p);
        }
        db.commit();
        long insertTime = System.currentTimeMillis() - start;
        System.out.println("Insert: " + insertTime + "ms");

        start = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
            org.dizitart.no2.objects.Cursor<persona> cursor = repoPersona.find(ObjectFilters.eq("name", "User " + i));
            for (persona p : cursor) { /* simulate reading */ }
        }
        long selectTime = System.currentTimeMillis() - start;
        System.out.println("Select (Indexed): " + selectTime + "ms");

        start = System.currentTimeMillis();
        org.dizitart.no2.objects.Cursor<persona> all = repoPersona.find(ObjectFilters.regex("name", ".*"));
        for (persona p : all) {
            p.setEdad(p.getEdad() + 1);
            repoPersona.update(p);
        }
        db.commit();
        long updateTime = System.currentTimeMillis() - start;
        System.out.println("Update: " + updateTime + "ms");

        start = System.currentTimeMillis();
        repoPersona.remove(ObjectFilters.regex("name", ".*"));
        db.commit();
        long deleteTime = System.currentTimeMillis() - start;
        System.out.println("Delete: " + deleteTime + "ms");
        
        db.close();
    }
}
