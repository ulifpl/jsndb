package org.jsndb.beans.mutator;

/**
 * Utility class for performing safe type casting and numeric mutations.
 * Handles conversion between primitive wrappers and generic Number types.
 */
public class cast {
	/**
	 * Casts an object to a target numeric class or primitive wrapper.
	 * @param c target Class.
	 * @param o object to cast (expected to be a Number).
	 * @return casted object.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Object to(Class<T> c, Object o) {
		System.out.println("cast.to("+c.getName()+")");
		switch (c.getName()) {
		case "java.lang.Long":
		case "long":
			return Number.class.cast(o).longValue();
		case "short":
		case "java.lang.Short":
			return Number.class.cast(o).shortValue();
		case "int":
		case "java.lang.Integer":
			return Number.class.cast(o).intValue();
		case "byte":
		case "java.lang.Byte":
			return Number.class.cast(o).byteValue();
		case "double":
		case "java.lang.Double":
			return Number.class.cast(o).doubleValue();
		case "float":
		case "java.lang.Float":
			return Number.class.cast(o).floatValue();
		default:
			break;
		}
		return (T) o;

	}
}
