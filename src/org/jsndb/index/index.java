package org.jsndb.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jsndb.jsndb;
import org.jsndb.beans.classtool;
import org.jsndb.kore.dataBase;
import org.jsndb.kore.enums.crud;
import org.jsndb.serializer.parser;
import org.jsndb.util.ListParted;
import org.jsndb.util.entryM;

/**
 * Standard implementation of a database index.
 * Uses a sorted list for binary search and a keyword map for "like" queries.
 */
public abstract class index extends superindex {

	/** Ordered list of object IDs in the index. */
	private ListParted<Long> list = new ListParted<>();
	/** Parallel list of indexed property values (same order as list). */
	private ListParted<Object> values = new ListParted<>();
	/** Map for keyword-based search: word -> set of object IDs. */
	private HashMap<String, HashSet<Long>> words;
	/** Set of IDs that need their words updated in the keyword map. */
	private HashSet<Long> wordsReindex;

	/** Number of fragmented/deleted entries in the current session. */
	int fragments;

	/** Internal enum to specify search position strategy. */
	private enum position {
		/** Find the first occurrence of a value. */
		first, 
		/** Find the last occurrence of a value. */
		last, 
		/** Find any occurrence. */
		any
	}

	/** Default constructor. */
	public index() {
	}

	/**
	 * Factory method to create and initialize a new index.
	 * @param dbase database instance.
	 * @param prop property name.
	 * @param ixcode unique index code.
	 * @param clsscode class code.
	 * @return initialized index instance.
	 */
	public static <T, C> index create(dataBase dbase, String prop, short ixcode, short clsscode) {
		index idx = new index() {
		};
		idx.setDb(dbase);
		idx.setPname(prop);
		idx.load(dbase, ixcode, clsscode);
		return idx;
	}

	@Override
	protected void add(Long id, ArrayList<Long> childs, int order, Object value) {
		if (order < 0) {
	/*		if (hasParent(id))
				return;*/
			if (list.get(-(order + 1)).equals(id)) {
				list.remove(-(order + 1));
				values.remove(-(order + 1));
				fragments++;
				return;
			}
			throw new UnknownError();
		} else {
			list.add(order, id);
			values.add(order, value);
		}
	}

	/**
	 * Gets an unmodifiable view of the indexed IDs.
	 * @return unmodifiable list of Long IDs.
	 */
	public List<Long> getList() {
		return Collections.unmodifiableList(list);
	}

	@Override
	public void change(Long id, crud c, HashMap<String, Object> o, ArrayList<Long> extListIds,
			HashMap<Long, HashMap<String, Object>> cache) {
		boolean del = false;
		switch (c) {
		case delete:
			del = true;
		case create:
		case update:
			if (list.contains(id)) {
				int rpos = ((ListParted<Long>) list).removeI(id);
				if (rpos >= 0 && rpos < values.size()) {
					values.remove(rpos);
				}
				write(id, -(rpos + 1), null, null);
				setChange(true);
			}
			getDb().removeFromGlobalCache(id);
			if (wordsReindex != null)
				wordsReindex.add(id);
			if (del)
				break;
			int pos = firstIndex(o.get(getPname()), cache);
			if (pos < 0)
				pos = -(pos + 1);
			entryM<Long, Long> enm = getDb().getObjIds().getDataOff().getEntry(id);
			Long oid;
			if (enm != null)
				oid = enm.getKey();
			else
				oid = id;
			Object indexedValue = o.get(getPname());
			write(id, pos, null, indexedValue);
			setChange(true);
			list.add(pos, oid);
			values.add(pos, indexedValue);
			cache.put(oid, o);
			getDb().putInGlobalCache(oid, o);
			break;
		default:
			break;
		}
	}

	/** Global counter for getPos calls. */
	public static int count = 0;

	long temptime;
	/** Cumulative time for position lookups. */
	public static long postime;

	/**
	 * Performs a binary search to find the position (or insertion point) of a value.
	 * @param value value to find.
	 * @param type search strategy (first, last, any).
	 * @param cache object data cache.
	 * @return index position or -(insertion_point + 1) if not found.
	 */
	@SuppressWarnings("unchecked")
	private <T, C> int getPos(C value, position type, HashMap<Long, HashMap<String, Object>> cache) {
		temptime = System.nanoTime();
		count++;
		if (list.size() == 0)
			return -1;
		int offset = -1;
		int low = 0, high = list.size();
		int mid = 0, res;
		Comparator<C> comp = (Comparator<C>) classtool.getComparator(classtool.getComparable(value));
		while (low < high) {
			mid = (low + high) / 2;
			Object midVal = values.get(mid);
			if (midVal == null) {
				// Fallback: value not in index (old format), load from object
				midVal = loadObject(cache, list.get(mid)).get(getPname());
			}
			res = comp.compare((C) midVal, value);
			if (res < 0)
				low = mid + 1;
			else if (res > 0)
				high = mid;
			else {
				switch (type) {
				case first:
					offset = high = mid;
					continue;
				case last:
				default:
					offset = low = mid;
					low++;
				}
			}
		}
		if (offset < 0)
			offset = -++high;
		postime += System.nanoTime() - temptime;
		return offset;
	}

	/** Global counter for lookups. */
	public static int bscount;

	/** Global counter for object loads from data file. */
	public static int loadcount;

	/**
	 * Internal helper to load an object map from cache or disk.
	 * @param cache current session cache.
	 * @param id object ID.
	 * @return map of object properties.
	 */
	private HashMap<String, Object> loadObject(HashMap<Long, HashMap<String, Object>> cache, Long id) {
		loadcount++;
		HashMap<String, Object> objson;
		if (cache.containsKey(id)) {
			objson = cache.get(id);
		} else {
			objson = getDb().getFromGlobalCache(id);
			if (objson == null) {
				String js = getDb().getDatafile().read(getDb().getObjIds().getDataOff().get(id), getClazzcode());
				objson = parser.jsonToFlatMap(js);
				getDb().putInGlobalCache(id, objson);
			}
			cache.put(id, objson);
		}
		return objson;
	}

	/** Find any occurrence of the value. */
	public int getIndex(Object value, HashMap<Long, HashMap<String, Object>> cache) {
		return getPos(value, position.any, cache);
	}

	/** Find the first occurrence of the value. */
	public int firstIndex(Object value, HashMap<Long, HashMap<String, Object>> cache) {
		return getPos(value, position.first, cache);
	}

	/** Find the last occurrence of the value. */
	public int lastIndex(Object value, HashMap<Long, HashMap<String, Object>> cache) {
		return getPos(value, position.last, cache);
	}

	/**
	 * Pre-filters IDs that start with the given value string.
	 * @param value value to pre-match.
	 * @param cache object data cache.
	 * @return set of matching IDs.
	 */
	public HashSet<Long> preLike(String value, HashMap<Long, HashMap<String, Object>> cache) {
		HashSet<Long> l = new HashSet<Long>();
		HashMap<String, HashSet<Long>> map = getWords(cache);
		for (String s : map.keySet())
			if (s.startsWith(value))
				l.addAll(map.get(s));
		return l;
	}

	/**
	 * Performs a full check of provided IDs to see if their indexed property contains the value.
	 * @param value value to search for.
	 * @param cache object data cache.
	 * @param idscheck list of candidate IDs.
	 * @return set of IDs that actually contain the value.
	 */
	public HashSet<Long> getLike(String value, HashMap<Long, HashMap<String, Object>> cache, HashSet<Long> idscheck) {
		HashSet<Long> ids = new HashSet<>();
		String propval;
		for (Long oid : idscheck) {
			if (cache.get(oid) != null)
				propval = (String) cache.get(oid).get(getPname());
			else {
				String js = getDb().getDatafile().read(getDb().getObjIds().getDataOff().get(oid), getClazzcode());
				HashMap<String, Object> map = parser.jsonToFlatMap(js);
				propval = (String) map.get(getPname());
			}
			if (propval.contains(value))
				ids.add(oid);
		}
		return ids;
	}

	/** Retrieves the keyword map, lazily building it if necessary. */
	private HashMap<String, HashSet<Long>> getWords(HashMap<Long, HashMap<String, Object>> cache) {
		if (words == null) {
			words = new HashMap<>();
			wordsReindex = new HashSet<>();
			for (int n = 0; n < list.size(); n++)
				wordsReindex.add(list.get(n));

		}
		if (!wordsReindex.isEmpty()) {
			for (HashSet<Long> hs : words.values()) {
				hs.removeAll(wordsReindex);
			}
			for (Long oid : wordsReindex) {
				if (oid == null)
					throw new NullPointerException();
				String propval;
				if (cache.get(oid) != null)
					propval = (String) cache.get(oid).get(getPname());
				else {
					String js = getDb().getDatafile().read(getDb().getObjIds().getDataOff().get(oid), getClazzcode());
					HashMap<String, Object> map = parser.jsonToFlatMap(js);
					propval = (String) map.get(getPname());
				}
				if (propval == null || propval.isEmpty())
					continue;
				changelike(propval, oid);
			}
			wordsReindex.clear();
		}
		return words;
	}

	/** Splts a property string into keywords and adds them to the word map. */
	private void changelike(String prop, Long id) {
		for (String s : prop.split(jsndb.LIKE_SPLIT)) {
			s = s.toUpperCase();
			if (s.isEmpty())
				continue;
			HashSet<Long> li = words.get(s);
			if (li == null)
				words.put(s, (li = new HashSet<>()));
			li.add(id);
		}
	}

	@Override
	protected ArrayList<blockidx> getBlokcs() {
		ArrayList<blockidx> blks = new ArrayList<blockidx>();
		for (int n = 0; n < list.size(); n++) {
			blockidx b = new blockidx();
			b.id = list.get(n);
			blks.add(b);
		}
		return blks;
	}

	@Override
	protected void clear() {
		list.clear();
		values.clear();
		fragments = 0;
	}

	@Override
	protected boolean isFragmented() {
		if (fragments > 1000)
			return fragments / (double) list.size() > 0.1;
		return false;
	}
}