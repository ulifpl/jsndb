package org.jsndb.serializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsndb.beans.classtool;
import org.jsndb.kore.dataBase;
import org.jsndb.kore.lazyload;
import org.jsndb.kore.loader;
import org.json.Json;

/**
 * High-level serializer and parser that bridges the core DB logic and the JSON library.
 * Handles deep object resolution, external relationship loading, and lazy loading.
 */
public class parser {
	/** Key used in flat maps to store external relationship IDs. */
	public final static String JSNDB_EXERNAL = "JSNDB EXERNAL";
	/** Marker for parsed state (internal use). */
	public final static String JSNDB_PARSED = "JSNDB PARSED";
	/** Marker for cleaned state (internal use). */
	public final static String JSNDB_CLEANED = "JSNDB CLEANED";


	/**
	 * Converts a JSON string into a bean, resolving all deep relationships.
	 * @param str JSON string.
	 * @param cl target bean class.
	 * @param db database instance for resolving IDs.
	 * @return populated bean instance.
	 */
	public static <T> T jsonToDeepBean(String str, Class<T> cl, dataBase db) {
		// System.out.println("parser.fromJson()  " + str + "   " + cl);
		return Json.mapToBean(jsonToDeepMap(str, db, cl, null), cl);
	}

	/**
	 * Converts a JSON string into a bean without resolving external relationships.
	 * @param str JSON string.
	 * @param cl target bean class.
	 * @return populated bean instance.
	 */
	public static <T> T jsonToFlatBean(String str, Class<T> cl) {
		return Json.jsonToBean(str, cl);
		// return Json.mapToBean(jsonToFlatMap(str), cl);
	}

	/**
	 * Populates a bean from a map of properties.
	 * @param m map of properties.
	 * @param c target bean class.
	 * @return populated bean instance.
	 */
	public static <T> T fromMap(HashMap<String, Object> m, Class<T> c) {
		return Json.mapToBean(m, c);
	}

	/**
	 * Converts a bean into a property map, with optional property filtering.
	 * @param obj bean to convert.
	 * @param props set of property names to filter.
	 * @param include true to include only listed props, false to exclude them.
	 * @return map of properties.
	 */
	public static <C> HashMap<String, Object> beanToMap(C obj, Set<String> props, boolean include) {
		return Json.beanToMap(obj, props, include);
	}

	/**
	 * Converts a bean into its full property map.
	 * @param obj bean to convert.
	 * @return map of properties.
	 */
	public static <C> HashMap<String, Object> beanToMap(C obj) {
		return Json.beanToMap(obj);
	}

	/**
	 * Parses a JSON string into a map and recursively resolves all external IDs into full maps or lazy lists.
	 * @param obj JSON string.
	 * @param db database instance.
	 * @param cl class type to guide parsing and reflection.
	 * @param parsed cache of already parsed IDs to prevent infinite loops.
	 * @return deep property map.
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, Object> jsonToDeepMap(String obj, dataBase db, Class<?> cl,
			HashMap<Long, HashMap<String, Object>> parsed) {
		HashMap<String, Object> m = Json.jsonToMap(obj);
		if (m.containsKey(JSNDB_EXERNAL)) {
			classtool clt = classtool.getInstance(cl);
			if (parsed == null)
				parsed = new HashMap<>();
			parsed.put((Long) m.get(clt.getIdName()), m);
			HashMap<String, ArrayList<Long>> je = (HashMap<String, ArrayList<Long>>) m.get(JSNDB_EXERNAL);
			for (String s : new HashSet<String>(je.keySet())) {
				ArrayList<Long> ids = je.get(s);
				je.remove(s);
				Class<?> c = clt.getGenericType(s);
				if (c != null) {
					if (Collection.class.isAssignableFrom(clt.getFieldClazz(s))) {
						List<?> ll = (List<?>) lazyload.create(ids, c, clt.getFieldClazz(s), db);
						m.put(s, ll);
					} else if (Map.class.isAssignableFrom(clt.getFieldClazz(s))) {
						Map<?, ?> ll = (Map<?, ?>) lazyload.create(ids, c, clt.getFieldClazz(s), db);
						m.put(s, ll);
					}
					continue;
				} else if ((c = clt.getFieldClazz(s)) != null) {
					short clcode = db.getMdatas(c).getClazzcode();
					for (Long id : ids) {
						if (parsed.containsKey(id)) {
							m.put(s, parsed.get(id));
						} else {
							HashMap<String, Object> map = jsonToDeepMap(loader.getJsonById(db, id, clcode), db, c, parsed);
							m.put(s, map);
						}
					}
					continue;
				}

			}
		}
		return m;
	}

	/**
	 * Multi-purpose utility to convert a map to a JSON string.
	 * @param obj map to convert.
	 * @return JSON string.
	 */
	public static <T> String mapToJson(HashMap<String, Object> obj) {
		return Json.mapToJson(obj);
	}

	/**
	 * Converts a bean into its JSON representation.
	 * @param obj bean to convert.
	 * @return JSON string.
	 */
	public static <T> String beanToJson(T obj) {
		return Json.beanToJson(obj);
	}

	/**
	 * Converts JSON directly to a flat property map.
	 * @param strjson JSON string.
	 * @return flat map.
	 */
	public static HashMap<String, Object> jsonToFlatMap(String strjson) {
		return Json.jsonToMap(strjson);
	}
}