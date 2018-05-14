/*
 * MIT License
 *
 * Copyright (c) 2018 Jakub Zagórski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package co.jaqobb.namemc.api.json;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * A JSONObject is an unordered collection of name/value pairs. Its external
 * form is a string wrapped in curly braces with colons between the names and
 * values, and commas between the values and names. The internal form is an
 * object having <code>get</code> and <code>opt</code> methods for accessing
 * the values by name, and <code>put</code> methods for adding or replacing
 * values by name. The values can be any of these types: <code>Boolean</code>,
 * <code>JSONArray</code>, <code>JSONObject</code>, <code>Number</code>,
 * <code>String</code>, or the <code>JSONObject.NULL</code> object. A
 * JSONObject constructor can be used to convert an external form JSON text
 * into an internal form whose values can be retrieved with the
 * <code>get</code> and <code>opt</code> methods, or to convert values into a
 * JSON text using the <code>put</code> and <code>toString</code> methods. A
 * <code>get</code> method returns a value if one can be found, and throws an
 * exception if one cannot be found. An <code>opt</code> method returns a
 * default value instead of throwing an exception, and so is useful for
 * obtaining optional values.
 * <p>
 * The generic <code>get()</code> and <code>opt()</code> methods return an
 * object, which you can cast or query for type. There are also typed
 * <code>get</code> and <code>opt</code> methods that do type checking and type
 * coercion for you. The opt methods differ from the get methods in that they
 * do not throw. Instead, they return a specified value, such as null.
 * <p>
 * The <code>put</code> methods add or replace values in an object. For
 * example,
 * <pre>
 * myString = new JSONObject()
 *         .put(&quot;JSON&quot;, &quot;Hello, World!&quot;).toString();
 * </pre>
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
 * <li>Strings do not need to be quoted at all if they do not begin with a
 * quote or single quote, and if they do not contain leading or trailing
 * spaces, and if they do not contain any of these characters:
 * <code>{ } [ ] / \ : , #</code> and if they do not look like numbers and
 * if they are not the reserved words <code>true</code>, <code>false</code>,
 * or <code>null</code>.</li>
 * </ul>
 *
 * @author JSON.org
 * @version 2016-08-15
 */
public class JSONObject {
	/**
	 * JSONObject.NULL is equivalent to the value that JavaScript calls null,
	 * whilst Java's null is equivalent to the value that JavaScript calls
	 * undefined.
	 */
	private static final class Null {
		/**
		 * There is only intended to be a single instance of the NULL object,
		 * so the clone method returns itself.
		 *
		 * @return NULL.
		 */
		@SuppressWarnings("MethodDoesntCallSuperMethod")
		@Override
		protected final Object clone() {
			return this;
		}

		/**
		 * A Null object is equal to the null value and to itself.
		 *
		 * @param object An object to test for nullness.
		 *
		 * @return true if the object parameter is the JSONObject.NULL object or
		 * null.
		 */
		@SuppressWarnings({"Contract", "EqualsWhichDoesntCheckParameterClass"})
		@Override
		public boolean equals(Object object) {
			return object == null || object == this;
		}

		/**
		 * A Null object is equal to the null value and to itself.
		 *
		 * @return always returns 0.
		 */
		@Override
		public int hashCode() {
			return 0;
		}

		/**
		 * Get the "null" string value.
		 *
		 * @return The string "null".
		 */
		@Override
		public String toString() {
			return "null";
		}
	}

	/**
	 * The map where the JSONObject's properties are kept.
	 */
	private final Map<String, Object> map;

	/**
	 * It is sometimes more convenient and less ambiguous to have a
	 * <code>NULL</code> object than to use Java's <code>null</code> value.
	 * <code>JSONObject.NULL.equals(null)</code> returns <code>true</code>.
	 * <code>JSONObject.NULL.toString()</code> returns <code>"null"</code>.
	 */
	public static final Object NULL = new Null();

	/**
	 * Construct an empty JSONObject.
	 */
	public JSONObject() {
		// HashMap is used on purpose to ensure that elements are unordered by
		// the specification.
		// JSON tends to be a portable transfer format to allows the container
		// implementations to rearrange their items for a faster element
		// retrieval based on associative access.
		// Therefore, an implementation mustn't rely on the order of the item.
		this.map = new HashMap<>(16);
	}

	/**
	 * Construct a JSONObject from a subset of another JSONObject. An array of
	 * strings is used to identify the keys that should be copied. Missing keys
	 * are ignored.
	 *
	 * @param object a JSONObject.
	 * @param names  an array of strings.
	 */
	public JSONObject(JSONObject object, String[] names) {
		this(names.length);
		for (String name : names) {
			try {
				this.putOnce(name, object.opt(name));
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * Construct a JSONObject from a JSONTokener.
	 *
	 * @param tokener a JSONTokener object containing the source string.
	 *
	 * @throws JSONException if there is a syntax error in the source string or a
	 *                       duplicated key.
	 */
	public JSONObject(JSONTokener tokener) throws JSONException {
		this();
		char character;
		String key;
		if (tokener.nextClean() != '{') {
			throw tokener.syntaxError("A JSONObject text must begin with '{'");
		}
		while (true) {
			character = tokener.nextClean();
			switch (character) {
				case 0:
					throw tokener.syntaxError("A JSONObject text must end with '}'");
				case '}':
					return;
				default:
					tokener.back();
					key = tokener.nextValue().toString();
			}
			// The key is followed by ':'.
			character = tokener.nextClean();
			if (character != ':') {
				throw tokener.syntaxError("Expected a ':' after a key");
			}
			// Use syntaxError(..) to include error location
			if (key != null) {
				// Check if key exists
				if (this.opt(key) != null) {
					// key already exists
					throw tokener.syntaxError("Duplicate key \"" + key + "\"");
				}
				// Only add value if non-null
				Object value = tokener.nextValue();
				if (value != null) {
					this.put(key, value);
				}
			}
			// Pairs are separated by ','.
			switch (tokener.nextClean()) {
				case ';':
				case ',':
					if (tokener.nextClean() == '}') {
						return;
					}
					tokener.back();
					break;
				case '}':
					return;
				default:
					throw tokener.syntaxError("Expected a ',' or '}'");
			}
		}
	}

	/**
	 * Construct a JSONObject from a Map.
	 *
	 * @param map a map object that can be used to initialize the contents of
	 *            the JSONObject.
	 *
	 * @throws JSONException        if a value in the map is non-finite number.
	 * @throws NullPointerException if a key in the map is <code>null</code>
	 */
	public JSONObject(Map<?, ?> map) {
		if (map == null) {
			this.map = new HashMap<>(16);
		} else {
			this.map = new HashMap<>(map.size());
			for (Entry<?, ?> entry : map.entrySet()) {
				if (entry.getKey() == null) {
					throw new NullPointerException("Null key");
				}
				Object value = entry.getValue();
				if (value != null) {
					this.map.put(String.valueOf(entry.getKey()), wrap(value));
				}
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
	 * <p>
	 * The key is formed by removing the <code>"get"</code> or <code>"is"</code>
	 * prefix. If the second remaining character is not upper case, then the
	 * first character is converted to lower case.
	 * <p>
	 * Methods that are <code>static</code>, return <code>void</code>,
	 * have parameters, or are "bridge" methods, are ignored.
	 * <p>
	 * For example, if an object has a method named <code>"getName"</code>, and
	 * if the result of calling <code>object.getName()</code> is
	 * <code>"Larry Fine"</code>, then the JSONObject will contain
	 * <code>"name": "Larry Fine"</code>.
	 * <p>
	 * The {@link JSONPropertyName} annotation can be used on a bean getter to
	 * override key name used in the JSONObject. For example, using the object
	 * above with the <code>getName</code> method, if we annotated it with:
	 * <pre>
	 * &#64;JSONPropertyName("FullName")
	 * public String getName() { return this.name; }
	 * </pre>
	 * The resulting JSON object would contain <code>"FullName": "Larry Fine"</code>
	 * <p>
	 * Similarly, the {@link JSONPropertyName} annotation can be used on non-
	 * <code>get</code> and <code>is</code> methods. We can also override key
	 * name used in the JSONObject as seen below even though the field would normally
	 * be ignored:
	 * <pre>
	 * &#64;JSONPropertyName("FullName")
	 * public String fullName() { return this.name; }
	 * </pre>
	 * The resulting JSON object would contain <code>"FullName": "Larry Fine"</code>
	 * <p>
	 * The {@link JSONPropertyIgnore} annotation can be used to force the bean property
	 * to not be serialized into JSON. If both {@link JSONPropertyIgnore} and
	 * {@link JSONPropertyName} are defined on the same method, a depth comparison is
	 * performed and the one closest to the concrete class being serialized is used.
	 * If both annotations are at the same level, then the {@link JSONPropertyIgnore}
	 * annotation takes precedent and the field is not serialized.
	 * For example, the following declaration would prevent the <code>getName</code>
	 * method from being serialized:
	 * <pre>
	 * &#64;JSONPropertyName("FullName")
	 * &#64;JSONPropertyIgnore
	 * public String getName() { return this.name; }
	 * </pre>
	 * <p>
	 *
	 * @param bean an object that has getter methods that should be used to make
	 *             a JSONObject.
	 */
	public JSONObject(Object bean) {
		this();
		this.populateMap(bean);
	}

	/**
	 * Construct a JSONObject from an Object, using reflection to find the
	 * public members. The resulting JSONObject's keys will be the strings from
	 * the names array, and the values will be the field values associated with
	 * those keys in the object. If a key is not found or not visible, then it
	 * will not be copied into the new JSONObject.
	 *
	 * @param object an object that has fields that should be used to make a
	 *               JSONObject.
	 * @param names  an array of strings, the names of the fields to be obtained
	 *               from the object.
	 */
	public JSONObject(Object object, String[] names) {
		this(names.length);
		Class<?> clazz = object.getClass();
		for (String name : names) {
			try {
				this.putOpt(name, clazz.getField(name).get(object));
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * Construct a JSONObject from a source JSON text string. This is the most
	 * commonly used JSONObject constructor.
	 *
	 * @param source a string beginning with <code>{</code>&nbsp;<small>(left
	 *               brace)</small> and ending with <code>}</code>
	 *               &nbsp;<small>(right brace)</small>.
	 *
	 * @throws JSONException if there is a syntax error in the source string or a
	 *                       duplicated key.
	 */
	public JSONObject(String source) throws JSONException {
		this(new JSONTokener(source));
	}

	/**
	 * Construct a JSONObject from a ResourceBundle.
	 *
	 * @param baseName the ResourceBundle base name.
	 * @param locale   the Locale to load the ResourceBundle for.
	 *
	 * @throws JSONException if any JSONExceptions are detected.
	 */
	public JSONObject(String baseName, Locale locale) throws JSONException {
		this();
		ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale, Thread.currentThread().getContextClassLoader());
		// Iterate through the keys in the bundle.
		Enumeration<String> keys = bundle.getKeys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			if (key != null) {
				// Go through the path, ensuring that there is a nested JSONObject for each
				// segment except the last. Add the value using the last segment's name into
				// the deepest nested JSONObject.
				String[] path = ((String) key).split("\\.");
				int last = path.length - 1;
				JSONObject target = this;
				for (int index = 0; index < last; index += 1) {
					String segment = path[index];
					JSONObject nextTarget = target.optJSONObject(segment);
					if (nextTarget == null) {
						nextTarget = new JSONObject();
						target.put(segment, nextTarget);
					}
					target = nextTarget;
				}
				target.put(path[last], bundle.getString((String) key));
			}
		}
	}

	/**
	 * Constructor to specify an initial capacity of the internal map. Useful for library
	 * internal calls where we know, or at least can best guess, how big this JSONObject
	 * will be.
	 *
	 * @param initialCapacity initial capacity of the internal map.
	 */
	protected JSONObject(int initialCapacity) {
		this.map = new HashMap<>(initialCapacity);
	}

	/**
	 * Accumulate values under a key. It is similar to the put method except
	 * that if there is already an object stored under the key then a JSONArray
	 * is stored under the key to hold all of the accumulated values. If there
	 * is already a JSONArray, then the new value is appended to it. In
	 * contrast, the put method replaces the previous value.
	 * If only one value is accumulated that is not a JSONArray, then the result
	 * will be the same as using put. But if multiple values are accumulated,
	 * then the result will be like append.
	 *
	 * @param key   a key string.
	 * @param value an object to be accumulated under the key.
	 *
	 * @return this.
	 *
	 * @throws JSONException        if the value is non-finite number.
	 * @throws NullPointerException if the key is <code>null</code>.
	 */
	public JSONObject accumulate(String key, Object value) throws JSONException {
		testValidity(value);
		Object object = this.opt(key);
		if (object == null) {
			this.put(key, value instanceof JSONArray ? new JSONArray().put(value) : value);
		} else if (object instanceof JSONArray) {
			((JSONArray) object).put(value);
		} else {
			this.put(key, new JSONArray().put(object).put(value));
		}
		return this;
	}

	/**
	 * Append values to the array under a key. If the key does not exist in the
	 * JSONObject, then the key is put in the JSONObject with its value being a
	 * JSONArray containing the value parameter. If the key was already
	 * associated with a JSONArray, then the value parameter is appended to it.
	 *
	 * @param key   a key string.
	 * @param value an object to be accumulated under the key.
	 *
	 * @return this.
	 *
	 * @throws JSONException        if the value is non-finite number or if the current value associated with
	 *                              the key is not a JSONArray.
	 * @throws NullPointerException if the key is <code>null</code>.
	 */
	public JSONObject append(String key, Object value) throws JSONException {
		testValidity(value);
		Object object = this.opt(key);
		if (object == null) {
			this.put(key, new JSONArray().put(value));
		} else if (object instanceof JSONArray) {
			this.put(key, ((JSONArray) object).put(value));
		} else {
			throw new JSONException("JSONObject[" + key + "] is not a JSONArray.");
		}
		return this;
	}

	/**
	 * Produce a string from a double. The string "null" will be returned if the
	 * number is not finite.
	 *
	 * @param number a double.
	 *
	 * @return a String.
	 */
	public static String doubleToString(double number) {
		if (Double.isInfinite(number) || Double.isNaN(number)) {
			return "null";
		}
		// Shave off trailing zeros and decimal point, if possible.
		String string = Double.toString(number);
		if (string.indexOf('.') > 0 && string.indexOf('e') < 0 && string.indexOf('E') < 0) {
			while (string.endsWith("0")) {
				string = string.substring(0, string.length() - 1);
			}
			if (string.endsWith(".")) {
				string = string.substring(0, string.length() - 1);
			}
		}
		return string;
	}

	/**
	 * Get the value object associated with a key.
	 *
	 * @param key a key string.
	 *
	 * @return the object associated with the key.
	 *
	 * @throws JSONException if the key is not found.
	 */
	public Object get(String key) throws JSONException {
		if (key == null) {
			throw new JSONException("Null key.");
		}
		Object object = this.opt(key);
		if (object == null) {
			throw new JSONException("JSONObject[" + quote(key) + "] not found");
		}
		return object;
	}

	/**
	 * Get the enum value associated with a key.
	 *
	 * @param clazz the type of enum to retrieve.
	 * @param key   a key string.
	 *
	 * @return the enum value associated with the key.
	 *
	 * @throws JSONException if the key is not found or if the value cannot be converted
	 *                       to an enum.
	 */
	public <E extends Enum<E>> E getEnum(Class<E> clazz, String key) throws JSONException {
		E value = this.optEnum(clazz, key);
		if (value == null) {
			// JSONException should really take a throwable argument.
			// If it did, I would re-implement this with the Enum.valueOf
			// method and place any thrown exception in the JSONException
			throw new JSONException("JSONObject[" + quote(key) + "] is not an enum of type " + quote(clazz.getSimpleName()));
		}
		return value;
	}

	/**
	 * Get the boolean value associated with a key.
	 *
	 * @param key a key string.
	 *
	 * @return the truth.
	 *
	 * @throws JSONException if the value is not a Boolean or the String "true" or
	 *                       "false".
	 */
	public boolean getBoolean(String key) throws JSONException {
		Object object = this.get(key);
		if (object.equals(Boolean.FALSE) || (object instanceof String && ((String) object).equalsIgnoreCase("false"))) {
			return false;
		}
		if (object.equals(Boolean.TRUE) || (object instanceof String && ((String) object).equalsIgnoreCase("true"))) {
			return true;
		}
		throw new JSONException("JSONObject[" + quote(key) + "] is not a Boolean");
	}

	/**
	 * Get the BigInteger value associated with a key.
	 *
	 * @param key A key string.
	 *
	 * @return The numeric value.
	 *
	 * @throws JSONException if the key is not found or if the value cannot
	 *                       be converted to BigInteger.
	 */
	public BigInteger getBigInteger(String key) throws JSONException {
		Object object = this.get(key);
		try {
			return new BigInteger(object.toString());
		} catch (Exception exception) {
			throw new JSONException("JSONObject[" + quote(key) + "] could not be converted to BigInteger", exception);
		}
	}

	/**
	 * Get the BigDecimal value associated with a key.
	 *
	 * @param key a key string.
	 *
	 * @return the numeric value.
	 *
	 * @throws JSONException if the key is not found or if the value
	 *                       cannot be converted to BigDecimal.
	 */
	public BigDecimal getBigDecimal(String key) throws JSONException {
		Object object = this.get(key);
		if (object instanceof BigDecimal) {
			return (BigDecimal) object;
		}
		try {
			return new BigDecimal(object.toString());
		} catch (Exception exception) {
			throw new JSONException("JSONObject[" + quote(key) + "] could not be converted to BigDecimal", exception);
		}
	}

	/**
	 * Get the double value associated with a key.
	 *
	 * @param key A key string.
	 *
	 * @return The numeric value.
	 *
	 * @throws JSONException if the key is not found or if the value is not a Number
	 *                       object and cannot be converted to a number.
	 */
	public double getDouble(String key) throws JSONException {
		Object object = this.get(key);
		try {
			return object instanceof Number ? ((Number) object).doubleValue() : Double.parseDouble(object.toString());
		} catch (Exception exception) {
			throw new JSONException("JSONObject[" + quote(key) + "] is not a number", exception);
		}
	}

	/**
	 * Get the float value associated with a key.
	 *
	 * @param key a key string.
	 *
	 * @return the numeric value.
	 *
	 * @throws JSONException if the key is not found or if the value is not a Number
	 *                       object and cannot be converted to a number.
	 */
	public float getFloat(String key) throws JSONException {
		Object object = this.get(key);
		try {
			return object instanceof Number ? ((Number) object).floatValue() : Float.parseFloat(object.toString());
		} catch (Exception exception) {
			throw new JSONException("JSONObject[" + quote(key) + "] is not a number", exception);
		}
	}

	/**
	 * Get the Number value associated with a key.
	 *
	 * @param key a key string.
	 *
	 * @return the numeric value.
	 *
	 * @throws JSONException if the key is not found or if the value is not a Number
	 *                       object and cannot be converted to a number.
	 */
	public Number getNumber(String key) throws JSONException {
		Object object = this.get(key);
		try {
			if (object instanceof Number) {
				return (Number) object;
			}
			return stringToNumber(object.toString());
		} catch (Exception exception) {
			throw new JSONException("JSONObject[" + quote(key) + "] is not a number", exception);
		}
	}

	/**
	 * Get the int value associated with a key.
	 *
	 * @param key a key string.
	 *
	 * @return the integer value.
	 *
	 * @throws JSONException if the key is not found or if the value cannot be converted
	 *                       to an integer.
	 */
	public int getInt(String key) throws JSONException {
		Object object = this.get(key);
		try {
			return object instanceof Number ? ((Number) object).intValue() : Integer.parseInt((String) object);
		} catch (Exception exception) {
			throw new JSONException("JSONObject[" + quote(key) + "] is not an int", exception);
		}
	}

	/**
	 * Get the JSONArray value associated with a key.
	 *
	 * @param key a key string.
	 *
	 * @return a JSONArray which is the value.
	 *
	 * @throws JSONException if the key is not found or if the value is not a JSONArray.
	 */
	public JSONArray getJSONArray(String key) throws JSONException {
		Object object = this.get(key);
		if (object instanceof JSONArray) {
			return (JSONArray) object;
		}
		throw new JSONException("JSONObject[" + quote(key) + "] is not a JSONArray");
	}

	/**
	 * Get the JSONObject value associated with a key.
	 *
	 * @param key a key string.
	 *
	 * @return a JSONObject which is the value.
	 *
	 * @throws JSONException if the key is not found or if the value is not a JSONObject.
	 */
	public JSONObject getJSONObject(String key) throws JSONException {
		Object object = this.get(key);
		if (object instanceof JSONObject) {
			return (JSONObject) object;
		}
		throw new JSONException("JSONObject[" + quote(key) + "] is not a JSONObject");
	}

	/**
	 * Get the long value associated with a key.
	 *
	 * @param key a key string.
	 *
	 * @return the long value.
	 *
	 * @throws JSONException if the key is not found or if the value cannot be converted
	 *                       to a long.
	 */
	public long getLong(String key) throws JSONException {
		Object object = this.get(key);
		try {
			return object instanceof Number ? ((Number) object).longValue() : Long.parseLong((String) object);
		} catch (Exception exception) {
			throw new JSONException("JSONObject[" + quote(key) + "] is not a long", exception);
		}
	}

	/**
	 * Get an array of field names from a JSONObject.
	 *
	 * @return an array of field names, or null if there are no names.
	 */
	public static String[] getNames(JSONObject object) {
		int length = object.length();
		if (length == 0) {
			return null;
		}
		return object.keySet().toArray(new String[length]);
	}

	/**
	 * Get an array of field names from an Object.
	 *
	 * @return an array of field names, or null if there are no names.
	 */
	public static String[] getNames(Object object) {
		if (object == null) {
			return null;
		}
		Class<?> clazz = object.getClass();
		Field[] fields = clazz.getFields();
		int length = fields.length;
		if (length == 0) {
			return null;
		}
		String[] names = new String[length];
		for (int index = 0; index < length; index += 1) {
			names[index] = fields[index].getName();
		}
		return names;
	}

	/**
	 * Get the string associated with a key.
	 *
	 * @param key a key string.
	 *
	 * @return a string which is the value.
	 *
	 * @throws JSONException if there is no string value for the key.
	 */
	public String getString(String key) throws JSONException {
		Object object = this.get(key);
		if (object instanceof String) {
			return (String) object;
		}
		throw new JSONException("JSONObject[" + quote(key) + "] not a string");
	}

	/**
	 * Determine if the JSONObject contains a specific key.
	 *
	 * @param key a key string.
	 *
	 * @return true if the key exists in the JSONObject.
	 */
	public boolean has(String key) {
		return this.map.containsKey(key);
	}

	/**
	 * Increment a property of a JSONObject. If there is no such property,
	 * create one with a value of 1. If there is such a property, and if it is
	 * an Integer, Long, Double, or Float, then add one to it.
	 *
	 * @param key a key string.
	 *
	 * @return this.
	 *
	 * @throws JSONException if there is already a property with this name that is not an
	 *                       Integer, Long, Double, or Float.
	 */
	public JSONObject increment(String key) throws JSONException {
		Object value = this.opt(key);
		if (value == null) {
			this.put(key, 1);
		} else if (value instanceof BigInteger) {
			this.put(key, ((BigInteger) value).add(BigInteger.ONE));
		} else if (value instanceof BigDecimal) {
			this.put(key, ((BigDecimal) value).add(BigDecimal.ONE));
		} else if (value instanceof Integer) {
			this.put(key, (Integer) value + 1);
		} else if (value instanceof Long) {
			this.put(key, (Long) value + 1L);
		} else if (value instanceof Double) {
			this.put(key, (Double) value + 1.0d);
		} else if (value instanceof Float) {
			this.put(key, (Float) value + 1.0f);
		} else {
			throw new JSONException("Unable to increment [" + quote(key) + "]");
		}
		return this;
	}

	/**
	 * Determine if the value associated with the key is <code>null</code> or if there is no
	 * value.
	 *
	 * @param key a key string.
	 *
	 * @return true if there is no value associated with the key or if the value
	 * is the JSONObject.NULL object.
	 */
	public boolean isNull(String key) {
		return JSONObject.NULL.equals(this.opt(key));
	}

	/**
	 * Get an enumeration of the keys of the JSONObject. Modifying this key Set will also
	 * modify the JSONObject. Use with caution.
	 *
	 * @return an iterator of the keys.
	 *
	 * @see Set#iterator()
	 */
	public Iterator<String> keys() {
		return this.keySet().iterator();
	}

	/**
	 * Get a set of keys of the JSONObject. Modifying this key Set will also modify the
	 * JSONObject. Use with caution.
	 *
	 * @return a keySet.
	 *
	 * @see Map#keySet()
	 */
	public Set<String> keySet() {
		return this.map.keySet();
	}

	/**
	 * Get a set of entries of the JSONObject. These are raw values and may not
	 * match what is returned by the JSONObject get* and opt* functions. Modifying
	 * the returned EntrySet or the Entry objects contained therein will modify the
	 * backing JSONObject. This does not return a clone or a read-only view.
	 * Use with caution.
	 *
	 * @return an entrySet.
	 *
	 * @see Map#entrySet()
	 */
	protected Set<Entry<String, Object>> entrySet() {
		return this.map.entrySet();
	}

	/**
	 * Get the number of keys stored in the JSONObject.
	 *
	 * @return the number of keys in the JSONObject.
	 */
	public int length() {
		return this.map.size();
	}

	/**
	 * Produce a JSONArray containing the names of the elements of this
	 * JSONObject.
	 *
	 * @return a JSONArray containing the key strings, or null if the JSONObject
	 * is empty.
	 */
	public JSONArray names() {
		if (this.map.isEmpty()) {
			return null;
		}
		return new JSONArray(this.map.keySet());
	}

	/**
	 * Produce a string from a Number.
	 *
	 * @param number a Number.
	 *
	 * @return a String.
	 *
	 * @throws JSONException if number is non-finite.
	 */
	public static String numberToString(Number number) throws JSONException {
		if (number == null) {
			throw new JSONException("Null pointer");
		}
		testValidity(number);
		// Shave off trailing zeros and decimal point, if possible.
		String string = number.toString();
		if (string.indexOf('.') > 0 && string.indexOf('e') < 0 && string.indexOf('E') < 0) {
			while (string.endsWith("0")) {
				string = string.substring(0, string.length() - 1);
			}
			if (string.endsWith(".")) {
				string = string.substring(0, string.length() - 1);
			}
		}
		return string;
	}

	/**
	 * Get an optional value associated with a key.
	 *
	 * @param key a key string.
	 *
	 * @return an object which is the value, or null if there is no value.
	 */
	public Object opt(String key) {
		return key == null ? null : this.map.get(key);
	}

	/**
	 * Get the enum value associated with a key.
	 *
	 * @param clazz the type of enum to retrieve.
	 * @param key   a key string.
	 *
	 * @return the enum value associated with the key or null if not found.
	 */
	public <E extends Enum<E>> E optEnum(Class<E> clazz, String key) {
		return this.optEnum(clazz, key, null);
	}

	/**
	 * Get the enum value associated with a key.
	 *
	 * @param clazz        the type of enum to retrieve.
	 * @param key          a key string.
	 * @param defaultValue the default in case the value is not found.
	 *
	 * @return the enum value associated with the key or defaultValue
	 * if the value is not found or cannot be assigned to <code>clazz</code>
	 */
	public <E extends Enum<E>> E optEnum(Class<E> clazz, String key, E defaultValue) {
		try {
			Object value = this.opt(key);
			if (NULL.equals(value)) {
				return defaultValue;
			}
			if (clazz.isAssignableFrom(value.getClass())) {
				// we just checked it!
				@SuppressWarnings("unchecked")
				E e = (E) value;
				return e;
			}
			return Enum.valueOf(clazz, value.toString());
		} catch (IllegalArgumentException | NullPointerException exception) {
			return defaultValue;
		}
	}

	/**
	 * Get an optional boolean associated with a key. It returns false if there
	 * is no such key, or if the value is not Boolean.TRUE or the String "true".
	 *
	 * @param key a key string.
	 *
	 * @return the truth.
	 */
	public boolean optBoolean(String key) {
		return this.optBoolean(key, false);
	}

	/**
	 * Get an optional boolean associated with a key. It returns the
	 * defaultValue if there is no such key, or if it is not a Boolean or the
	 * String "true" or "false" (case insensitive).
	 *
	 * @param key          a key string.
	 * @param defaultValue the default.
	 *
	 * @return the truth.
	 */
	public boolean optBoolean(String key, boolean defaultValue) {
		Object value = this.opt(key);
		if (NULL.equals(value)) {
			return defaultValue;
		}
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		try {
			// we'll use the get anyway because it does string conversion.
			return this.getBoolean(key);
		} catch (Exception exception) {
			return defaultValue;
		}
	}

	/**
	 * Get an optional BigDecimal associated with a key, or the defaultValue if
	 * there is no such key or if its value is not a number. If the value is a
	 * string, an attempt will be made to evaluate it as a number.
	 *
	 * @param key          a key string.
	 * @param defaultValue the default.
	 *
	 * @return an object which is the value.
	 */
	public BigDecimal optBigDecimal(String key, BigDecimal defaultValue) {
		Object value = this.opt(key);
		if (NULL.equals(value)) {
			return defaultValue;
		}
		if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		}
		if (value instanceof BigInteger) {
			return new BigDecimal((BigInteger) value);
		}
		if (value instanceof Double || value instanceof Float) {
			return new BigDecimal(((Number) value).doubleValue());
		}
		if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
			return new BigDecimal(((Number) value).longValue());
		}
		// don't check if it's a string in case of unchecked Number subclasses
		try {
			return new BigDecimal(value.toString());
		} catch (Exception exception) {
			return defaultValue;
		}
	}

	/**
	 * Get an optional BigInteger associated with a key, or the defaultValue if
	 * there is no such key or if its value is not a number. If the value is a
	 * string, an attempt will be made to evaluate it as a number.
	 *
	 * @param key          a key string.
	 * @param defaultValue the default.
	 *
	 * @return an object which is the value.
	 */
	public BigInteger optBigInteger(String key, BigInteger defaultValue) {
		Object value = this.opt(key);
		if (NULL.equals(value)) {
			return defaultValue;
		}
		if (value instanceof BigInteger) {
			return (BigInteger) value;
		}
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value).toBigInteger();
		}
		if (value instanceof Double || value instanceof Float) {
			return new BigDecimal(((Number) value).doubleValue()).toBigInteger();
		}
		if (value instanceof Long || value instanceof Integer || value instanceof Short || value instanceof Byte) {
			return BigInteger.valueOf(((Number) value).longValue());
		}
		// don't check if it's a string in case of unchecked Number subclasses
		try {
			// the other opt functions handle implicit conversions, i.e.
			// jo.put("double",1.1d);
			// jo.optInt("double"); -- will return 1, not an error
			// this conversion to BigDecimal then to BigInteger is to maintain
			// that type cast support that may truncate the decimal.
			final String valueString = value.toString();
			if (isDecimalNotation(valueString)) {
				return new BigDecimal(valueString).toBigInteger();
			}
			return new BigInteger(valueString);
		} catch (Exception exception) {
			return defaultValue;
		}
	}

	/**
	 * Get an optional double associated with a key, or NaN if there is no such
	 * key or if its value is not a number. If the value is a string, an attempt
	 * will be made to evaluate it as a number.
	 *
	 * @param key a string which is the key.
	 *
	 * @return an object which is the value.
	 */
	public double optDouble(String key) {
		return this.optDouble(key, Double.NaN);
	}

	/**
	 * Get an optional double associated with a key, or the defaultValue if
	 * there is no such key or if its value is not a number. If the value is a
	 * string, an attempt will be made to evaluate it as a number.
	 *
	 * @param key          a key string.
	 * @param defaultValue the default.
	 *
	 * @return an object which is the value.
	 */
	public double optDouble(String key, double defaultValue) {
		Object value = this.opt(key);
		if (NULL.equals(value)) {
			return defaultValue;
		}
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		if (value instanceof String) {
			try {
				return Double.parseDouble((String) value);
			} catch (Exception exception) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	/**
	 * Get the optional double value associated with an index. NaN is returned
	 * if there is no value for the index, or if the value is not a number and
	 * cannot be converted to a number.
	 *
	 * @param key a key string.
	 *
	 * @return the value.
	 */
	public float optFloat(String key) {
		return this.optFloat(key, Float.NaN);
	}

	/**
	 * Get the optional double value associated with an index. The defaultValue
	 * is returned if there is no value for the index, or if the value is not a
	 * number and cannot be converted to a number.
	 *
	 * @param key          a key string.
	 * @param defaultValue the default value.
	 *
	 * @return the value.
	 */
	public float optFloat(String key, float defaultValue) {
		Object value = this.opt(key);
		if (JSONObject.NULL.equals(value)) {
			return defaultValue;
		}
		if (value instanceof Number) {
			return ((Number) value).floatValue();
		}
		if (value instanceof String) {
			try {
				return Float.parseFloat((String) value);
			} catch (Exception exception) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	/**
	 * Get an optional int value associated with a key, or zero if there is no
	 * such key or if the value is not a number. If the value is a string, an
	 * attempt will be made to evaluate it as a number.
	 *
	 * @param key a key string.
	 *
	 * @return an object which is the value.
	 */
	public int optInt(String key) {
		return this.optInt(key, 0);
	}

	/**
	 * Get an optional int value associated with a key, or the default if there
	 * is no such key or if the value is not a number. If the value is a string,
	 * an attempt will be made to evaluate it as a number.
	 *
	 * @param key          a key string.
	 * @param defaultValue the default.
	 *
	 * @return an object which is the value.
	 */
	public int optInt(String key, int defaultValue) {
		Object value = this.opt(key);
		if (NULL.equals(value)) {
			return defaultValue;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		if (value instanceof String) {
			try {
				return new BigDecimal((String) value).intValue();
			} catch (Exception exception) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	/**
	 * Get an optional JSONArray associated with a key. It returns null if there
	 * is no such key, or if its value is not a JSONArray.
	 *
	 * @param key a key string.
	 *
	 * @return a JSONArray which is the value.
	 */
	public JSONArray optJSONArray(String key) {
		Object object = this.opt(key);
		return object instanceof JSONArray ? (JSONArray) object : null;
	}

	/**
	 * Get an optional JSONObject associated with a key. It returns null if
	 * there is no such key, or if its value is not a JSONObject.
	 *
	 * @param key a key string.
	 *
	 * @return a JSONObject which is the value.
	 */
	public JSONObject optJSONObject(String key) {
		Object object = this.opt(key);
		return object instanceof JSONObject ? (JSONObject) object : null;
	}

	/**
	 * Get an optional long value associated with a key, or zero if there is no
	 * such key or if the value is not a number. If the value is a string, an
	 * attempt will be made to evaluate it as a number.
	 *
	 * @param key a key string.
	 *
	 * @return an object which is the value.
	 */
	public long optLong(String key) {
		return this.optLong(key, 0);
	}

	/**
	 * Get an optional long value associated with a key, or the default if there
	 * is no such key or if the value is not a number. If the value is a string,
	 * an attempt will be made to evaluate it as a number.
	 *
	 * @param key          a key string.
	 * @param defaultValue the default.
	 *
	 * @return an object which is the value.
	 */
	public long optLong(String key, long defaultValue) {
		Object value = this.opt(key);
		if (NULL.equals(value)) {
			return defaultValue;
		}
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		if (value instanceof String) {
			try {
				return new BigDecimal((String) value).longValue();
			} catch (Exception exception) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	/**
	 * Get an optional {@link Number} value associated with a key, or <code>null</code>
	 * if there is no such key or if the value is not a number. If the value is a string,
	 * an attempt will be made to evaluate it as a number ({@link BigDecimal}). This method
	 * would be used in cases where type coercion of the number value is unwanted.
	 *
	 * @param key a key string.
	 *
	 * @return an object which is the value.
	 */
	public Number optNumber(String key) {
		return this.optNumber(key, null);
	}

	/**
	 * Get an optional {@link Number} value associated with a key, or the default if there
	 * is no such key or if the value is not a number. If the value is a string,
	 * an attempt will be made to evaluate it as a number. This method
	 * would be used in cases where type coercion of the number value is unwanted.
	 *
	 * @param key          a key string.
	 * @param defaultValue the default.
	 *
	 * @return an object which is the value.
	 */
	public Number optNumber(String key, Number defaultValue) {
		Object value = this.opt(key);
		if (NULL.equals(value)) {
			return defaultValue;
		}
		if (value instanceof Number) {
			return (Number) value;
		}
		if (value instanceof String) {
			try {
				return stringToNumber((String) value);
			} catch (Exception exception) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	/**
	 * Get an optional string associated with a key. It returns an empty string
	 * if there is no such key. If the value is not a string and is not null,
	 * then it is converted to a string.
	 *
	 * @param key a key string.
	 *
	 * @return a string which is the value.
	 */
	public String optString(String key) {
		return this.optString(key, "");
	}

	/**
	 * Get an optional string associated with a key. It returns the defaultValue
	 * if there is no such key.
	 *
	 * @param key          a key string.
	 * @param defaultValue the default.
	 *
	 * @return a string which is the value.
	 */
	public String optString(String key, String defaultValue) {
		Object object = this.opt(key);
		return NULL.equals(object) ? defaultValue : object.toString();
	}

	/**
	 * Populates the internal map of the JSONObject with the bean properties. The
	 * bean can not be recursive.
	 *
	 * @param bean the bean.
	 *
	 * @see JSONObject#JSONObject(Object)
	 */
	private void populateMap(Object bean) {
		Class<?> clazz = bean.getClass();
		// If clazz is a System class then set includeSuperClass to false.
		boolean includeSuperClass = clazz.getClassLoader() != null;
		Method[] methods = includeSuperClass ? clazz.getMethods() : clazz.getDeclaredMethods();
		for (Method method : methods) {
			int modifiers = method.getModifiers();
			if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers) && method.getParameterTypes().length == 0 && !method.isBridge() && method.getReturnType() != Void.TYPE && this.isValidMethodName(method.getName())) {
				String key = this.getKeyNameFromMethod(method);
				if (key != null && !key.isEmpty()) {
					try {
						Object result = method.invoke(bean);
						if (result != null) {
							this.map.put(key, wrap(result));
							// we don't use the result anywhere outside of wrap
							// if it's a resource we should be sure to close it
							// after calling toString
							if (result instanceof Closeable) {
								try {
									((Closeable) result).close();
								} catch (IOException ignored) {
								}
							}
						}
					} catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ignored) {
					}
				}
			}
		}
	}

	private boolean isValidMethodName(String name) {
		return !"getClass".equals(name) && !"getDeclaringClass".equals(name);
	}

	private String getKeyNameFromMethod(Method method) {
		int ignoreDepth = getAnnotationDepth(method, JSONPropertyIgnore.class);
		if (ignoreDepth > 0) {
			int forcedNameDepth = getAnnotationDepth(method, JSONPropertyName.class);
			if (forcedNameDepth < 0 || ignoreDepth <= forcedNameDepth) {
				// the hierarchy asked to ignore, and the nearest name override
				// was higher or non-existent
				return null;
			}
		}
		JSONPropertyName annotation = getAnnotation(method, JSONPropertyName.class);
		if (annotation != null && !annotation.value().isEmpty()) {
			return annotation.value();
		}
		String key;
		final String name = method.getName();
		if (name.startsWith("get") && name.length() > 3) {
			key = name.substring(3);
		} else if (name.startsWith("is") && name.length() > 2) {
			key = name.substring(2);
		} else {
			return null;
		}
		// if the first letter in the key is not uppercase, then skip.
		// This is to maintain backwards compatibility before PR406
		// (https://github.com/stleary/JSON-java/pull/406/)
		if (Character.isLowerCase(key.charAt(0))) {
			return null;
		}
		if (key.length() == 1) {
			key = key.toLowerCase(Locale.ROOT);
		} else if (!Character.isUpperCase(key.charAt(1))) {
			key = key.substring(0, 1).toLowerCase(Locale.ROOT) + key.substring(1);
		}
		return key;
	}

	/**
	 * Searches the class hierarchy to see if the method or it's super
	 * implementations and interfaces has the annotation.
	 *
	 * @param <A>             type of the annotation.
	 * @param method          method to check.
	 * @param annotationClass annotation to look for.
	 *
	 * @return the {@link Annotation} if the annotation exists on the current method
	 * or one of it's super class definitions.
	 */
	private static <A extends Annotation> A getAnnotation(Method method, final Class<A> annotationClass) {
		// if we have invalid data the result is null
		if (method == null || annotationClass == null) {
			return null;
		}
		if (method.isAnnotationPresent(annotationClass)) {
			return method.getAnnotation(annotationClass);
		}
		// if we've already reached the Object class, return null;
		Class<?> clazz = method.getDeclaringClass();
		if (clazz.getSuperclass() == null) {
			return null;
		}
		// check directly implemented interfaces for the method being checked
		for (Class<?> interfaceClass : clazz.getInterfaces()) {
			try {
				Method interfaceMethod = interfaceClass.getMethod(method.getName(), method.getParameterTypes());
				return getAnnotation(interfaceMethod, annotationClass);
			} catch (SecurityException | NoSuchMethodException ignored) {
			}
		}
		try {
			return getAnnotation(clazz.getSuperclass().getMethod(method.getName(), method.getParameterTypes()), annotationClass);
		} catch (SecurityException | NoSuchMethodException exception) {
			return null;
		}
	}

	/**
	 * Searches the class hierarchy to see if the method or it's super
	 * implementations and interfaces has the annotation. Returns the depth of the
	 * annotation in the hierarchy.
	 *
	 * @param <A>             type of the annotation.
	 * @param method          method to check.
	 * @param annotationClass annotation to look for.
	 *
	 * @return depth of the annotation or -1 if the annotation is not on the method.
	 */
	private static int getAnnotationDepth(Method method, Class<? extends Annotation> annotationClass) {
		// if we have invalid data the result is -1
		if (method == null || annotationClass == null) {
			return -1;
		}
		if (method.isAnnotationPresent(annotationClass)) {
			return 1;
		}
		// if we've already reached the Object class, return -1;
		Class<?> claazz = method.getDeclaringClass();
		if (claazz.getSuperclass() == null) {
			return -1;
		}
		// check directly implemented interfaces for the method being checked
		for (Class<?> interfaceClazz : claazz.getInterfaces()) {
			try {
				Method interfaceMethod = interfaceClazz.getMethod(method.getName(), method.getParameterTypes());
				int annotationDepth = getAnnotationDepth(interfaceMethod, annotationClass);
				if (annotationDepth > 0) {
					// since the annotation was on the interface, add 1
					return annotationDepth + 1;
				}
			} catch (final SecurityException | NoSuchMethodException ignored) {
			}
		}
		try {
			int annotationDepth = getAnnotationDepth(claazz.getSuperclass().getMethod(method.getName(), method.getParameterTypes()), annotationClass);
			if (annotationDepth > 0) {
				// since the annotation was on the superclass, add 1
				return annotationDepth + 1;
			}
			return -1;
		} catch (SecurityException | NoSuchMethodException exception) {
			return -1;
		}
	}

	/**
	 * Put a key/boolean pair in the JSONObject.
	 *
	 * @param key   a key string.
	 * @param value a boolean which is the value.
	 *
	 * @return this.
	 *
	 * @throws JSONException        if the value is non-finite number.
	 * @throws NullPointerException if the key is <code>null</code>.
	 */
	public JSONObject put(String key, boolean value) throws JSONException {
		return this.put(key, value ? Boolean.TRUE : Boolean.FALSE);
	}

	/**
	 * Put a key/value pair in the JSONObject, where the value will be a
	 * JSONArray which is produced from a Collection.
	 *
	 * @param key   a key string.
	 * @param value a Collection value.
	 *
	 * @return this.
	 *
	 * @throws JSONException        if the value is non-finite number.
	 * @throws NullPointerException if the key is <code>null</code>.
	 */
	public JSONObject put(String key, Collection<?> value) throws JSONException {
		return this.put(key, new JSONArray(value));
	}

	/**
	 * Put a key/double pair in the JSONObject.
	 *
	 * @param key   aa key string.
	 * @param value Aadouble which is the value.
	 *
	 * @return this.
	 *
	 * @throws JSONException        if the value is non-finite number.
	 * @throws NullPointerException if the key is <code>null</code>.
	 */
	public JSONObject put(String key, double value) throws JSONException {
		return this.put(key, Double.valueOf(value));
	}

	/**
	 * Put a key/float pair in the JSONObject.
	 *
	 * @param key   a key string.
	 * @param value a float which is the value.
	 *
	 * @return this.
	 *
	 * @throws JSONException        if the value is non-finite number.
	 * @throws NullPointerException if the key is <code>null</code>.
	 */
	public JSONObject put(String key, float value) throws JSONException {
		return this.put(key, Float.valueOf(value));
	}

	/**
	 * Put a key/int pair in the JSONObject.
	 *
	 * @param key   a key string.
	 * @param value an int which is the value.
	 *
	 * @return this.
	 *
	 * @throws JSONException        if the value is non-finite number.
	 * @throws NullPointerException If the key is <code>null</code>.
	 */
	public JSONObject put(String key, int value) throws JSONException {
		return this.put(key, Integer.valueOf(value));
	}

	/**
	 * Put a key/long pair in the JSONObject.
	 *
	 * @param key   a key string.
	 * @param value a long which is the value.
	 *
	 * @return this.
	 *
	 * @throws JSONException        if the value is non-finite number.
	 * @throws NullPointerException if the key is <code>null</code>.
	 */
	public JSONObject put(String key, long value) throws JSONException {
		return this.put(key, Long.valueOf(value));
	}

	/**
	 * Put a key/value pair in the JSONObject, where the value will be a
	 * JSONObject which is produced from a Map.
	 *
	 * @param key   a key string.
	 * @param value a Map value.
	 *
	 * @return this.
	 *
	 * @throws JSONException        if the value is non-finite number.
	 * @throws NullPointerException if the key is <code>null</code>.
	 */
	public JSONObject put(String key, Map<?, ?> value) throws JSONException {
		return this.put(key, new JSONObject(value));
	}

	/**
	 * Put a key/value pair in the JSONObject. If the value is <code>null</code>, then the
	 * key will be removed from the JSONObject if it is present.
	 *
	 * @param key   a key string.
	 * @param value an object which is the value. It should be of one of these
	 *              types: Boolean, Double, Integer, JSONArray, JSONObject, Long,
	 *              String, or the JSONObject.NULL object.
	 *
	 * @return this.
	 *
	 * @throws JSONException        if the value is non-finite number.
	 * @throws NullPointerException if the key is <code>null</code>.
	 */
	public JSONObject put(String key, Object value) throws JSONException {
		if (key == null) {
			throw new NullPointerException("Null key");
		}
		if (value != null) {
			testValidity(value);
			this.map.put(key, value);
		} else {
			this.remove(key);
		}
		return this;
	}

	/**
	 * Put a key/value pair in the JSONObject, but only if the key and the value
	 * are both non-null, and only if there is not already a member with that
	 * name.
	 *
	 * @param key   string.
	 * @param value object.
	 *
	 * @return this.
	 *
	 * @throws JSONException if the key is a duplicate.
	 */
	public JSONObject putOnce(String key, Object value) throws JSONException {
		if (key != null && value != null) {
			if (this.opt(key) != null) {
				throw new JSONException("Duplicate key \"" + key + "\"");
			}
			return this.put(key, value);
		}
		return this;
	}

	/**
	 * Put a key/value pair in the JSONObject, but only if the key and the value
	 * are both non-null.
	 *
	 * @param key   A key string.
	 * @param value An object which is the value. It should be of one of these
	 *              types: Boolean, Double, Integer, JSONArray, JSONObject, Long,
	 *              String, or the JSONObject.NULL object.
	 *
	 * @return this.
	 *
	 * @throws JSONException if the value is a non-finite number.
	 */
	public JSONObject putOpt(String key, Object value) throws JSONException {
		if (key != null && value != null) {
			return this.put(key, value);
		}
		return this;
	}

	/**
	 * Creates a JSONPointer using an initialization string and tries to
	 * match it to an item within this JSONObject. For example, given a
	 * JSONObject initialized with this document:
	 * <pre>
	 * {
	 *     "a":{"b":"c"}
	 * }
	 * </pre>
	 * and this JSONPointer string:
	 * <pre>
	 * "/a/b"
	 * </pre>
	 * Then this method will return the String "c".
	 * A JSONPointerException may be thrown from code called by this method.
	 *
	 * @param pointer string that can be used to create a JSONPointer.
	 *
	 * @return the item matched by the JSONPointer, otherwise null.
	 */
	public Object query(String pointer) {
		return this.query(new JSONPointer(pointer));
	}

	/**
	 * Uses a user initialized JSONPointer  and tries to
	 * match it to an item within this JSONObject. For example, given a
	 * JSONObject initialized with this document:
	 * <pre>
	 * {
	 *     "a":{"b":"c"}
	 * }
	 * </pre>
	 * and this JSONPointer:
	 * <pre>
	 * "/a/b"
	 * </pre>
	 * Then this method will return the String "c".
	 * A JSONPointerException may be thrown from code called by this method.
	 *
	 * @param pointer string that can be used to create a JSONPointer.
	 *
	 * @return the item matched by the JSONPointer, otherwise null.
	 */
	public Object query(JSONPointer pointer) {
		return pointer.queryFrom(this);
	}

	/**
	 * Queries and returns a value from this object using {@code jsonPointer}, or
	 * returns null if the query fails due to a missing key.
	 *
	 * @param pointer the string representation of the JSON pointer.
	 *
	 * @return the queried value or {@code null}.
	 *
	 * @throws IllegalArgumentException if {@code jsonPointer} has invalid syntax.
	 */
	public Object optQuery(String pointer) {
		return this.optQuery(new JSONPointer(pointer));
	}

	/**
	 * Queries and returns a value from this object using {@code jsonPointer}, or
	 * returns null if the query fails due to a missing key.
	 *
	 * @param pointer the JSON pointer/
	 *
	 * @return the queried value or {@code null}/
	 *
	 * @throws IllegalArgumentException if {@code jsonPointer} has invalid syntax/
	 */
	public Object optQuery(JSONPointer pointer) {
		try {
			return pointer.queryFrom(this);
		} catch (JSONPointerException exception) {
			return null;
		}
	}

	/**
	 * Produce a string in double quotes with backslash sequences in all the
	 * right places. A backslash will be inserted within </, producing <\/,
	 * allowing JSON text to be delivered in HTML. In JSON text, a string cannot
	 * contain a control character or an unescaped quote or backslash.
	 *
	 * @param string a String.
	 *
	 * @return a String correctly formatted for insertion in a JSON text.
	 */
	public static String quote(String string) {
		try {
			try (StringWriter writer = new StringWriter()) {
				synchronized (writer.getBuffer()) {
					return quote(string, writer).toString();
				}
			}
		} catch (IOException ignored) {
			return "";
		}
	}

	public static Writer quote(String string, Writer writer) throws IOException {
		if (string == null || string.isEmpty()) {
			writer.write("\"\"");
			return writer;
		}
		char currentCharacter;
		char character = 0;
		String hex;
		int index;
		int length = string.length();
		writer.write('"');
		for (index = 0; index < length; index += 1) {
			currentCharacter = character;
			character = string.charAt(index);
			switch (character) {
				case '\\':
				case '"':
					writer.write('\\');
					writer.write(character);
					break;
				case '/':
					if (currentCharacter == '<') {
						writer.write('\\');
					}
					writer.write(character);
					break;
				case '\b':
					writer.write("\\b");
					break;
				case '\t':
					writer.write("\\t");
					break;
				case '\n':
					writer.write("\\n");
					break;
				case '\f':
					writer.write("\\f");
					break;
				case '\r':
					writer.write("\\r");
					break;
				default:
					if (character < ' ' || (character >= '\u0080' && character < '\u00a0') || (character >= '\u2000' && character < '\u2100')) {
						writer.write("\\u");
						hex = Integer.toHexString(character);
						writer.write("0000", 0, 4 - hex.length());
						writer.write(hex);
					} else {
						writer.write(character);
					}
			}
		}
		writer.write('"');
		return writer;
	}

	/**
	 * Remove a name and its value, if present.
	 *
	 * @param key the name to be removed.
	 *
	 * @return the value that was associated with the name, or null if there was
	 * no value.
	 */
	public Object remove(String key) {
		return this.map.remove(key);
	}

	/**
	 * Determine if two JSONObjects are similar.
	 * They must contain the same set of names which must be associated with
	 * similar values.
	 *
	 * @param other the other JSONObject
	 *
	 * @return true if they are equal.
	 */
	public boolean similar(Object other) {
		try {
			if (!(other instanceof JSONObject)) {
				return false;
			}
			if (!this.keySet().equals(((JSONObject) other).keySet())) {
				return false;
			}
			for (Entry<String, ?> entry : this.entrySet()) {
				String name = entry.getKey();
				Object valueThis = entry.getValue();
				Object valueOther = ((JSONObject) other).get(name);
				if (valueThis.equals(valueOther)) {
					continue;
				}
				if (valueThis instanceof JSONObject) {
					if (!((JSONObject) valueThis).similar(valueOther)) {
						return false;
					}
				} else if (valueThis instanceof JSONArray) {
					if (!((JSONArray) valueThis).similar(valueOther)) {
						return false;
					}
				} else if (!valueThis.equals(valueOther)) {
					return false;
				}
			}
			return true;
		} catch (Throwable exception) {
			return false;
		}
	}

	/**
	 * Tests if the value should be tried as a decimal. It makes no test if there are actual digits.
	 *
	 * @param value value to test.
	 *
	 * @return true if the string is "-0" or if it contains '.', 'e', or 'E', false otherwise.
	 */
	protected static boolean isDecimalNotation(String value) {
		return value.indexOf('.') > -1 || value.indexOf('e') > -1 || value.indexOf('E') > -1 || "-0".equals(value);
	}

	/**
	 * Converts a string to a number using the narrowest possible type. Possible
	 * returns for this function are BigDecimal, Double, BigInteger, Long, and Integer.
	 * When a Double is returned, it should always be a valid Double and not NaN or +-infinity.
	 *
	 * @param value value to convert.
	 *
	 * @return number representation of the value.
	 *
	 * @throws NumberFormatException thrown if the value is not a valid number. A public
	 *                               caller should catch this and wrap it in a {@link JSONException} if applicable.
	 */
	protected static Number stringToNumber(String value) throws NumberFormatException {
		char initial = value.charAt(0);
		if ((initial >= '0' && initial <= '9') || initial == '-') {
			// decimal representation
			if (isDecimalNotation(value)) {
				// quick dirty way to see if we need a BigDecimal instead of a Double
				// this only handles some cases of overflow or underflow
				if (value.length() > 14) {
					return new BigDecimal(value);
				}
				Double number = Double.valueOf(value);
				if (number.isInfinite() || number.isNaN()) {
					// if we can't parse it as a double, go up to BigDecimal
					// this is probably due to underflow like 4.32e-678
					// or overflow like 4.65e5324. The size of the string is small
					// but can't be held in a Double.
					return new BigDecimal(value);
				}
				return number;
			}
			// integer representation.
			// This will narrow any values to the smallest reasonable Object representation
			// (Integer, Long, or BigInteger)

			// string version
			// The compare string length method reduces GC,
			// but leads to smaller integers being placed in larger wrappers even though not
			// needed. i.e. 1,000,000,000 -> Long even though it's an Integer
			// 1,000,000,000,000,000,000 -> BigInteger even though it's a Long
			//if(val.length()<=9){
			//    return Integer.valueOf(val);
			//}
			//if(val.length()<=18){
			//    return Long.valueOf(val);
			//}
			//return new BigInteger(val);

			// BigInteger version: We use a similar bitLenth compare as
			// BigInteger#intValueExact uses. Increases GC, but objects hold
			// only what they need. i.e. Less runtime overhead if the value is
			// long lived. Which is the better tradeoff? This is closer to what's
			// in stringToValue.
			BigInteger number = new BigInteger(value);
			if (number.bitLength() <= 31) {
				return number.intValue();
			}
			if (number.bitLength() <= 63) {
				return number.longValue();
			}
			return number;
		}
		throw new NumberFormatException("Value [" + value + "] is not a valid number");
	}

	/**
	 * Try to convert a string into a number, boolean, or null. If the string
	 * can't be converted, return the string.
	 *
	 * @param string a String.
	 *
	 * @return a simple JSON value.
	 */
	// Changes to this method must be copied to the corresponding method in
	// the XML class to keep full support for Android
	public static Object stringToValue(String string) {
		if (string.isEmpty()) {
			return string;
		}
		if (string.equalsIgnoreCase("true")) {
			return Boolean.TRUE;
		}
		if (string.equalsIgnoreCase("false")) {
			return Boolean.FALSE;
		}
		if (string.equalsIgnoreCase("null")) {
			return JSONObject.NULL;
		}
		/*
		 * If it might be a number, try converting it. If a number cannot be
		 * produced, then the value will just be a string.
		 */
		char initial = string.charAt(0);
		if ((initial >= '0' && initial <= '9') || initial == '-') {
			try {
				// if we want full Big Number support this block can be replaced with:
				// return stringToNumber(string);
				if (isDecimalNotation(string)) {
					Double value = Double.valueOf(string);
					if (!value.isInfinite() && !value.isNaN()) {
						return value;
					}
				} else {
					Long value = Long.valueOf(string);
					if (string.equals(value.toString())) {
						if (value == value.intValue()) {
							return value.intValue();
						}
						return value;
					}
				}
			} catch (Exception ignored) {
			}
		}
		return string;
	}

	/**
	 * Throw an exception if the object is a NaN or infinite number.
	 *
	 * @param object the object to test.
	 *
	 * @throws JSONException if object is a non-finite number.
	 */
	public static void testValidity(Object object) throws JSONException {
		if (object != null) {
			if (object instanceof Double) {
				if (((Double) object).isInfinite() || ((Double) object).isNaN()) {
					throw new JSONException("JSON does not allow non-finite numbers");
				}
			} else if (object instanceof Float) {
				if (((Float) object).isInfinite() || ((Float) object).isNaN()) {
					throw new JSONException("JSON does not allow non-finite numbers");
				}
			}
		}
	}

	/**
	 * Produce a JSONArray containing the values of the members of this
	 * JSONObject.
	 *
	 * @param names a JSONArray containing a list of key strings. This determines
	 *              the sequence of the values in the result.
	 *
	 * @return a JSONArray of values.
	 *
	 * @throws JSONException if any of the values are non-finite numbers.
	 */
	public JSONArray toJSONArray(JSONArray names) throws JSONException {
		if (names == null || names.length() == 0) {
			return null;
		}
		JSONArray array = new JSONArray();
		for (int index = 0; index < names.length(); index += 1) {
			array.put(this.opt(names.getString(index)));
		}
		return array;
	}

	/**
	 * Make a JSON text of this JSONObject. For compactness, no whitespace is
	 * added. If this would not result in a syntactically correct JSON text,
	 * then null will be returned instead.
	 * <p><b>
	 * Warning: This method assumes that the data structure is acyclical.
	 * </b>
	 *
	 * @return a printable, displayable, portable, transmittable representation
	 * of the object, beginning with <code>{</code>&nbsp;<small>(left
	 * brace)</small> and ending with <code>}</code>&nbsp;<small>(right
	 * brace)</small>.
	 */
	@Override
	public String toString() {
		try {
			return this.toString(0);
		} catch (Exception exception) {
			return null;
		}
	}

	/**
	 * Make a pretty-printed JSON text of this JSONObject.
	 * <p>If <code>indentFactor > 0</code> and the {@link JSONObject}
	 * has only one key, then the object will be output on a single line:
	 * <pre>{@code {"key": 1}}</pre>
	 * <p>If an object has 2 or more keys, then it will be output across
	 * multiple lines: <code><pre>{
	 *  "key1": 1,
	 *  "key2": "value 2",
	 *  "key3": 3
	 * }</pre></code>
	 * <p><b>
	 * Warning: This method assumes that the data structure is acyclical.
	 * </b>
	 *
	 * @param indentFactor The number of spaces to add to each level of indentation.
	 *
	 * @return a printable, displayable, portable, transmittable representation
	 * of the object, beginning with <code>{</code>&nbsp;<small>(left
	 * brace)</small> and ending with <code>}</code>&nbsp;<small>(right
	 * brace)</small>.
	 *
	 * @throws JSONException if the object contains an invalid number.
	 */
	public String toString(int indentFactor) throws JSONException {
		try {
			try (StringWriter writer = new StringWriter()) {
				synchronized (writer.getBuffer()) {
					return this.write(writer, indentFactor, 0).toString();
				}
			}
		} catch (IOException exception) {
			return "";
		}
	}

	/**
	 * Make a JSON text of an Object value. If the object has an
	 * value.toJSONString() method, then that method will be used to produce the
	 * JSON text. The method is required to produce a strictly conforming text.
	 * If the object does not contain a toJSONString method (which is the most
	 * util case), then a text will be produced by other means. If the value
	 * is an array or Collection, then a JSONArray will be made from it and its
	 * toJSONString method will be called. If the value is a MAP, then a
	 * JSONObject will be made from it and its toJSONString method will be
	 * called. Otherwise, the value's toString method will be called, and the
	 * result will be quoted.
	 * <p>
	 * Warning: This method assumes that the data structure is acyclical.
	 *
	 * @param value The value to be serialized.
	 *
	 * @return a printable, displayable, transmittable representation of the
	 * object, beginning with <code>{</code>&nbsp;<small>(left
	 * brace)</small> and ending with <code>}</code>&nbsp;<small>(right
	 * brace)</small>.
	 *
	 * @throws JSONException if the value is or contains an invalid number.
	 */
	public static String valueToString(Object value) throws JSONException {
		// moves the implementation to JSONWriter as:
		// 1. It makes more sense to be part of the writer class
		// 2. For Android support this method is not available. By implementing it in the Writer
		//    Android users can use the writer with the built in Android JSONObject implementation.
		return JSONWriter.valueToString(value);
	}

	/**
	 * Wrap an object, if necessary. If the object is <code>null</code>, return the NULL
	 * object. If it is an array or collection, wrap it in a JSONArray. If it is
	 * a map, wrap it in a JSONObject. If it is a standard property (Double,
	 * String, et al) then it is already wrapped. Otherwise, if it comes from
	 * one of the java packages, turn it into a string. And if it doesn't, try
	 * to wrap it in a JSONObject. If the wrapping fails, then null is returned.
	 *
	 * @param object the object to wrap.
	 *
	 * @return the wrapped value.
	 */
	public static Object wrap(Object object) {
		try {
			if (object == null) {
				return NULL;
			}
			if (object instanceof JSONObject || object instanceof JSONArray || NULL.equals(object) || object instanceof JSONString || object instanceof Byte || object instanceof Character || object instanceof Short || object instanceof Integer || object instanceof Long || object instanceof Boolean || object instanceof Float || object instanceof Double || object instanceof String || object instanceof BigInteger || object instanceof BigDecimal || object instanceof Enum) {
				return object;
			}
			if (object instanceof Collection) {
				Collection<?> collection = (Collection<?>) object;
				return new JSONArray(collection);
			}
			if (object.getClass().isArray()) {
				return new JSONArray(object);
			}
			if (object instanceof Map) {
				Map<?, ?> map = (Map<?, ?>) object;
				return new JSONObject(map);
			}
			Package objectPackage = object.getClass().getPackage();
			String objectPackageName = objectPackage != null ? objectPackage.getName() : "";
			if (objectPackageName.startsWith("java.") || objectPackageName.startsWith("javax.") || object.getClass().getClassLoader() == null) {
				return object.toString();
			}
			return new JSONObject(object);
		} catch (Exception exception) {
			return null;
		}
	}

	/**
	 * Write the contents of the JSONObject as JSON text to a writer. For
	 * compactness, no whitespace is added.
	 * <p><b>
	 * Warning: This method assumes that the data structure is acyclical.
	 * </b>
	 *
	 * @return the writer.
	 */
	public Writer write(Writer writer) throws JSONException {
		return this.write(writer, 0, 0);
	}

	static Writer writeValue(Writer writer, Object value, int indentFactor, int indent) throws JSONException, IOException {
		if (value == null) {
			writer.write("null");
		} else if (value instanceof JSONString) {
			Object object;
			try {
				object = ((JSONString) value).toJSONString();
			} catch (Exception exception) {
				throw new JSONException(exception);
			}
			writer.write(object != null ? object.toString() : quote(value.toString()));
		} else if (value instanceof Number) {
			// not all Numbers may match actual JSON Numbers. i.e. fractions or Imaginary
			final String numberAsString = numberToString((Number) value);
			try {
				// Use the BigDecimal constructor for its parser to validate the format.
				@SuppressWarnings("unused")
				BigDecimal validateFormat = new BigDecimal(numberAsString);
				// Close enough to a JSON number that we will use it unquoted
				writer.write(numberAsString);
			} catch (NumberFormatException ex) {
				// The Number value is not a valid JSON number.
				// Instead we will quote it as a string
				quote(numberAsString, writer);
			}
		} else if (value instanceof Boolean) {
			writer.write(value.toString());
		} else if (value instanceof Enum<?>) {
			writer.write(quote(((Enum<?>) value).name()));
		} else if (value instanceof JSONObject) {
			((JSONObject) value).write(writer, indentFactor, indent);
		} else if (value instanceof JSONArray) {
			((JSONArray) value).write(writer, indentFactor, indent);
		} else if (value instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) value;
			new JSONObject(map).write(writer, indentFactor, indent);
		} else if (value instanceof Collection) {
			Collection<?> coll = (Collection<?>) value;
			new JSONArray(coll).write(writer, indentFactor, indent);
		} else if (value.getClass().isArray()) {
			new JSONArray(value).write(writer, indentFactor, indent);
		} else {
			quote(value.toString(), writer);
		}
		return writer;
	}

	static void indent(Writer writer, int indent) throws IOException {
		for (int index = 0; index < indent; index += 1) {
			writer.write(' ');
		}
	}

	/**
	 * Write the contents of the JSONObject as JSON text to a writer.
	 * <p>If <code>indentFactor > 0</code> and the {@link JSONObject}
	 * has only one key, then the object will be output on a single line:
	 * <pre>{@code {"key": 1}}</pre>
	 * <p>If an object has 2 or more keys, then it will be output across
	 * multiple lines: <code><pre>{
	 *  "key1": 1,
	 *  "key2": "value 2",
	 *  "key3": 3
	 * }</pre></code>
	 * <p><b>
	 * Warning: This method assumes that the data structure is acyclical.
	 * </b>
	 *
	 * @param writer       writes the serialized JSON.
	 * @param indentFactor the number of spaces to add to each level of indentation.
	 * @param indent       the indentation of the top level.
	 *
	 * @return the writer.
	 */
	public Writer write(Writer writer, int indentFactor, int indent) throws JSONException {
		try {
			boolean commanate = false;
			int length = this.length();
			writer.write('{');
			if (length == 1) {
				Entry<String, ?> entry = this.entrySet().iterator().next();
				String key = entry.getKey();
				writer.write(quote(key));
				writer.write(':');
				if (indentFactor > 0) {
					writer.write(' ');
				}
				try {
					writeValue(writer, entry.getValue(), indentFactor, indent);
				} catch (Exception e) {
					throw new JSONException("Unable to write JSONObject value for key: " + key, e);
				}
			} else if (length != 0) {
				int newIndent = indent + indentFactor;
				for (Entry<String, ?> entry : this.entrySet()) {
					if (commanate) {
						writer.write(',');
					}
					if (indentFactor > 0) {
						writer.write('\n');
					}
					indent(writer, newIndent);
					String key = entry.getKey();
					writer.write(quote(key));
					writer.write(':');
					if (indentFactor > 0) {
						writer.write(' ');
					}
					try {
						writeValue(writer, entry.getValue(), indentFactor, newIndent);
					} catch (Exception exception) {
						throw new JSONException("Unable to write JSONObject value for key: " + key, exception);
					}
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

	/**
	 * Returns a java.util.Map containing all of the entries in this object.
	 * If an entry in the object is a JSONArray or JSONObject it will also
	 * be converted.
	 * <p>
	 * Warning: This method assumes that the data structure is acyclical.
	 *
	 * @return a java.util.Map containing the entries of this object
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> results = new HashMap<>(16);
		for (Entry<String, Object> entry : this.entrySet()) {
			Object value;
			if (entry.getValue() == null || NULL.equals(entry.getValue())) {
				value = null;
			} else if (entry.getValue() instanceof JSONObject) {
				value = ((JSONObject) entry.getValue()).toMap();
			} else if (entry.getValue() instanceof JSONArray) {
				value = ((JSONArray) entry.getValue()).toList();
			} else {
				value = entry.getValue();
			}
			results.put(entry.getKey(), value);
		}
		return results;
	}
}