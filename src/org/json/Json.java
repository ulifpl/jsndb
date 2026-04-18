package org.json;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jsndb.beans.classtool;
import org.jsndb.serializer.parser;

/**
 * Facade class for JSON operations within the jsndb engine.
 * Provides easy-to-use methods for bean-to-JSON and JSON-to-bean transformations.
 */
public class Json {

	/**
	 * Converts a bean object into a JSON string.
	 * @param bean object to convert.
	 * @return JSON string.
	 */
	public static <T> String beanToJson(T bean) {
		HashMap<String, Object> m = JSONObject.beanToMap(bean);
		String s = JSONObject.toString(m);
		return s;
	}

	/**
	 * Converts a JSON string into a bean of the specified class.
	 * @param json JSON string.
	 * @param clazz target bean class.
	 * @return populated bean instance.
	 */
	public static <T> T jsonToBean(String json, Class<T> clazz) {
		HashMap<String, Object> m = JSONObject.stringToMap(json);
		return mapToBean(m, clazz);
	}

	/**
	 * Converts a bean into a property map with optional filtering.
	 * @param bean object to convert.
	 * @param props properties to filter.
	 * @param exclude true to exclude listed props, false to include them.
	 * @return property map.
	 */
	public static <T> HashMap<String, Object> beanToMap(T bean,Set<String> props,boolean exclude) {
		return JSONObject.beanToMap(bean,props,exclude);
	}

	/**
	 * Converts a bean into a full property map.
	 * @param bean object to convert.
	 * @return property map.
	 */
	public static <T> HashMap<String, Object> beanToMap(T bean) {
		return JSONObject.beanToMap(bean);
	}

	/**
	 * Hydrates a bean from a property map using reflection via {@link classtool}.
	 * @param map property map.
	 * @param clazz target bean class.
	 * @return populated bean instance.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T mapToBean(HashMap<String, Object> map, Class<T> clazz) {
		classtool clt = classtool.getInstance(clazz);
		if (map == null || clt == null)
			return null;
		T bean = (T) clt.createObject();
		map.put(parser.JSNDB_PARSED, bean);
		if (bean == null)
			return null;
		for (String prop : map.keySet()) {
			if (prop.equals(parser.JSNDB_PARSED)||prop.equals(parser.JSNDB_EXERNAL))
				continue;
			Object obj = map.get(prop);
			if (obj == null)
				continue;
			if (clt.getFieldClazz(prop).equals(obj.getClass())) {
				clt.setProp(prop, bean, obj);
				continue;
			}

			if (clt.getExtProps().contains(prop)) {
				if (obj instanceof HashMap) {
					HashMap<String, Object> inmap = (HashMap<String, Object>) obj;
					obj = inmap.get(parser.JSNDB_PARSED);
					if (obj == null)
						obj = mapToBean(inmap, clt.getFieldClazz(prop));
				}
			}
			if (clt.getFieldClazz(prop).isArray()) {
				clt.setProp(prop, bean, classtool.newArray(clt.getArrayType(prop), (List<Object>) obj));
				continue;
			}
			// if (List.class.isAssignableFrom(clt.getFieldClazz(prop))) {
			// clt.setProp(prop, bean, cast.to(clt.getGenericType(prop),
			// obj));
			// continue;
			// }
			// if (Map.class.isAssignableFrom(clt.getFieldClazz(prop))) {
			// clt.setProp(prop, bean, cast.to(clt.getGenericType(prop),
			// obj));
			// continue;
			// }
			clt.setProp(prop, bean, obj);
		}
		return bean;
	}

	/**
	 * Parses a JSON string into a flat property map.
	 * @param json JSON string.
	 * @return property map.
	 */
	public static HashMap<String, Object> jsonToMap(String json) {
		return JSONObject.stringToMap(json);
	}

	/**
	 * Converts a property map into a JSON string.
	 * @param obj map to convert.
	 * @return JSON string.
	 */
	public static String mapToJson(HashMap<String, Object> obj) {
		return JSONObject.toString(obj);
	}

}
