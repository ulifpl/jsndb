package org.jsndb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jsndb.beans.classtool;
import org.jsndb.index.idOffset;
import org.jsndb.index.fileids;
import org.jsndb.index.index;
import org.jsndb.index.indexChild;
import org.jsndb.index.superindex;
import org.jsndb.kore.dataBase;
import org.jsndb.kore.datafile;
import org.jsndb.kore.metaData;
import org.jsndb.kore.enums.crud;
import org.jsndb.lazyLoad.lazyArrayList;
import org.jsndb.qwery.connector;
import org.jsndb.qwery.qwery;
import org.jsndb.serializer.parser;
import org.jsndb.util.circleRef;
import org.jsndb.util.logger;

/**
 * Main facade for the jsndb database engine.
 * Provides methods for persisting, deleting, and querying objects.
 */
public class jsndb {
	/** Underlying database core management. */
	private dataBase db;
	
	/** Objects queued for insertion/update in the current transaction. */
	private HashMap<Class<?>, ArrayList<Object>> transaction = new HashMap<Class<?>, ArrayList<Object>>();
	
	/** Objects queued for deletion in the current transaction. */
	private HashMap<Class<?>, ArrayList<Object>> transactionRemove = new HashMap<Class<?>, ArrayList<Object>>();
	
	/** Regex split pattern for "like" queries. */
	public static String LIKE_SPLIT = "(\\W+|ES$|es$|s$)";

	/**
	 * Initializes the database session with a given directory path.
	 * @param path directory path where data is stored.
	 */
	public jsndb(String path) {
		db = new dataBase(path);
	}

	/**
	 * Queues an object to be persisted in the database.
	 * The object will be saved to disk upon calling commit().
	 * @param obj the object to persist.
	 * @return this instance for method chaining.
	 */
	public <T> jsndb persist(T obj) {
		if (db.getMdatas(obj.getClass()) == null) {
			return null;
		}
		ArrayList<Object> arr = transaction.get(obj.getClass());
		if (arr == null) {
			arr = new ArrayList<Object>();
			transaction.put(obj.getClass(), arr);
		}
		arr.add(obj);
		return this;
	}

	/**
	 * Queues a list of objects to be persisted.
	 * @param objs list of objects to persist.
	 * @return this instance for method chaining.
	 */
	public jsndb persist(List<Object> objs) {
		for (Object o : objs)
			persist(o);
		return this;
	}

	/**
	 * Commits the current transaction, flushing all pending changes to disk.
	 * This operation is thread-safe.
	 * @return true if the commit was successful, false otherwise.
	 */
	public synchronized boolean commit() {
		// if (!state) {
		// transaction.clear();
		// state = true;
		// return false;
		// }
		HashMap<Long, HashMap<String, Object>> cache = new HashMap<>();
		for (Class<?> clz : transaction.keySet()) {
			for (Object o : transaction.get(clz)) {
				if (!save(o, false, new circleRef(o, null), cache)) {
					rollBack();
					return false;
				}
			}
		}
		db.getDatafile().flush();
		db.getObjIds().flush();
		superindex.flush();

		HashMap<Class<?>, ArrayList<idOffset>> toremove = new HashMap<Class<?>, ArrayList<idOffset>>();
		for (Class<?> clz : transactionRemove.keySet()) {
			ArrayList<idOffset> li = toremove.get(clz);
			if (li == null) {
				li = new ArrayList<>();
				toremove.put(clz, li);
			}
			for (Object o : transactionRemove.get(clz)) {
				li.add(del(o));
			}
		}
		for (Class<?> clz : toremove.keySet()) {
			db.getDatafile().flush();
			db.getObjIds().add(toremove.get(clz));
			db.getObjIds().flush();
			superindex.flush();
		}
		transaction.clear();
		transactionRemove.clear();
		cache.clear();
		return true;
	}

	/**
	 * Rolls back the current transaction, clearing all pending changes.
	 */
	private void rollBack() {
		transaction.clear();
		transactionRemove.clear();
		cache.clear();
	}

	HashMap<Class<?>, parser> jsons = new HashMap<>();

	/**
	 * Checks for circular references during serialization to avoid infinite recursion.
	 * @param cr reference tracker.
	 * @param o object to check.
	 * @param counter depth counter.
	 * @return true if a circular reference is detected.
	 * @throws IllegalArgumentException if recursion depth exceeds 20.
	 */
	private boolean chekCircle(circleRef cr, Object o, int counter) {
		counter++;
		if (counter > 20)
			throw new IllegalArgumentException();
		if (cr.hasParent()) {
			if (cr.getParent().getVal() != o)
				chekCircle(cr.getParent(), o, counter);
			else
				return true;
		}
		return false;

	}

	/**
	 * Internal method to save an object and its dependencies.
	 * @param obj object to save.
	 * @param checkonly if true, only checks if the object needs to be created.
	 * @param circle circular reference tracker.
	 * @param cache cache for object data maps.
	 * @return true if successful.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> boolean save(T obj, boolean checkonly, circleRef circle, HashMap<Long, HashMap<String, Object>> cache) {
		if (chekCircle(circle, obj, 0))
			return true;
		Class<?> clz = obj.getClass();
		classtool cls = db.getMdatas(clz).getClstool();
		crud cr = crud.update;
		if (cls.getid(obj) == null) {
			cls.setid(obj, (Long) db.nextkey());
			if (cls.getid(obj).equals(0l)) {
				return false;
			}
			cr = crud.create;
		}

		if (cr == crud.update && checkonly)
			return true;
		HashMap<String, Object> mapObj = parser.beanToMap(obj, cls.getExtProps(), false);
		// HashMap<String, Object> extList = cls.nullext(mapObj);
		for (String prop : cls.getExtProps()) {
			if (cls.getProp(prop, obj) == null)
				continue;
			if (Collection.class.isAssignableFrom(cls.getProp(prop, obj).getClass())) {
				saveColection((Collection) cls.getProp(prop, obj), circle, cache);
			} else if (Map.class.isAssignableFrom(cls.getProp(prop, obj).getClass())) {
				saveColection(((Map) cls.getProp(prop, obj)).values(), circle, cache);
			} else {
				save(cls.getProp(prop, obj), true, new circleRef(cls.getProp(prop, obj), circle), cache);
			}
		}
		HashMap<String, ArrayList<Long>> extListIds = new HashMap<>();
		for (String prop : cls.getExtProps()) {
			if (cls.getProp(prop, obj) == null)
				continue;
			ArrayList<Long> ids = db.getMdatas(clz).getClstool().getExtObjIds(cls.getProp(prop, obj), prop);
			extListIds.put(prop, ids);
		}
		mapObj.put(parser.JSNDB_EXERNAL, extListIds);
		String str = parser.mapToJson((HashMap<String, Object>) mapObj);
		idOffset dof = db.getDatafile().write(str, cls.getid(obj), db.getMdatas(clz).getClazzcode());
		db.getObjIds().add(dof);
		cache.put(dof.getIdObj(), mapObj);
		db.getMdatas(clz).updateidx(dof, cr, mapObj, extListIds, cache);
		return true;
	}

	HashMap<Long, Object> cache = new HashMap<Long, Object>();

	/**
	 * Queues an object for deletion.
	 * @param obj object to delete.
	 * @return this instance for method chaining.
	 */
	public <T> jsndb delete(T obj) {
		ArrayList<Object> lis = transactionRemove.get(obj.getClass());
		if (lis == null) {
			lis = new ArrayList<>();
			transactionRemove.put(obj.getClass(), lis);
		}
		lis.add(obj);
		return this;
	}

	/**
	 * Internal method to delete an object from disk and indexes.
	 * @param obj object to delete.
	 * @return the offset information of the deleted object.
	 */
	private <T> idOffset del(T obj) {
		metaData md = db.getMdatas(obj.getClass());
		long id = md.getClstool().getid(obj);
		idOffset iof = new idOffset(id, -1l);
		for (String prop : md.getPropIdxs()) {
			superindex si = db.getIndice(null, md.getClazz(), prop);
			si.change(id, crud.delete, null, new ArrayList<Long>(), null);
		}
		return iof;
	}

	/**
	 * Queues multiple objects for deletion.
	 * @param objs list of objects to delete.
	 * @return this instance for method chaining.
	 */
	public jsndb delete(ArrayList<Object> objs) {
		for (Object obj : objs) {
			ArrayList<Object> lis = transactionRemove.get(obj.getClass());
			if (lis == null) {
				lis = new ArrayList<>();
				transactionRemove.put(obj.getClass(), lis);
			}
			lis.add(obj);
		}
		return this;
	}

	/**
	 * Helper method to save all items in a collection.
	 * @param coll collection to save.
	 * @param circle circular reference tracker.
	 * @param cache cache for object maps.
	 */
	private <T> void saveColection(Collection<T> coll, circleRef circle, HashMap<Long, HashMap<String, Object>> cache) {
		for (T oo : coll)
			save(oo, true, new circleRef(oo, circle), cache);
	}

	/**
	 * Retrieves an object by its database ID.
	 * @param id the object ID.
	 * @param cls the class of the object.
	 * @return the object, or null if not found.
	 */
	public <T> T getById(Long id, Class<T> cls) {
		if (id != null) {
			datafile dtfl = db.getDatafile();
			fileids fids = db.getObjIds();
			if ((id = fids.getDataOff().get(id)) != null)
				return parser.jsonToDeepBean(dtfl.read(id, (short) -1), cls, db);
		}
		return null;
	}

	/**
	 * Retrieves multiple objects by their database IDs.
	 * @param ids list of IDs.
	 * @param cls the class of the objects.
	 * @return list of retrieved objects.
	 */
	public <T> List<T> getByIds(List<Long> ids, Class<T> cls) {
		List<T> objs = new ArrayList<>();
		for (Long i : ids) {
			T obj;
			if ((obj = getById(i, cls)) != null)
				objs.add(obj);
		}
		return objs;
	}

	/**
	 * Executes a query and returns a list of results.
	 * Results are loaded lazily.
	 * @param c the class to search for.
	 * @param query the query criteria.
	 * @return list of matching objects.
	 */
	public <T> List<T> select(Class<T> c, qwery query) {
		ArrayList<Long> ids = analize(query, false, new HashMap<Long, HashMap<String, Object>>());
		return new lazyArrayList<T>(db, c, ids);
	}

	/**
	 * Executes a query with pagination support.
	 * @param c class to search for.
	 * @param query query criteria.
	 * @param from starting index (offset).
	 * @param count maximum number of results.
	 * @return list of matching objects.
	 */
	public <T> List<T> select(Class<T> c, qwery query, int from, int count) {
		ArrayList<Long> ids = analize(query, false, new HashMap<Long, HashMap<String, Object>>());
		if (from < ids.size()) {
			int to = from + count;
			if (to >= ids.size())
				to = ids.size();
			return new lazyArrayList<T>(db, c, ids.subList(from, to));
		}
		return new ArrayList<T>();
	}

	/**
	 * Internal method to analyze a query and return matching IDs.
	 * @param query query to analyze.
	 * @param not if true, negates the query results (currently unused/partial).
	 * @param cach query-time cache.
	 * @return list of matching IDs.
	 */
	private <T> ArrayList<Long> analize(qwery query, boolean not, HashMap<Long, HashMap<String, Object>> cach) {
		ArrayList<Long> array = null;
		if (query instanceof connector) {
			connector c = (connector) query;
			switch (c.getType()) {
			case and:
				for (qwery q : c.getList()) {
					if (array == null) {
						array = analize(q, false, cach);
						continue;
					}
					ArrayList<Long> temp;
					ArrayList<Long> array2 = analize(q, false, cach);
					if (array2.size() > array.size()) {
						temp = array;
						array = array2;
						array2 = temp;
					}
					Collections.sort(array);
					temp = new ArrayList<Long>();
					for (Long l : array2) {
						if (Collections.binarySearch(array, l) > -1)
							// array.add(-(n + 1), l);
							temp.add(l);
					}
					array = temp;
				}
				break;
			// case not:
			case or:
				for (qwery q : c.getList()) {
					if (array == null) {
						array = analize(q, false, cach);
						continue;
					}
					ArrayList<Long> temp;
					ArrayList<Long> array2 = analize(q, false, cach);
					if (array2.size() > array.size()) {
						temp = array;
						array = array2;
						array2 = temp;
					}
					Collections.sort(array);
					temp = new ArrayList<Long>();
					for (Long l : array2) {
						if (Collections.binarySearch(array, l) < 0)
							array.add(l);
					}
				}
			default:
				break;
			}
		} else {
			return search(query, query.getClazz(), cach);
		}
		if (array.size() > 1)
			logger.line("jsndb.analize()");
		return array;
	}

	public dataBase getDb() {
		return db;
	}

	/**
	 * Performs a specific search based on a single query criterion.
	 * @param query query criteria.
	 * @param cls class to search for.
	 * @param cach query-time cache.
	 * @return list of matching IDs.
	 */
	private <T> ArrayList<Long> search(qwery query, Class<T> cls, HashMap<Long, HashMap<String, Object>> cach) {
		ArrayList<Long> list = new ArrayList<Long>();
		if (db.getMdatas(cls) != null) {
			db.getIndice("", cls, query.getProperty());
			if (db.getMdatas(cls).getPropIdxs().contains(query.getProperty())) {
				index idx = null;
				indexChild idex = null;
				if (db.getIndice(null, cls, query.getProperty()) instanceof index) {
					idx = (index) db.getIndice(null, cls, query.getProperty());
				} else {
					idex = (indexChild) db.getIndice(null, cls, query.getProperty());
				}
				int from, to;
				switch (query.getComp()) {
				case equal:
					from = idx.firstIndex(query.getValue(), cach);
					to = idx.lastIndex(query.getValue(), cach);
					if (from > -1) {
						if (to < 0)
							to = from;
						list = new ArrayList<Long>(idx.getList().subList(from, to + 1));
					}
					break;
				case egreatter:
					from = idx.firstIndex(query.getValue(), cach);
					if (from < 0)
						from = -(from + 1);
					if (from < idx.getList().size())
						list = new ArrayList<Long>(idx.getList().subList(from, idx.getList().size()));
					break;
				case greatter:
					to = idx.lastIndex(query.getValue(), cach);
					if (to < 0)
						to = -(to + 1);
					else
						to++;
					if (to < idx.getList().size())
						list = new ArrayList<Long>(idx.getList().subList(to, idx.getList().size()));
					break;
				case smaller:
					from = idx.firstIndex(query.getValue(), cach);
					if (from < 0)
						from = -(from + 1);
					if (from > -1)
						list = new ArrayList<Long>(idx.getList().subList(0, from));
					break;

				case esmaller:
					to = idx.lastIndex(query.getValue(), cach);
					if (to < 0)
						to = -(to + 1);
					else
						to++;
					if (to < idx.getList().size())
						list = new ArrayList<Long>(idx.getList().subList(0, to));
					break;
				case not:
					from = idx.firstIndex(query.getValue(), cach);
					to = idx.lastIndex(query.getValue(), cach);
					if (from > -1) {
						if (to < 0)
							to = from;
						HashSet<Long> hs = new HashSet<Long>(idx.getList());
						for (Long p : idx.getList().subList(from, to + 1))
							hs.remove(p);
						list = new ArrayList<Long>(hs);
					}
					break;
				case like:
					ArrayList<HashSet<Long>> ids = new ArrayList<>();
					// int max = 4;
					for (String s : ((String) query.getValue()).split(LIKE_SPLIT)) {
						s = s.toUpperCase();
						if (s.isEmpty())
							continue;
						ids.add(idx.preLike(s, cach));
					}

					HashSet<Long> ids2 = new HashSet<>();
					for (HashSet<Long> hs : ids) {
						ids2.addAll(hs);
					}
					list = new ArrayList<>(idx.getLike((String) query.getValue(), cach, ids2));
					break;
				case contains:
					ArrayList<Long> temp = new ArrayList<Long>();
					if (query.getValue() instanceof qwery)
						temp = analize(((qwery) query.getValue()), false, cach);
					for (Long dof : temp) {
						List<Long> al = idex.getContains(dof, cach);
						for (Long id : al)
							list.add(id);
					}
					System.gc();
					logger.line("select.contains  mem:" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
				default:
					break;
				}
			}
		}
		return list;
	}

	/**
	 * Closes the database session and flushes all metadata.
	 */
	public void close() {
		db.close();
	}
}
