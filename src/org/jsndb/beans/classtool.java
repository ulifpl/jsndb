package org.jsndb.beans;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsndb.error;
import org.jsndb.annotations.jsndbObjectId;
import org.jsndb.error.errortype;
import org.jsndb.kore.dataBase;

public abstract class classtool {
	private static HashMap<String, classtool> cache = new HashMap<>();
	private static HashMap<String, classtool> marshable = new HashMap<>();
	private HashMap<String, Field> fields = new HashMap<>();
	private HashMap<String, Method> gMeth = new HashMap<>();
	private HashMap<String, Method> sMeth = new HashMap<>();
	// private ArrayList<String> arrayprops = new ArrayList<String>();
	private HashSet<String> externalprops = new HashSet<>();
	private HashSet<String> needMutate = new HashSet<>();
	private HashMap<String, Class<?>> arrayType = new HashMap<>();
	private HashMap<String, Class<?>> genericType = new HashMap<>();

	// private ArrayList<String> innerprops = new ArrayList<String>();
	private String objId;
	private Field idProp;
	private Method methGetId;
	private Method methSetId;
	private Constructor<?> constructor;

	public Object createObject() {
		try {
			return getConstructor().newInstance();
		} catch (IllegalArgumentException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	private HashMap<String, Field> getFields() {
		return fields;
	}

	public Set<String> getAllPropNames() {
		return fields.keySet();
	}

	public Constructor<?> getConstructor() {
		return constructor;
	}

	private void setConstructor(Constructor<?> constructor) {
		this.constructor = constructor;
	}

	public HashSet<String> getExtProps() {
		return externalprops;
	}

	public HashSet<String> getNeedMutate() {
		return needMutate;
	}

	public String getIdName() {
		return objId;
	}

	private void setIdName(String idprop) {
		this.objId = idprop;
	}

	private HashMap<String, Method> getgMeth() {
		return gMeth;
	}

	private HashMap<String, Method> getsMeth() {
		return sMeth;
	}

	public Class<?> getFieldClazz(String prop) {
		if (fields.get(prop) == null)
			System.out.println("classtool.getFieldClazz()");

		return fields.get(prop).getType();
	}

	public Class<?> getGenericType(String prop) {
		return genericType.get(prop);
	}

	public Class<?> getArrayType(String prop) {
		return arrayType.get(prop);
	}

	public void setProp(String prop, Object o, Object val) {
		try {
			if (getsMeth().containsKey(prop))
				getsMeth().get(prop).invoke(o, val);
			else if (getFields().containsKey(prop))
				getFields().get(prop).set(o, val);
		} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			dataBase.getEhandler().error(new error("can't set prop " + prop + " on " + o.getClass(), errortype.externalException, e));
		}

	}

	public Object getProp(String prop, Object o) {
		try {
			if (getgMeth().containsKey(prop))
				return getgMeth().get(prop).invoke(o);
			if (getFields().containsKey(prop))
				return getFields().get(prop).get(o);
		} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			dataBase.getEhandler()
					.error(new error("can't get prop " + prop + " from " + o.getClass(), errortype.externalException, e));
		}

		return null;
	}

	public static int count = 0;

	@SuppressWarnings("unchecked")
	public static <T> T[] newArray(Class<T> cl, List<Object> list) {
		T[] a = (T[]) Array.newInstance(cl, list.size());
		for (int n = 0; n < list.size(); n++)
			a[n] = (T) list.get(n);
		return a;
	}

	public Long getid(Object o) {
		count++;
		try {
			if (getIdGMeth() != null)
				return (Long) getIdGMeth().invoke(o);
			return getIdField().getLong(o);
		} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			dataBase.getEhandler().error(new error("can't get id from " + o.getClass(), errortype.externalException, e));
		}
		return null;
	}

	public ArrayList<Long> getExtObjIds(Object obj, String prop) {
		ArrayList<Long> ids = new ArrayList<Long>();
		if (Collection.class.isAssignableFrom(obj.getClass())) {
			Collection<?> col = (Collection<?>) obj;
			for (Object child : col) {
				ids.add(classtool.getInstance(getGenericType(prop)).getid(child));
			}
		} else if (Map.class.isAssignableFrom(obj.getClass())) {
			Map<?, ?> col = (Map<?, ?>) obj;
			for (Object child : col.values()) {
				ids.add(classtool.getInstance(getGenericType(prop)).getid(child));
			}
		} else {
			ids.add(classtool.getInstance(getFieldClazz(prop)).getid(obj));
		}
		return ids;
	}

	public void setid(Object o, Long n) {
		try {
			if (getIdSMeth() != null)
				getIdSMeth().invoke(o, n);
			else
				getIdField().setLong(o, n);
		} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			dataBase.getEhandler().error(new error("can't set id on " + o.getClass(), errortype.externalException, e));
		}
	}

	public HashMap<String, Object> nullext(HashMap<String, Object> o) {
		HashMap<String, Object> list = new HashMap<String, Object>();
		for (String p : getExtProps()) {
			Object val = o.remove(p);
			if (val == null)
				continue;
			list.put(p, val);
		}
		return list;
	}

	private static HashMap<Class<?>, Class<?>> compchache = new HashMap<>();

	@SuppressWarnings("rawtypes")
	public static Comparable getComparable(Object val) {
		Class<?> c = val.getClass();
		Class<?> comp = compchache.get(c);
		if (comp != null)
			return (Comparable) val;
		if (Comparable.class.isAssignableFrom(c)) {
			compchache.put(c, val.getClass());
			return (Comparable) val;
		} else if (c.isPrimitive()) {
			if (int.class.isAssignableFrom(c)) {
				compchache.put(int.class, Integer.class);
				return ((Comparable) Integer.class.cast(((Number) val).intValue()));
			}
			if (long.class.isAssignableFrom(c)) {
				compchache.put(long.class, Long.class);
				return ((Comparable) Long.class.cast(((Number) val).intValue()));
			}
			if (boolean.class.isAssignableFrom(c)) {
				compchache.put(boolean.class, Boolean.class);
				return ((Comparable) Boolean.class.cast(val));
			}
			if (double.class.isAssignableFrom(c)) {
				compchache.put(double.class, Double.class);
				return ((Comparable) Double.class.cast(((Number) val).doubleValue()));
			}
			if (float.class.isAssignableFrom(c)) {
				compchache.put(float.class, Float.class);
				return ((Comparable) Float.class.cast(((Number) val).floatValue()));
			}
			if (short.class.isAssignableFrom(c)) {
				compchache.put(short.class, Short.class);
				return ((Comparable) Short.class.cast(((Number) val).shortValue()));
			}
			if (byte.class.isAssignableFrom(c)) {
				compchache.put(byte.class, Byte.class);
				return ((Comparable) Byte.class.cast(((Number) val).byteValue()));
			}
			if (char.class.isAssignableFrom(c)) {
				compchache.put(char.class, Character.class);
				return ((Comparable) Character.class.cast(val));
			}
		}
		return null;
	}

	public static <T extends Comparable<T>> Comparator<T> getComparator(T val) {
		return new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return o1.compareTo(o2);
			}
		};
	}

	private Field getIdField() {
		return idProp;
	}

	private void setIdField(Field idProp) {
		this.idProp = idProp;
	}

	private void setIdGMeth(Method methId) {
		this.methGetId = methId;
	}

	private Method getIdGMeth() {
		return methGetId;
	}

	private Method getIdSMeth() {
		return methSetId;
	}

	private void setIdSMeth(Method methSetId) {
		this.methSetId = methSetId;
	}

	private static HashSet<Class<?>> proccessing = new HashSet<Class<?>>();

	public static <T> classtool checkMarshable(Class<T> cl) {
		if (marshable.containsKey(cl))
			return marshable.get(cl);
		marshable.put(cl.getName(), null);
		// classtool clt = null;
		if (Collection.class.isAssignableFrom(cl)) {

		}
		if (cl.isPrimitive())
			return null;
		if (Number.class.isAssignableFrom(cl))
			return null;
		if (String.class.isAssignableFrom(cl))
			return null;

		return null;
	}

	public static classtool getInstanceIfLoaded(String clzz) {
		return cache.get(clzz);
	}

	public static synchronized classtool getInstance(Class<?> clzz) {
		classtool ct = cache.get(clzz.getName());
		if (ct != null)
			return ct;
		proccessing.add(clzz);
		ct = new classtool() {
		};
		Class<?> tempclas = clzz;
		if (clzz.isPrimitive())
			return null;
		try {
			ct.setConstructor(clzz.getConstructor());
		} catch (NoSuchMethodException | SecurityException e) {
			// e.printStackTrace();
			return null;
		}
		if (ct.getConstructor() == null)
			return null;
		while (tempclas != null) {
			for (Field f : tempclas.getDeclaredFields()) {
				if (Modifier.isTransient(f.getModifiers()))
					continue;
				if (Modifier.isStatic(f.getModifiers()))
					continue;
				f.setAccessible(true);
				ct.getFields().put(f.getName(), f);
				Class<?> type = f.getType();
				if (f.isAnnotationPresent(jsndbObjectId.class)) {
					if (type.equals(Long.class)) {
						ct.setIdName(f.getName());
						ct.setIdField(f);
						cache.put(clzz.getName(), ct);
					}
				}
				if (type.isArray()) {
					type = type.getComponentType();
					ct.getNeedMutate().add(f.getName());
					ct.arrayType.put(f.getName(), type);
				} else if (Collection.class.isAssignableFrom(type)) {
					ParameterizedType stringListType = (ParameterizedType) f.getGenericType();
					if (stringListType.getActualTypeArguments()[0] instanceof Class<?>) {
						type = (Class<?>) stringListType.getActualTypeArguments()[0];
						ct.genericType.put(f.getName(), type);
					}
				} else if (Map.class.isAssignableFrom(type)) {
					ParameterizedType stringListType = (ParameterizedType) f.getGenericType();
					if (stringListType.getActualTypeArguments()[1] instanceof Class<?>) {
						type = (Class<?>) stringListType.getActualTypeArguments()[1];
						ct.genericType.put(f.getName(), type);
					}
				}
				// System.out.println("classtool.getInstance() " + type);

				boolean isEntity = !type.isPrimitive() && !type.getName().startsWith("java.") && !type.getName().startsWith("javax.");
				if (isEntity && (proccessing.contains(type) || classtool.getInstance(type) != null)) {
					ct.getExtProps().add(f.getName());
					// if (Collection.class.isAssignableFrom(type))
					// ct.getCollecionsProps().add(f.getName());
				}
			}

			for (Method meth : tempclas.getDeclaredMethods()) {
				meth.setAccessible(true);
				for (String cl : ct.getFields().keySet()) {
					if (meth.getName().equalsIgnoreCase("get" + cl))
						ct.getgMeth().put(cl, meth);
					if (meth.getName().equalsIgnoreCase("is" + cl))
						ct.getgMeth().put(cl, meth);
					if (meth.getName().equalsIgnoreCase("set" + cl))
						ct.getsMeth().put(cl, meth);
					if (meth.getName().equalsIgnoreCase("set" + ct.getIdName()))
						ct.setIdSMeth(meth);
					if (meth.getName().equalsIgnoreCase("get" + ct.getIdName()))
						ct.setIdGMeth(meth);
				}
			}

			tempclas = tempclas.getSuperclass();
		}
		proccessing.remove(clzz);
		if (ct.getIdField() == null)
			return null;
		cache.put(clzz.getName(), ct);
		return ct;
	}

	/**
	 * Clears all static caches to ensure a clean state between database sessions.
	 */
	public static synchronized void close() {
		cache.clear();
		marshable.clear();
		compchache.clear();
		proccessing.clear();
		count = 0;
	}
}
