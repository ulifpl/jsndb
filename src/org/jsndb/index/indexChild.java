package org.jsndb.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.jsndb.kore.dataBase;
import org.jsndb.kore.enums.crud;

/**
 * Implementation of an index for object relationships (collections or single references).
 * Maintains a bidirectional mapping between "parent" IDs and "child" IDs.
 */
public class indexChild extends superindex {

	/** Map of child ID to list of parent IDs. */
	private HashMap<Long, ArrayList<Long>> fathers = new HashMap<>();
	/** Map of parent ID to list of child IDs. */
	private HashMap<Long, ArrayList<Long>> childs = new HashMap<>();

	/**
	 * Factory method to create and initialize a new relational index.
	 * @param dbase database instance.
	 * @param prop property name.
	 * @param ixcode unique index code.
	 * @param clsscode class code.
	 * @return initialized indexChild instance.
	 */
	public static <T, C> indexChild create(dataBase dbase, String prop, short ixcode, short clsscode) {
		indexChild idx = new indexChild() {
		};
		idx.setDb(dbase);
		idx.setPname(prop);
		idx.load(dbase, ixcode, clsscode);
		return idx;
	}

	/**
	 * Gets the parent relationship map.
	 * @return map of child-to-parents.
	 */
	public HashMap<Long, ArrayList<Long>> getFathers() {
		return fathers;
	}

	/**
	 * Gets the child relationship map.
	 * @return map of parent-to-children.
	 */
	public HashMap<Long, ArrayList<Long>> getChilds() {
		return childs;
	}

	@Override
	protected void add(Long fid, ArrayList<Long> chs, int order, Object value) {
		if (order < 1) {
/*			if (hasParent(fid)) {
				if (order < 0)
				return;
			}*/
			delete(fid, chs);
			if (order < 0)
				return;
		}
		// chs.trimToSize();
		Collections.sort(chs);
		childs.put(fid, chs);
		// if (chsold != null) {
		// for (Long l : chsold) {
		// ArrayList<Long> f = fathers.get(l);
		// if (f == null || f.isEmpty())
		// continue;
		// int n = Collections.binarySearch(f, fid);
		// if (n > -1)
		// f.remove(n);
		// }
		// }
		for (Long l : chs) {
			ArrayList<Long> f = fathers.get(l);
			if (f == null)
				fathers.put(l, f = new ArrayList<>(1));
			int n = Collections.binarySearch(fathers.get(l), fid);
			if (n > -1)
				continue;
			f.add(-(++n), fid);
			// f.trimToSize();
		}
	}

	/** tracks deleted entries for fragmentation check. */
	int fragments = 0;

	/**
	 * Removes a parent and its relationships from the index.
	 * @param fid parent ID.
	 * @param chs optional list of children (not used for lookup).
	 */
	private void delete(Long fid, ArrayList<Long> chs) {
		ArrayList<Long> chd = childs.get(fid);
		if (chd == null)
			chd = new ArrayList<Long>();
		for (Long l : chd) {
			ArrayList<Long> f = fathers.get(l);
			if (f == null || f.isEmpty())
				continue;
			int n = Collections.binarySearch(f, fid);
			if (n > -1)
				f.remove(n);
			if(f.isEmpty())
				fathers.remove(l);
		}
		fragments++;
		childs.remove(fid);
		return;
	}

	@Override
	public void change(Long id, crud c, HashMap<String, Object> o, ArrayList<Long> childs, HashMap<Long, HashMap<String, Object>> cache) {
		// if (childs != null && !childs.isEmpty()) {
		// if (!Long.class.equals(childs.get(0).getClass()))
		// for (int n = 0; n < childs.size(); n++) {
		// childs.set(n, ((Number) childs.get(n)).longValue());
		// }
		int order=0;
		switch (c) {
		case delete:
			add(id, childs, -1, null);
			order=-1;
			break;
		case update:
			add(id, childs, 0, null);
			break;
		case create:
			add(id, childs, 1, null);
			break;
		default:
			break;
		}
		// }
		write(id, order, childs);
		setChange(true);
	}

	/**
	 * Finds all parents that "contain" a specific child ID.
	 * @param long1 child object ID.
	 * @param cache object data cache.
	 * @return list of parent IDs.
	 */
	public List<Long> getContains(Long long1, HashMap<Long, HashMap<String, Object>> cache) {
		List<Long> par = fathers.get(long1);
		if (par == null)
			par = Collections.emptyList();
		return par;
	}

	@Override
	protected ArrayList<blockidx> getBlokcs() {
		ArrayList<blockidx> blks = new ArrayList<blockidx>();
		for (Long l : childs.keySet()) {
			blockidx b = new blockidx();
			b.id = l;
			b.childs = childs.get(l);
			b.childcount = b.childs.size();
			blks.add(b);
		}
		return blks;
	}

	@Override
	protected void clear() {
		childs.clear();
		fathers.clear();
		fragments = 0;
	}

	@Override
	protected boolean isFragmented() {
		if (fragments > 1000)
			return fragments / (double) childs.size() > 0.1;
		return false;
	}
}
