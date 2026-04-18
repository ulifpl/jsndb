package org.jsndb.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.jsndb.annotations.jsndbObjectId;
import org.jsndb.jsndb;
import org.jsndb.qwery.comparators;
import org.jsndb.qwery.connector;
import org.jsndb.qwery.qwery;

/**
 * Automated test suite for the jsndb database engine.
 * This class verifies CRUD operations, query engine, index integrity, and complex object relationships.
 */
public class test {
	private static jsndb js;
	// Path for temporary database files, using a unique suffix to avoid collisions
	private static String dbPath;
	private static int testsPassed = 0;
	private static int totalTests = 0;

	public static void main(String[] args) {
		try {
			System.out.println("=== Starting jsndb Automated Test Suite ===\n");
			
			runTest("Basic CRUD", test::testBasicCRUD);
			runTest("Query Engine", test::testQueryEngine);
			runTest("Circular References", test::testCircularReferences);
			runTest("Data Consistency & Performance", test::testConsistency);
			
			System.out.println("\n=== Test Suite Finished ===");
			System.out.println("Tests Passed: " + testsPassed + "/" + totalTests);
			
			if (testsPassed < totalTests) {
				System.exit(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			cleanup();
		}
	}

	private static void runTest(String name, Runnable testTask) {
		System.out.print("Running [" + name + "]... ");
		totalTests++;
		try {
			setup();
			testTask.run();
			System.out.println("PASS");
			testsPassed++;
		} catch (AssertionError e) {
			System.out.println("FAIL");
			System.out.println("  Reason: " + e.getMessage());
		} catch (Exception e) {
			System.out.println("ERROR");
			System.out.println("  Exception: " + e.toString());
			e.printStackTrace();
		} finally {
			teardown();
		}
	}

	private static void setup() {
		dbPath = System.getProperty("java.io.tmpdir") + File.separator + "jsndb_test_" + System.currentTimeMillis() + "_" + (new Random().nextInt(1000));
		js = new jsndb(dbPath);
	}

	private static void teardown() {
		if (js != null) {
			try {
				js.close();
			} catch (Exception e) {
				System.err.println("Warning: Error closing database: " + e.getMessage());
			}
			js = null;
		}
	}

	private static void cleanup() {
		if (dbPath == null) return;
		File dbDir = new File(dbPath);
		if (!dbDir.exists()) return;
		
		for (int i = 0; i < 5; i++) {
			deleteDirectory(dbDir);
			if (!dbDir.exists()) return;
			try { Thread.sleep(100); } catch (InterruptedException e) {}
		}
		System.err.println("Warning: Could not fully cleanup directory " + dbPath);
	}

	private static void deleteDirectory(File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					deleteDirectory(f);
				} else {
					f.delete();
				}
			}
		}
		dir.delete();
	}

	private static void testBasicCRUD() {
		persona p = new persona("Test User", 25, new ArrayList<>(), 12345L);
		js.persist(p).commit();
		
		Long id = p.getPersonaId();
		assertNotNull(id, "ID should be assigned after persist/commit");
		
		persona retrieved = js.getById(id, persona.class);
		assertNotNull(retrieved, "Should retrieve persona by ID");
		assertEquals("Test User", retrieved.getName(), "Name mismatch");
		
		retrieved.setName("Updated User");
		js.persist(retrieved).commit();
		
		persona updated = js.getById(id, persona.class);
		assertEquals("Updated User", updated.getName(), "Update failed");
		
		js.delete(updated).commit();
		persona deleted = js.getById(id, persona.class);
		assertNull(deleted, "Persona should be deleted");
	}

	private static void testQueryEngine() {
		js.persist(new persona("Alice", 20, new ArrayList<>(), 1L))
		  .persist(new persona("Bob", 30, new ArrayList<>(), 2L))
		  .persist(new persona("Charlie", 40, new ArrayList<>(), 3L))
		  .commit();
		
		qwery q1 = qwery.create(persona.class, 25, "edad", comparators.greatter);
		List<persona> results1 = js.select(persona.class, q1);
		assertEquals(2, results1.size(), "Should find 2 people older than 25");
		
		qwery q2 = qwery.create(persona.class, "Ali", "name", comparators.like);
		List<persona> results2 = js.select(persona.class, q2);
		assertEquals(1, results2.size(), "Should find 1 person named Alice via LIKE");
	}

	private static void testCircularReferences() {
		persona p = new persona("Owner", 30, new ArrayList<>(), 1L);
		animal a = new animal("Buddy", "Dog", p);
		p.getMascota().add(a);
		
		js.persist(p).persist(a).commit();
		
		System.out.println("  Debug: Owner ID=" + p.getPersonaId() + ", Animal ID=" + a.getAnimalId());
		
		qwery q = qwery.create(persona.class, "Owner", "name", comparators.equal);
		List<persona> results = js.select(persona.class, q);
		
		assertEquals(1, results.size(), "Owner not found after circular save");
		persona retrievedP = results.get(0);
		assertNotNull(retrievedP.getMascota(), "Mascota list is null");
		assertEquals(1, retrievedP.getMascota().size(), "Mascota not linked");
		assertEquals("Buddy", retrievedP.getMascota().get(0).getNombre(), "Animal name mismatch");
		assertEquals(retrievedP.getPersonaId(), retrievedP.getMascota().get(0).getOwner().getPersonaId(), "Circular link broken");
	}

	private static void testConsistency() {
		int count = 1000;
		long start = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			persona p = new persona("Persona " + i, (i % 100), new ArrayList<>(), (long) i);
			js.persist(p);
		}
		js.commit();
		System.out.print("(Save time: " + (System.currentTimeMillis() - start) + "ms) ");
		
		qwery q = qwery.create(persona.class, 50, "edad", comparators.equal);
		List<persona> results = js.select(persona.class, q);
		assertEquals(10, results.size(), "Consistency check failed: found " + results.size() + " instead of 10");

		qwery qLike = qwery.create(persona.class, "Pers", "name", comparators.like);
		List<persona> resultsLike = js.select(persona.class, qLike);
		assertEquals(count, resultsLike.size(), "Updates should be detectable via LIKE query, found " + resultsLike.size());
	}

	private static void assertNotNull(Object obj, String msg) {
		if (obj == null) throw new AssertionError(msg);
	}

	private static void assertNull(Object obj, String msg) {
		if (obj != null) throw new AssertionError(msg);
	}

	private static void assertEquals(Object expected, Object actual, String msg) {
		if (expected == null && actual == null) return;
		if (expected != null && expected.equals(actual)) return;
		throw new AssertionError(msg + " (Expected: " + expected + ", Actual: " + actual + ")");
	}
}
