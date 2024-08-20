import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2024 scintilla0 (<a href="https://github.com/scintilla0">https://github.com/scintilla0</a>)<br>
 * license MIT License <a href="http://www.opensource.org/licenses/mit-license.html">http://www.opensource.org/licenses/mit-license.html</a><br>
 * license GPL2 License <a href="http://www.gnu.org/licenses/gpl.html">http://www.gnu.org/licenses/gpl.html</a><br>
 * <br>
 * This class Provides an assortment of reflective operation methods.<br>
 * All catchable exceptions thrown by this class are wrapped into <b>RuntimeException</b>s.
 * @version 1.1.15 - 2024-08-20
 * @author scintilla0
 */
@SuppressWarnings("unchecked")
public class ReflectiveUtil {

	/**
	 * Gets the <b>String</b> char sequence value of the specified field of the target instance.<br>
	 * If the specified field does not exist in the target type and its super type, this method will throw a <b>NoSuchFieldException</b>.
	 * @param object Target instance.
	 * @param fieldName Specified field name.
	 * @return Fetched <b>String</b> char sequence.
	 */
	public static String getField(Object object, String fieldName) {
		return getField(object, fieldName, String.class);
	}

	/**
	 * Gets the value of the specified field of the target instance.<br>
	 * Uses the get method of the instance in preference to direct field value.<br>
	 * If the specified field does not exist in the target type and its super type, this method will throw a <b>NoSuchFieldException</b>.
	 * @param <ReturnType> Return type.
	 * @param object Target instance.
	 * @param fieldName Specified field name.
	 * @param returnClass Class object of the return type.
	 * @return Fetched <b>ReturnType</b> value.
	 */
	public static <ReturnType> ReturnType getField(Object object, String fieldName, Class<ReturnType> returnClass) {
		try {
			return (ReturnType) fetchPropertyDescriptor(object.getClass(), fieldName).getReadMethod().invoke(object);
		} catch (IntrospectionException | NullPointerException | IllegalAccessException | InvocationTargetException caught) {
			Field field = fetchField(object.getClass(), fieldName);
			return getField(object, field, returnClass);
		}
	}

	/**
	 * Gets the value of the specified field of the target instance.<br>
	 * If the specified field does not exist in the target type and its super type, this method will throw a <b>NoSuchFieldException</b>.
	 * @param <ReturnType> Return type.
	 * @param object Target instance.
	 * @param field Specified field.
	 * @param returnClass Class object of the return type.
	 * @return Fetched <b>ReturnType</b> value.
	 */
	public static <ReturnType> ReturnType getField(Object object, Field field, Class<ReturnType> returnClass) {
		if (object == null) {
			return null;
		}
		if (!matchClass(field.getType(), returnClass) && !returnClass.equals(Object.class)) {
			throw new IllegalArgumentException("Incorrect return type: " + returnClass.getName());
		}
		boolean isAccessible = field.isAccessible();
		field.setAccessible(true);
		try {
			return (ReturnType) field.get(object);
		} catch (IllegalArgumentException | IllegalAccessException exception) {
			throw new RuntimeException(exception);
		} finally {
			field.setAccessible(isAccessible);
		}
	}


	/**
	 * Sets the value into the specified field of the target instance.<br>
	 * Uses the set method of the instance in preference to direct field value.<br>
	 * If the specified field does not exist in the target type and its super type, this method will throw a <b>NoSuchFieldException</b>.
	 * @param object Target instance.
	 * @param fieldName Specified field name.
	 * @param value Value object to be set into the field.
	 */
	public static void setField(Object object, String fieldName, Object value) {
		try {
			fetchPropertyDescriptor(object.getClass(), fieldName).getWriteMethod().invoke(object, value);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Incorrect value type: " + value.getClass().getName());
		} catch (IntrospectionException | NullPointerException | IllegalAccessException | InvocationTargetException caught) {
			Field field = fetchField(object.getClass(), fieldName);
			setField(object, field, value);
		}
	}

	/**
	 * Sets the value into the specified field of the target instance.<br>
	 * If the specified field does not exist in the target type and its super type, this method will throw a <b>NoSuchFieldException</b>.
	 * @param object Target instance.
	 * @param field Specified field.
	 * @param value Value object to be set into the field.
	 */
	public static void setField(Object object, Field field, Object value) {
		if (object == null) {
			return;
		}
		if (value != null && (!matchClass(value.getClass(), field.getType()) && !field.getType().equals(Object.class))) {
			throw new IllegalArgumentException("Incorrect value type: " + value.getClass().getName());
		}
		boolean isAccessible = field.isAccessible();
		field.setAccessible(true);
		try {
			field.set(object, value);
		} catch (IllegalArgumentException | IllegalAccessException exception) {
			throw new RuntimeException(exception);
		} finally {
			field.setAccessible(isAccessible);
		}
	}

	private static PropertyDescriptor fetchPropertyDescriptor(Class<?> objectClass, String fieldName) throws IntrospectionException {
		return Arrays.stream(Introspector.getBeanInfo(objectClass).getPropertyDescriptors())
				.filter(property -> property.getName().equals(fieldName)).findAny().orElse(null);
	}

	/**
	 * Fetches the specified field of the target type.<br>
	 * If the specified field does not exist in the target type and its super type, this method will throw a <b>NoSuchFieldException</b>.
	 * @param objectClass Class object of the target type.
	 * @param fieldName Specified field name.
	 * @return Fetched field.
	 */
	public static Field fetchField(Class<?> objectClass, String fieldName) {
		Field field = null;
		Class<?> superClass = objectClass;
		while (field == null && superClass != null && !TOP_SUPER_CLASSES.contains(superClass)) {
			try {
				field = superClass.getDeclaredField(fieldName);
			} catch (NoSuchFieldException | SecurityException ignored) {
			} finally {
				superClass = superClass.getSuperclass();
			}
		}
		if (field == null) {
			throw new RuntimeException(new NoSuchFieldException(fieldName));
		}
		return field;
	}

	/**
	 * Fetches the specified method of the target type.<br>
	 * If the specified method does not exist in the target type and its super type, this method will throw a <b>NoSuchMethodException</b>.
	 * @param objectClass Class object of the target type.
	 * @param methodName Specified method name.
	 * @param argumentTypes Class objects of the specified method's argument types.
	 * @return Fetched method.
	 */
	public static Method fetchMethod(Class<?> objectClass, String methodName, Class<?>... argumentTypes) {
		try {
			try {
				return objectClass.getDeclaredMethod(methodName, argumentTypes);
			} catch (NoSuchMethodException caught) {
				return objectClass.getMethod(methodName, argumentTypes);
			}
		} catch (NoSuchMethodException exception) {
			throw new RuntimeException(exception);
		}
	}

	/**
	 * Fetches the specified method of the target type and then invoke it.<br>
	 * If the specified method does not exist in the target type and its super type, this method will throw a <b>NoSuchMethodException</b>.
	 * If there is an exception occuring during invoking process, this method will throw it out.
	 * @param object Target instance.
	 * @param methodName Specified method name.
	 * @param arguments Arguments used in the specified method.
	 * @return Fetched method.
	 */
	public static Object invokeMethod(Object object, String methodName, Object... arguments) {
		try {
			Method method = null;
			try {
				Class<?>[] argumentTypes = Arrays.stream(arguments).map(argument -> argument != null ? argument.getClass() : Object.class).toArray(Class<?>[]::new);
				method = fetchMethod(object.getClass(), methodName, argumentTypes);
			} catch (RuntimeException caught) {
				if (!(caught.getCause() instanceof NoSuchMethodException)) {
					throw caught;
				}
				if ((method = looseFetchMethod(object.getClass(), methodName, arguments)) == null) {
					try {
						return looseFetchAndInvokeVarArgsMethod(object, methodName, arguments);
					} catch (NoSuchMethodException ignored) {
						throw caught;
					}
				}
			}
			return method.invoke(object, arguments);
		} catch (IllegalAccessException | InvocationTargetException exception) {
			throw new RuntimeException(exception);
		}
	}

	private static Method looseFetchMethod(Class<?> objectClass, String methodName, Object... arguments) {
		forMethod: for (Method method : Arrays.stream(objectClass.getMethods()).filter(method -> method.getName().equals(methodName)).toArray(Method[]::new)) {
			if (method.getParameterTypes().length == arguments.length) {
				for (int index = 0; index < method.getParameterTypes().length; index ++) {
					if (!matchType(arguments[index], method.getParameterTypes()[index])) {
						continue forMethod;
					}
				}
				return method;
			}
		}
		return null;
	}

	private static Object looseFetchAndInvokeVarArgsMethod(Object object, String methodName, Object... arguments) throws NoSuchMethodException {
		forMethod: for (Method method : Arrays.stream(object.getClass().getMethods()).filter(method -> method.getName().equals(methodName)).toArray(Method[]::new)) {
			if (method.isVarArgs()) {
				int parameterLength = method.getParameterTypes().length;
				Object[] fullArguments = new Object[parameterLength];
				for (int index = 0; index < parameterLength - 1; index ++) {
					if (!matchClass(arguments[index] != null ? arguments[index].getClass() : Object.class, method.getParameterTypes()[index])) {
						continue forMethod;
					}
					fullArguments[index] = arguments[index];
				}
				Object[] varArguments = new Object[arguments.length - parameterLength + 1];
				Class<?> varArgumentsType = method.getParameterTypes()[parameterLength - 1].getComponentType();
				for (int index = parameterLength - 1, varIndex = 0; index < arguments.length; index ++, varIndex ++) {
					if (!matchType(arguments[index], varArgumentsType)) {
						continue forMethod;
					}
					varArguments[varIndex] = arguments[index];
				}
				fullArguments[parameterLength - 1] = varArguments;
				try {
					return method.invoke(object, fullArguments);
				} catch (IllegalAccessException |  InvocationTargetException exception) {
					throw new RuntimeException(exception);
				}
			}
		}
		throw new NoSuchMethodException();
	}

	/**
	 * Creates an instance of the specified type using its no-argument constructor.<br>
	 * If the target class is not a static class, this method will throw a wrapped runtime exception.<br>
	 * If the target class does not have a public no-argument constructor, this method will throw a wrapped runtime exception.
	 * @param <ObjectType> Target type.
	 * @param objectClass Class object of the target type.
	 * @return An instance of the target type.
	 */
	public static <ObjectType> ObjectType createInstance(Class<ObjectType> objectClass) {
		try {
			Constructor<ObjectType> constructor = objectClass.getDeclaredConstructor();
			boolean isAccessible = constructor.isAccessible();
			constructor.setAccessible(true);
			try {
				return constructor.newInstance();
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
				throw new RuntimeException(exception);
			} finally {
				constructor.setAccessible(isAccessible);
			}
		} catch (NoSuchMethodException exception) {
			throw new RuntimeException(exception);
		}
	}

	/**
	 * Evaluates whether the object class matches the target class.<br>
	 * @param objectClass Class of the target object.
	 * @param targetClass Target class.
	 * @return {@code true} if matches.
	 */
	public static boolean matchClass(Class<?> objectClass, Class<?> targetClass) {
		for (Map.Entry<Class<?>, Class<?>> entry : BASIC_CLASS_MAP.entrySet()) {
			if (targetClass.equals(entry.getKey()) || targetClass.equals(entry.getValue())) {
				return entry.getValue().isAssignableFrom(objectClass) || targetClass.isAssignableFrom(objectClass);
			}
		}
		return targetClass.isAssignableFrom(objectClass);
	}

	/**
	 * Evaluates whether the object matches the target class.<br>
	 * @param object Target object.
	 * @param targetClass Target class.
	 * @return {@code true} if matches.
	 */
	public static boolean matchType(Object object, Class<?> targetClass) {
		for (Map.Entry<Class<?>, Class<?>> entry : BASIC_CLASS_MAP.entrySet()) {
			if (targetClass.equals(entry.getKey()) || targetClass.equals(entry.getValue())) {
				return entry.getKey().isInstance(object);
			}
		}
		return targetClass.isInstance(object);
	}

	private static final List<Class<?>> TOP_SUPER_CLASSES = Collections.singletonList(Object.class);
	private static final Map<Class<?>, Class<?>> BASIC_CLASS_MAP;

	static {
		Map<Class<?>, Class<?>> basicClassMap = new HashMap<>();
		basicClassMap.put(Byte.class, byte.class);
		basicClassMap.put(Short.class, short.class);
		basicClassMap.put(Integer.class, int.class);
		basicClassMap.put(Long.class, long.class);
		basicClassMap.put(Float.class, float.class);
		basicClassMap.put(Double.class, double.class);
		basicClassMap.put(Boolean.class, boolean.class);
		basicClassMap.put(Character.class, char.class);
		BASIC_CLASS_MAP = Collections.unmodifiableMap(basicClassMap);
	}

}
