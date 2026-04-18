package org.jsndb.lazyLoad;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Custom HashMap implementation for lazy loading.
 * Currently, the lazy loading logic for maps appears to be incomplete or a placeholder.
 * @param <K> key type.
 * @param <V> value type.
 */
public class lazyHashMap<K, V> extends HashMap<K, V> {
	private static final long serialVersionUID = 2140390373959511957L;
	/** List of object IDs associated with this map. */
	ArrayList<Long> ids;

	/**
	 * Creates a new lazy hash map.
	 * @param lzids list of object identifiers.
	 */
	public lazyHashMap(ArrayList<Long> lzids) {
		ids = lzids;
	}

	@Override
	public V get(Object key) {
		if (super.containsKey(key))
			return super.get(key);
		return super.get(key);
	}
}
