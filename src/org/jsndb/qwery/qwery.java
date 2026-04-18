package org.jsndb.qwery;

/**
 * Base abstract class for defining database queries.
 * Encapsulates the target class, the property to filter on, the comparison value, and the comparator type.
 */
public abstract class qwery {
	/** The target java class to query. */
	private Class<?> clazz;
	/** The value to compare against. */
	private Object value;
	/** The property name of the target class to filter. */
	private String property;
	/** The comparison operator (equals, greaterThan, lessThan, etc.). */
	private comparators comp;

	/**
	 * Factory method to create a new query object.
	 * @param clz target class.
	 * @param val comparison value.
	 * @param prop property name.
	 * @param com comparator type.
	 * @return a query instance or null if clz or com are missing.
	 */
	public static qwery create(Class<?> clz, Object val, String prop, comparators com) {
		qwery q = new qwery() {
		};
		q.value = val;
		if (clz != null) {
			q.clazz = clz;
			q.property = prop;
			if (com != null) {
				q.comp = com;
				return q;
			}
		}
		return null;

	}

	/**
	 * Gets the target class of the query.
	 * @return target Class.
	 */
	public Class<?> getClazz() {
		return clazz;
	}

	/**
	 * Gets the comparison value.
	 * @return comparison Object.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Gets the property name to filter.
	 * @return property string.
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * Gets the comparator type.
	 * @return comparators enum.
	 */
	public comparators getComp() {
		return comp;
	}
}
