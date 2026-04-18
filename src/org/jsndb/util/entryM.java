package org.jsndb.util;

/**
 * Generic entry class that pairs a comparable key with a value.
 * Implements Comparable for key-based comparisons.
 * @param <T> type of the key, must be Comparable.
 * @param <V> type of the value.
 */
public class entryM<T extends Comparable<T>, V> implements Comparable<T> {
	/** Comparable key of the entry. */
	private final T key;
	/** Value associated with the key. */
	private final V value;

	/**
	 * Creates a new entry.
	 * @param l key.
	 * @param p value.
	 */
	public entryM(T l, V p) {
		key = l;
		value = p;
	}

	/**
	 * Gets the key of the entry.
	 * @return key.
	 */
	public T getKey() {
		return key;
	}

//	public void setKey(T id) {
//		this.key = id;
//	}

	/**
	 * Gets the value of the entry.
	 * @return value.
	 */
	public V getValue() {
		return value;
	}

//	public void setValue(V fathers) {
//		this.value = fathers;
//	}

	@Override
	public int compareTo(T o) {
		return getKey().compareTo(o);
	}

}
