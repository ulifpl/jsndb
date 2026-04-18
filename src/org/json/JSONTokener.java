package org.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

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

/**
 * A JSONTokener takes a source string and extracts characters and tokens from
 * it. It is used by the JSONObject and JSONArray constructors to parse JSON
 * source strings.
 * 
 * @author JSON.org
 * @version 2012-02-16
 */
public class JSONTokener {

	/** The current character count. */
	private long character;
	/** End of file reached. */
	private boolean eof;
	/** Current index in the source. */
	private long index;
	/** Current line in the source. */
	private long line;
	/** Previous character read. */
	private char previous;
	/** Source reader. */
	private Reader reader;
	/** Should use the previous character next. */
	private boolean usePrevious;

	/**
	 * Construct a JSONTokener from a Reader.
	 * 
	 * @param reader A reader.
	 */
	public JSONTokener(Reader reader) {
		this.reader = reader.markSupported() ? reader : new BufferedReader(reader);
		this.eof = false;
		this.usePrevious = false;
		this.previous = 0;
		this.index = 0;
		this.character = 1;
		this.line = 1;
	}

	/**
	 * Construct a JSONTokener from an InputStream.
	 * @param inputStream source stream.
	 * @throws JSONException if error reading stream.
	 */
	public JSONTokener(InputStream inputStream) throws JSONException {
		this(new InputStreamReader(inputStream));
	}

	/**
	 * Construct a JSONTokener from a string.
	 * 
	 * @param s A source string.
	 */
	public JSONTokener(String s) {
		this(new StringReader(s));
	}

	/**
	 * Back up one character. This provides a sort of lookahead capability.
	 * @throws JSONException if attempting to back up two steps.
	 */
	public void back() throws JSONException {
		if (this.usePrevious || this.index <= 0) {
			throw new JSONException("Stepping back two steps is not supported");
		}
		this.index -= 1;
		this.character -= 1;
		this.usePrevious = true;
		this.eof = false;
	}

	/**
	 * Checks if the end of source has been reached.
	 * @return true if eof.
	 */
	public boolean end() {
		return this.eof && !this.usePrevious;
	}

	/**
	 * Get the next character in the source string.
	 * 
	 * @return The next character, or 0 if past the end of the source string.
	 * @throws JSONException if error reading.
	 */
	public char next() throws JSONException {
		int c;
		if (this.usePrevious) {
			this.usePrevious = false;
			c = this.previous;
		} else {
			try {
				c = this.reader.read();
			} catch (IOException exception) {
				throw new JSONException(exception);
			}

			if (c <= 0) { // End of stream
				this.eof = true;
				c = 0;
			}
		}
		this.index += 1;
		if (this.previous == '\r') {
			this.line += 1;
			this.character = c == '\n' ? 0 : 1;
		} else if (c == '\n') {
			this.line += 1;
			this.character = 0;
		} else {
			this.character += 1;
		}
		this.previous = (char) c;
		return this.previous;
	}

	/**
	 * Get the next n characters.
	 * 
	 * @param n The number of characters to take.
	 * @return A string of n characters.
	 * @throws JSONException Substring bounds error if there are not n characters remaining.
	 */
	public String next(int n) throws JSONException {
		if (n == 0) {
			return "";
		}

		char[] chars = new char[n];
		int pos = 0;

		while (pos < n) {
			chars[pos] = this.next();
			if (this.end()) {
				throw this.syntaxError("Substring bounds error");
			}
			pos += 1;
		}
		return new String(chars);
	}

	/**
	 * Get the next char in the string, skipping whitespace.
	 * 
	 * @return A character, or 0 if there are no more characters.
	 * @throws JSONException if error occurs.
	 */
	public char nextClean() throws JSONException {
		for (;;) {
			char c = this.next();
			if (c == 0 || c > ' ') {
				return c;
			}
		}
	}

	/** Hexadecimal character digit array. */
	public final static char[] HEX = "0123456789ABCDEF".toCharArray();

	/**
	 * Appends a unicode escape sequence for a character to a buffer.
	 * @param c character to escape.
	 * @param out target buffer.
	 */
	public static void unicode(char c, StringBuffer out){
		out.append("\\u");
		int n = c;
		for (int i = 0; i < 4; ++i) {
			int digit = (n & 0xf000) >> 12;
			out.append(HEX[digit]);
			n <<= 4;
		}
	}

	/**
	 * Return the characters up to the next close quote character.
	 * 
	 * @param quote The quoting character.
	 * @return A String.
	 * @throws JSONException Unterminated string.
	 */
	public String nextString(char quote) throws JSONException {
		char c;
		StringBuffer sb = new StringBuffer();
		for (;;) {
			c = this.next();
			switch (c) {
			case 0:
			case '\n':
			case '\r':
				throw this.syntaxError("Unterminated string");
			case '\\':
				c = this.next();
				switch (c) {
				case 'b':
					sb.append('\b');
					break;
				case 't':
					sb.append('\t');
					break;
				case 'n':
					sb.append('\n');
					break;
				case 'f':
					sb.append('\f');
					break;
				case 'r':
					sb.append('\r');
					break;
				case 'u':
					sb.append((char) Integer.parseInt(this.next(4), 16));
					break;
				case '"':
				case '\'':
				case '\\':
				case '/':
					sb.append(c);
					break;
				default:
					throw this.syntaxError("Illegal escape.");
				}
				break;
			default:
				if (c == quote) {
					return sb.toString();
				}
				sb.append(c);
			}
		}
	}


	/**
	 * Get the next value. The value can be a Boolean, Double, Integer,
	 * JSONArray, JSONObject, Long, or String.
	 * 
	 * @return An object.
	 * @throws JSONException If syntax error occurs.
	 */
	public Object nextValue() throws JSONException {
		char c = this.nextClean();

		switch (c) {
		case '"':
		case '\'':
			return nextString(c);
		case '{':
			this.back();
			return JSONObject.toMap(this);
		case '[':
			this.back();
			return JSONArray.toArrayList(this, String.class);
		}

		StringBuffer sb = new StringBuffer();
		while (c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
			sb.append(c);
			c = this.next();
		}
		this.back();

		String string = sb.toString().trim();
		if ("".equals(string)) {
			throw this.syntaxError("Missing value");
		}
		return JSONObject.stringToValue(string);
	}

	/**
	 * Make a JSONException to signal a syntax error.
	 * 
	 * @param message The error message.
	 * @return A JSONException object.
	 */
	public JSONException syntaxError(String message) {
		return new JSONException(message + this.toString());
	}

	/**
	 * Make a printable string of this JSONTokener position.
	 * 
	 * @return " at {index} [character {character} line {line}]"
	 */
	public String toString() {
		return " at " + this.index + " [character " + this.character + " line " + this.line + "]";
	}
}
