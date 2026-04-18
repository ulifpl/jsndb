package org.json;

/*
 Copyright (c) 2002 JSON.org

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 The Software shall be used for Good, not Evil.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsndb.beans.classtool;
import org.jsndb.util.circleRef;

/**
 * A JSONObject is an unordered collection of name/value pairs. Its external
 * form is a string wrapped in curly braces with colons between the names and
 * values, and commas between the values and names. The internal form is an
 * object having <code>get</code> and <code>opt</code> methods for accessing the
 * values by name, and <code>put</code> methods for adding or replacing values
 * by name. The values can be any of these types: <code>Boolean</code>,
 * <code>JSONArray</code>, <code>JSONObject</code>, <code>Number</code>,
 * <code>String</code>, or the <code>JSONObject.NULL</code> object. A JSONObject
 * constructor can be used to convert an external form JSON text into an
 * internal form whose values can be retrieved with the <code>get</code> and
 * <code>opt</code> methods, or to convert values into a JSON text using the
 * <code>put</code> and <code>toString</code> methods. A <code>get</code> method
 * returns a value if one can be found, and throws an exception if one cannot be
 * found. An <code>opt</code> method returns a default value instead of throwing
 * an exception, and so is useful for obtaining optional values.
 * <p>
 * The generic <code>get()</code> and <code>opt()</code> methods return an
 * object, which you can cast or query for type. There are also typed
 * <code>get</code> and <code>opt</code> methods that do type checking and type
 * coercion for you. The opt methods differ from the get methods in that they do
 * not throw. Instead, they return a specified value, such as null.
 * <p>
 * The <code>put</code> methods add or replace values in an object. For example,
 * 
 * <pre>
 * myString = new JSONObject().put(&quot;JSON&quot;, &quot;Hello, World!&quot;).toString();
 * </pre>
 * 
 * produces the string <code>{"JSON": "Hello, World"}</code>.
 * <p>
 * The texts produced by the <code>toString</code> methods strictly conform to
 * the JSON syntax rules. The constructors are more forgiving in the texts they
 * will accept:
 * <ul>
 * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just
 * before the closing brace.</li>
 * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single
 * quote)</small>.</li>
 * <li>Strings do not need to be quoted at all if they do not begin with a quote
 * or single quote, and if they do not contain leading or trailing spaces, and
 * if they do not contain any of these characters:
 * <code>{ } [ ] / \ : , #</code> and if they do not look like numbers and if
 * they are not the reserved words <code>true</code>, <code>false</code>, or
 * <code>null</code>.</li>
 * </ul>
 * 
 * @author JSON.org
 * @version 2013-04-18
 */
public class JSONObject {

	// private static HashMap<Class<?>, classtool> clzTools = new
	// HashMap<Class<?>, classtool>();

	/**
	 * Wraps a map's values for JSON processing.
	 * @param map map to check.
	 * @param maped circular reference tracker.
	 * @return new map with wrapped values.
	 */
	public static HashMap<Object, Object> checkMap(HashMap<Object, Object> map, circleRef maped) {
		HashMap<Object, Object> newmap = new HashMap<>();
		if (map != null) {
			for (Object key : map.keySet()) {
				newmap.put(key, wrap(map.get(key), maped));
			}
		}
		return newmap;
	}

	// public final static char[] HEX = "0123456789ABCDEF".toCharArray();
	//
	// public static void unicode(char c, Writer out) {
	// try {
	// out.write("\\u");
	// int n = c;
	// for (int i = 0; i < 4; ++i) {
	// int digit = (n & 0xf000) >> 12;
	// out.write(HEX[digit]);
	// n <<= 4;
	// }
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }

	/**
	 * Construct a JSONObject from a JSONTokener.
	 * 
	 * @param x
	 *            A JSONTokener object containing the source string.
	 * @throws JSONException
	 *             If there is a syntax error in the source string or a
	 *             duplicated key.
	 * @throws IOException
	 */
	/**
	 * Parses a JSON text from a tokener into a map.
	 * @param x JSONTokener source.
	 * @return property map.
	 * @throws JSONException if syntax error occurs.
	 */
	protected static HashMap<String, Object> toMap(JSONTokener x) throws JSONException {
		HashMap<String, Object> map = new HashMap<String, Object>();
		char c;
		String key;
		if (x.nextClean() != '{') {
			throw x.syntaxError("A JSONObject text must begin with '{'");
		}
		for (;;) {
			c = x.nextClean();
			switch (c) {
			case 0:
				throw x.syntaxError("A JSONObject text must end with '}'");
			case '}':
				return map;
			default:
				x.back();
				key = x.nextValue().toString();
			}

			// The key is followed by ':'.

			c = x.nextClean();
			if (c != ':') {
				throw x.syntaxError("Expected a ':' after a key");
			}
			map.put(key, x.nextValue());

			// Pairs are separated by ','.

			switch (x.nextClean()) {
			case ';':
			case ',':
				if (x.nextClean() == '}') {
					return map;
				}
				x.back();
				break;
			case '}':
				return map;
			default:
				throw x.syntaxError("Expected a ',' or '}'");
			}
		}
	}

	/**
	 * Construct a JSONObject from an Object using bean getters. It reflects on
	 * all of the public methods of the object. For each of the methods with no
	 * parameters and a name starting with <code>"get"</code> or
	 * <code>"is"</code> followed by an uppercase letter, the method is invoked,
	 * and a key and the value returned from the getter method are put into the
	 * new JSONObject.
	 * 
	 * The key is formed by removing the <code>"get"</code> or <code>"is"</code>
	 * prefix. If the second remaining character is not upper case, then the
	 * first character is converted to lower case.
	 * 
	 * For example, if an object has a method named <code>"getName"</code>, and
	 * if the result of calling <code>object.getName()</code> is
	 * <code>"Larry Fine"</code>, then the JSONObject will contain
	 * <code>"name": "Larry Fine"</code>.
	 * 
	 * @param bean
	 *            An object that has getter methods that should be used to make
	 *            a JSONObject.
	 */
	public static HashMap<String, Object> beanToMap(Object bean) {
		return populateMap(bean, new circleRef(bean, null), new HashSet<String>(), false);
	}

	/**
	 * Construct a JSONObject from an Object, using reflection to find the
	 * public members. The resulting JSONObject's keys will be the strings from
	 * the names array, and the values will be the field values associated with
	 * those keys in the object. If a key is not found or not visible, then it
	 * will not be copied into the new JSONObject.
	 * 
	 * @param object
	 *            An object that has fields that should be used to make a
	 *            JSONObject.
	 * @param names
	 *            An array of strings, the names of the fields to be obtained
	 *            from the object.
	 */
	public static HashMap<String, Object> beanToMap(Object bean, Set<String> props, boolean include) {
		return populateMap(bean, new circleRef(bean, null), props, include);
	}

	/**
	 * Construct a JSONObject from a source JSON text string. This is the most
	 * commonly used JSONObject constructor.
	 * 
	 * @param source
	 *            A string beginning with <code>{</code>&nbsp;<small>(left
	 *            brace)</small> and ending with <code>}</code>
	 *            &nbsp;<small>(right brace)</small>.
	 * @exception JSONException
	 *                If there is a syntax error in the source string or a
	 *                duplicated key.
	 * @throws IOException
	 */
	public static HashMap<String, Object> stringToMap(String source) throws JSONException {
		return toMap(new JSONTokener(source));
	}

	/**
	 * Get the boolean value associated with a key.
	 * 
	 * @param key
	 *            A key string.
	 * @return The truth.
	 * @throws JSONException
	 *             if the value is not a Boolean or the String "true" or
	 *             "false".
	 */

	/**
	 * Formats a number for JSON output, adding type suffixes (l, s, i, b, d, f) for jsndb's internal parser.
	 * @param number number to format.
	 * @return formatted string with type suffix.
	 * @throws JSONException if number is null or invalid.
	 */
	public static String numberToString(Number number) throws JSONException {
		if (number == null) {
			throw new JSONException("Null pointer");
		}
		testValidity(number);

		String string = number.toString();
		switch (number.getClass().getName()) {
		case "java.lang.Long":
		case "java.lang.long":
		case "long":
			string = string + "l";
			break;
		case "short":
		case "java.lang.short":
		case "java.lang.Short":
			string = string + "s";
			break;
		case "int":
		case "java.lang.int":
		case "java.lang.Integer":
			string = string + "i";
			break;
		case "byte":
		case "java.lang.byte":
		case "java.lang.Byte":
			string = string + "b";
			break;
		case "double":
		case "java.lang.double":
		case "java.lang.Double":
			string = string + "d";
			break;
		case "float":
		case "java.lang.float":
		case "java.lang.Float":
			string = string + "f";
			break;
		default:
			throw new UnknownError(number.getClass().getName());
			// break;
		}
		return string;
	}

	private static HashMap<String, Object> populateMap(Object bean, circleRef maped, Set<String> props, boolean include) {
		if (bean == null)
			return null;
		classtool clt = classtool.getInstance(bean.getClass());
		if (clt == null)
			return null;
		// for (int n = 0; n < parsed.size(); n++)

		circleRef pp = maped;
		while (pp.hasParent()) {
			pp = pp.getParent();
			if (pp.getVal() == bean)
				return pp.getTransformed();
		}

		// maped.getAll().put(clt.getid(bean), bean);
		HashMap<String, Object> map = new HashMap<String, Object>();
		maped.setTransformed(map);
		if (maped.hasParent())
			maped.getParent().getChilds().add(maped);
		circleRef p = new circleRef(bean, maped);
		for (String prop : clt.getAllPropNames()) {
			if ((props.contains(prop) && !include) || (!props.contains(prop) && include))
				continue;
			map.put(prop, wrap(clt.getProp(prop, bean), p));
		}
		return map;
	}

	//
	// private static Object wrap(Object bean) {
	// classtool clt = classtool.getInstance(bean.getClass());
	// if (clt != null)
	// return populateMap(bean);
	// if (Collection.class.isAssignableFrom(bean.getClass())) {
	// Collection<?> c = (Collection<?>) bean;
	// if (c.isEmpty())
	// return c;
	// }
	// return null;
	// }

	//

	/**
	 * Produce a string in double quotes with backslash sequences in all the
	 * right places. A backslash will be inserted within </, producing <\/,
	 * allowing JSON text to be delivered in HTML. In JSON text, a string cannot
	 * contain a control character or an unescaped quote or backslash.
	 * 
	 * @param string
	 *            A String
	 * @return A String correctly formatted for insertion in a JSON text.
	 */
	// public static String quote(String string, StringWriter sw) {
	// return quote(string, sw).toString();
	// }

	/**
	 * Quotes a string for use in JSON text, escaping control characters and special symbols.
	 * @param string string to quote.
	 * @return quoted string.
	 */
	public static String quote(String string) {
		StringWriter w = new StringWriter((int) ((string.length() * 1.25)));
		if (string == null || string.length() == 0) {
			w.write("\"\"");
			return w.toString();
		}

		char b;
		char c = 0;
		String hhhh;
		int i;
		int len = string.length();

		w.write('"');
		for (i = 0; i < len; i += 1) {
			b = c;
			c = string.charAt(i);
			switch (c) {
			case '\\':
			case '"':
				w.write('\\');
				w.write(c);
				break;
			case '/':
				if (b == '<') {
					w.write('\\');
				}
				w.write(c);
				break;
			case '\b':
				w.write("\\b");
				break;
			case '\t':
				w.write("\\t");
				break;
			case '\n':
				w.write("\\n");
				break;
			case '\f':
				w.write("\\f");
				break;
			case '\r':
				w.write("\\r");
				break;
			// case '{':
			// case '}':
			// case '[':
			// case ']':
			// unicode(c, w);
			// break;
			default:
				if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
					w.write("\\u");
					hhhh = Integer.toHexString(c);
					w.write("0000", 0, 4 - hhhh.length());
					w.write(hhhh);
				} else {
					w.write(c);
				}
			}
		}
		w.write('"');
		return w.toString();
	}

	/**
	 * Try to convert a string into a number, boolean, or null. If the string
	 * can't be converted, return the string.
	 * 
	 * @param string
	 *            A String.
	 * @return A simple JSON value.
	 */

	private static Pattern number = Pattern.compile("^-?\\d+(\\.\\d+(E-?\\d+)?)?$");
	private static Pattern entero = Pattern.compile("^-?\\d+$");

	/**
	 * Converts a JSON literal string into its Java object equivalent (Boolean, null, or Number with type suffix).
	 * @param string source string.
	 * @return Java object or original string if no conversion matches.
	 */
	public static Object stringToValue(String string) {
		if (string.equals("")) {
			return string;
		}
		if (string.equalsIgnoreCase("true")) {
			return Boolean.TRUE;
		}
		if (string.equalsIgnoreCase("false")) {
			return Boolean.FALSE;
		}
		if (string.equalsIgnoreCase("null")) {
			return null;
		}

		/*
		 * If it might be a number, try converting it. If a number cannot be
		 * produced, then the value will just be a string.
		 */
		char t = string.charAt(string.length() - 1);
		string = string.substring(0, string.length() - 1);
		if (number.matcher(string).matches()) {
			if (entero.matcher(string).matches()) {
				switch (t) {
				case 'b':
					return Byte.decode(string);
				case 's':
					return Short.decode(string);
				case 'i':
					return Integer.decode(string);
				case 'l':
					return Long.decode(string);
				default:
					break;
				}
			} else {
				switch (t) {
				case 'd':
					return Double.parseDouble(string);
				case 'f':
					return Float.parseFloat(string);
				default:
					break;
				}
			}
		}
		return string;
	}

	/**
	 * Throw an exception if the object is a NaN or infinite number.
	 * 
	 * @param o
	 *            The object to test.
	 * @throws JSONException
	 *             If o is a non-finite number.
	 */
	public static void testValidity(Object o) throws JSONException {
		if (o != null) {
			if (o instanceof Double) {
				if (((Double) o).isInfinite() || ((Double) o).isNaN()) {
					throw new JSONException("JSON does not allow non-finite numbers.");
				}
			} else if (o instanceof Float) {
				if (((Float) o).isInfinite() || ((Float) o).isNaN()) {
					throw new JSONException("JSON does not allow non-finite numbers.");
				}
			}
		}
	}

	/**
	 * Make a JSON text of this JSONObject. For compactness, no whitespace is
	 * added. If this would not result in a syntactically correct JSON text,
	 * then null will be returned instead.
	 * <p>
	 * Warning: This method assumes that the data structure is acyclical.
	 * 
	 * @return a printable, displayable, portable, transmittable representation
	 *         of the object, beginning with <code>{</code>&nbsp;<small>(left
	 *         brace)</small> and ending with <code>}</code>&nbsp;<small>(right
	 *         brace)</small>.
	 */
	public static String toString(HashMap<String, Object> map) {
		return toString(0, map);
	}

	/**
	 * Make a prettyprinted JSON text of this JSONObject.
	 * <p>
	 * Warning: This method assumes that the data structure is acyclical.
	 * 
	 * @param indentFactor
	 *            The number of spaces to add to each level of indentation.
	 * @return a printable, displayable, portable, transmittable representation
	 *         of the object, beginning with <code>{</code>&nbsp;<small>(left
	 *         brace)</small> and ending with <code>}</code>&nbsp;<small>(right
	 *         brace)</small>.
	 * @throws JSONException
	 *             If the object contains an invalid number.
	 */
	public static String toString(int indentFactor, HashMap<String, Object> map) throws JSONException {
		StringWriter w = new StringWriter();
		return write(w, indentFactor, 0, map, new circleRef(null, null)).toString();
	}

	/**
	 * Wrap an object, if necessary. If the object is null, return the NULL
	 * object. If it is an array or collection, wrap it in a JSONArray. If it is
	 * a map, wrap it in a JSONObject. If it is a standard property (Double,
	 * String, et al) then it is already wrapped. Otherwise, if it comes from
	 * one of the java packages, turn it into a string. And if it doesn't, try
	 * to wrap it in a JSONObject. If the wrapping fails, then null is returned.
	 * 
	 * @param object
	 *            The object to wrap
	 * @return The wrapped value
	 */
	@SuppressWarnings("unchecked")
	public static Object wrap(Object object, circleRef maped) {
		try {
			if (object == null) {
				return null;
			}
			if (object instanceof String)
				return (String) object;
			if (Number.class.isAssignableFrom(object.getClass()))
				return object;
			if (object instanceof Byte || object instanceof Character || object instanceof Boolean) {
				return object;
			}

			if (object instanceof Collection) {
				return JSONArray.checkCollection((Collection<?>) object, maped);
			}
			if (object.getClass().isArray()) {
				return JSONArray.checkArray(object, object.getClass().getComponentType(), maped);
			}
			if (object instanceof Map) {
				return checkMap((HashMap<Object, Object>) object, maped);
			}
			Package objectPackage = object.getClass().getPackage();
			String objectPackageName = objectPackage != null ? objectPackage.getName() : "";
			if (objectPackageName.startsWith("java.") || objectPackageName.startsWith("javax.")
					|| object.getClass().getClassLoader() == null) {
				return object.toString();
			}
			return populateMap(object, maped, new HashSet<String>(), false);
			// return new JSONObject(object);
		} catch (Exception exception) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	static final Writer writeValue(Writer writer, Object value, int indentFactor, int indent, circleRef p) throws JSONException,
			IOException {
		if (value == null) {
			writer.write("null");
		} else if (value instanceof Map) {
			write(writer, indentFactor, indent, (HashMap<String, Object>) value, p);
		} else if (value instanceof Collection) {
			JSONArray.write(writer, indentFactor, indent, (Collection<?>) value, p);
		} else if (value.getClass().isArray()) {
			JSONArray.write(writer, indentFactor, indent,
					JSONArray.checkArray(value, value.getClass().getComponentType(), new circleRef(value, null)), p);
		} else if (value instanceof Number) {
			writer.write(numberToString((Number) value));
		} else if (value instanceof Boolean) {
			writer.write(value.toString());
		} else if (value instanceof String) {
			writer.write(quote((String) value));
		} else {
			writer.write(quote(value.toString()));
		}
		return writer;
	}

	static final void indent(Writer writer, int indent) throws IOException {
		for (int i = 0; i < indent; i += 1) {
			writer.write(' ');
		}
	}

	/**
	 * Write the contents of the JSONObject as JSON text to a writer. For
	 * compactness, no whitespace is added.
	 * <p>
	 * Warning: This method assumes that the data structure is acyclical.
	 * 
	 * @return The writer.
	 * @throws JSONException
	 */
	static Writer write(Writer writer, int indentFactor, int indent, HashMap<String, Object> map, circleRef maped)
			throws JSONException {
		try {
			boolean commanate = false;
			final int length = map.size();
			Iterator<String> keys = map.keySet().iterator();
			writer.write('{');
			b: if (length == 1) {
				Object key = keys.next();
				circleRef ptmp = maped;
				while (ptmp.hasParent())
					if (ptmp.getVal() == map.get(key))
						break b;
					else
						ptmp = ptmp.getParent();
				circleRef parent = new circleRef(map.get(key), maped);
				maped.getChilds().add(parent);
				writer.write(quote(key.toString()));
				writer.write(':');
				if (indentFactor > 0) {
					writer.write(' ');
				}
				writeValue(writer, map.get(key), indentFactor, indent, parent);
			} else if (length != 0) {
				final int newindent = indent + indentFactor;
				a: while (keys.hasNext()) {
					Object key = keys.next();
					circleRef ptmp = maped;
					while (ptmp.hasParent()) {
						if (ptmp.getVal() == map.get(key))
							continue a;
						else
							ptmp = ptmp.getParent();
					}
					circleRef parent = new circleRef(map.get(key), maped);
					maped.getChilds().add(parent);
					if (commanate) {
						writer.write(',');
					}
					if (indentFactor > 0) {
						writer.write('\n');
					}
					indent(writer, newindent);
					writer.write(quote(key.toString()));
					writer.write(':');
					if (indentFactor > 0) {
						writer.write(' ');
					}
					writeValue(writer, map.get(key), indentFactor, newindent, parent);
					commanate = true;
				}
				if (indentFactor > 0) {
					writer.write('\n');
				}
				indent(writer, indent);
			}
			writer.write('}');
			return writer;
		} catch (IOException exception) {
			throw new JSONException(exception);
		}
	}
}