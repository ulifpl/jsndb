package org.jsndb.util;

/**
 * Simple internal logger for the database engine.
 * Controls console output based on a global flag.
 */
public class logger {
	/** Global log enablement flag. */
	private static boolean log;

	/**
	 * Prints a line to standard output if logging is enabled.
	 * @param s string to log.
	 */
	public static void line(String s) {
		if(log)
		System.out.println(s);
	}
}
