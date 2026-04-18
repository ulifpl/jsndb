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
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.jsndb.util.circleRef;

/**
 * A JSONArray is an ordered sequence of values. Its external text form is a
 * string wrapped in square brackets with commas separating the values.
 * 
 * @author JSON.org
 * @version 2013-04-18
 */
public class JSONArray {
	/**
	 * Parses a JSON text from a tokener into an ArrayList.
	 * 
	 * @param x JSONTokener source.
	 * @param cls target element class (unused for raw value parsing).
	 * @return populated ArrayList.
	 * @throws JSONException if syntax error occurs.
	 */
	@SuppressWarnings("unchecked")
	public static <T> ArrayList<T> toArrayList(JSONTokener x, Class<T> cls) throws JSONException {
		ArrayList<T> myArrayList = new ArrayList<>();
		if (x.nextClean() != '[') {
			throw x.syntaxError("A JSONArray text must start with '['");
		}
		if (x.nextClean() != ']') {
			x.back();
			for (;;) {
				if (x.nextClean() == ',') {
					x.back();
					myArrayList.add(null);
				} else {
					x.back();
					myArrayList.add((T) x.nextValue());
				}
				switch (x.nextClean()) {
				case ',':
					if (x.nextClean() == ']') {
						return myArrayList;
					}
					x.back();
					break;
				case ']':
					return myArrayList;
				default:
					throw x.syntaxError("Expected a ',' or ']'");
				}
			}
		}
		return myArrayList;
	}

	/**
	 * Wraps a collection's elements for JSON processing.
	 * @param collection source collection.
	 * @param maped circular reference tracker.
	 * @return new collection with wrapped values.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Collection<T> checkCollection(Collection<T> collection, circleRef maped) {
		ArrayList<T> myArrayList = new ArrayList<>();
		if (collection != null) {
			Iterator<T> iter = collection.iterator();
			while (iter.hasNext())
				myArrayList.add((T) JSONObject.wrap(iter.next(), maped));

		}
		return myArrayList;
	}

	/**
	 * Wraps an array's elements for JSON processing.
	 * @param array source array object.
	 * @param cl element class.
	 * @param maped circular reference tracker.
	 * @return ArrayList with wrapped values.
	 */
	@SuppressWarnings("unchecked")
	public static <T> ArrayList<T> checkArray(Object array, Class<T> cl, circleRef maped) {
		ArrayList<T> list = new ArrayList<>();
		int length = Array.getLength(array);
		for (int i = 0; i < length; i += 1) {
			Object obj = Array.get(array, i);
			list.add((T) JSONObject.wrap(obj, maped));
		}
		return list;
	}

	/**
	 * Converts a JSON string into an ArrayList.
	 * @param source JSON text.
	 * @param cls element class.
	 * @return populated ArrayList.
	 */
	public static <T> ArrayList<T> toArraylist(String source, Class<T> cls) {
		return toArrayList(new JSONTokener(source), cls);
	}

	/**
	 * Writes a collection of objects as a JSON array to a writer.
	 * @param writer target writer.
	 * @param indentFactor spaces for indentation.
	 * @param indent current indentation level.
	 * @param myArrayList collection to write.
	 * @param p circular reference tracker node.
	 * @return target writer.
	 * @throws JSONException if error occurs during writing.
	 */
	public static <T> Writer write(Writer writer, int indentFactor, int indent, Collection<T> myArrayList, circleRef p)
			throws JSONException {
		try {
			boolean commanate = false;
			int length = myArrayList.size();
			writer.write('[');
			Iterator<T> it = myArrayList.iterator();
			if (length == 1) {
				JSONObject.writeValue(writer, it.next(), indentFactor, indent, p);
			} else if (length != 0) {
				final int newindent = indent + indentFactor;

				while (it.hasNext()) {
					if (commanate) {
						writer.write(',');
					}
					if (indentFactor > 0) {
						writer.write('\n');
					}
					JSONObject.indent(writer, newindent);
					JSONObject.writeValue(writer, it.next(), indentFactor, newindent, p);
					commanate = true;
				}
				if (indentFactor > 0) {
					writer.write('\n');
				}
				JSONObject.indent(writer, indent);
			}
			writer.write(']');
			return writer;
		} catch (IOException e) {
			throw new JSONException(e);
		}
	}
}
