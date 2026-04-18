package org.jsndb.qwery;

/**
 * Enumeration of available comparison operators for database queries.
 */
public enum comparators {
	/** Checks if the value is equal to the target. */
	equal,
	/** Checks if the value is strictly greater than the target. */
	greatter,
	/** Checks if the value is strictly smaller than the target. */
	smaller,
	/** Checks if the value is equal to or smaller than the target. */
	esmaller,
	/** Checks if the value is equal to or greater than the target. */
	egreatter,
	/** Performs a string prefix match (starts with). */
	like,
	/** Checks if a string contains another string or if a collection contains a value. */
	contains,
	/** Negation / inequality operator. */
	not
}
