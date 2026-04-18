package org.jsndb.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A Map implementation that maintains entries in a sorted list.
 * Uses binary search for efficient lookups.
 * Underlying storage is a {@link partedList}.
 * @param <T> type of keys, must be Comparable.
 * @param <V> type of values.
 */
public class mapList<T extends Comparable<T>, V> implements Map<T, V> {
	/** Underlying sorted list of entries. */
	partedList<entryM<T, V>> listEntry = new partedList<>();

	@SuppressWarnings("unchecked")
	@Override
	public boolean containsKey(Object key) {
		if (Collections.binarySearch(listEntry, (T) key) < 0)
			return false;
		return true;
	}

	/**
	 * Returns the key at the specified index in the sorted list.
	 * @param n index.
	 * @return key at index.
	 */
	public T getKeyByIndex(int n) {
		return listEntry.get(n).getKey();
	}

	/** Not implemented. */
	@Override
	public boolean containsValue(Object value) {
		throw new UnknownError("method not implemented yet");
	}

	/** Not implemented. */
	@Override
	public Set<java.util.Map.Entry<T, V>> entrySet() {
		throw new UnknownError("method not implemented yet");
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		return getValue((T) key);
	}

	/**
	 * Retrieves the value associated with the given key.
	 * @param key target key.
	 * @return associated value or null if not found.
	 */
	public V getValue(T key) {
		int p = Collections.binarySearch(listEntry, key);
		if (p < 0)
			return null;
		if (listEntry.get(p).getValue() == null)
			throw new UnknownError("");
		return listEntry.get(p).getValue();
	}

	/**
	 * Retrieves the entry associated with the given key.
	 * @param key target key.
	 * @return associated entry or null if not found.
	 */
	public entryM<T, V> getEntry(T key) {
		int p = Collections.binarySearch(listEntry, key);
		if (p < 0)
			return null;
		return listEntry.get(p);
	}

	/** Not implemented. */
	@Override
	public Set<T> keySet() {
		throw new UnknownError("method not implemented yet");
	}

	@Override
	public V put(T key, V value) {
		int p = Collections.binarySearch(listEntry, key);
		entryM<T, V> em = new entryM<>(key, value);
		if (p < 0) {
			p = -(++p);
			listEntry.add(p, em);
			return null;
		}
		entryM<T, V> ep = listEntry.get(p);
		listEntry.set(p, em);
		return ep.getValue();
	}

	/** Not implemented. */
	@Override
	public void putAll(Map<? extends T, ? extends V> m) {
		throw new UnknownError("method not implemented yet");

	}

	/** Not implemented. */
	@Override
	public Collection<V> values() {
		throw new UnknownError("method not implemented yet");
	}

	@Override
	public void clear() {
		listEntry.clear();

	}

	@Override
	public boolean isEmpty() {
		return listEntry.isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		return removeKey((T) key);
	}

	/**
	 * Removes the key and its associated value from the map.
	 * @param key target key.
	 * @return removed value or null if not found.
	 */
	public V removeKey(T key) {
		int p = Collections.binarySearch(listEntry, key);
		if (p < 0)
			return null;
		return listEntry.remove(p).getValue();
	}

	@Override
	public int size() {
		return listEntry.size();
	}

	/**
	 * Returns the entry at the specified index in the sorted list.
	 * @param n index.
	 * @return entry at index.
	 */
	public entryM<T, V> getEnryByIndex(int n) {
		return listEntry.get(n);
	}
}
