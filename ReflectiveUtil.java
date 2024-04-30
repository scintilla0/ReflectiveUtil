import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2024 scintilla0 (<a href="https://github.com/scintilla0">https://github.com/scintilla0</a>)<br>
 * license MIT License <a href="http://www.opensource.org/licenses/mit-license.html">http://www.opensource.org/licenses/mit-license.html</a><br>
 * license GPL2 License <a href="http://www.gnu.org/licenses/gpl.html">http://www.gnu.org/licenses/gpl.html</a><br>
 * <br>
 * This class Provides an assortment of reflective operation methods.<br>
 * All catchable exceptions thrown by this class are wrapped into <b>RuntimeException</b>s.
 * @version 1.1.11 - 2024-04-30
 * @author scintilla0
 */
@SuppressWarnings("unchecked")
public class ReflectiveUtil {

	/**
	 * Gets the <b>String</b> char sequence value of the specified field of the target instance.<br>
	 * If the specified field does not exist in the target type and its super type, this method will throw a <b>NoSuchFieldException</b>.
	 * @param <ObjectType> Target type.
	 * @param object Target instance.
	 * @param fieldName Specified field name.
	 * @return Fetched <b>String</b> char sequence.
	 */
	public static <ObjectType> String getField(ObjectType object, String fieldName) {
		return getField(object, fieldName, String.class);
	}

	/**
	 * Gets the value of the specified field of the target instance.<br>
	 * Uses the get method of the instance in preference to direct field value.<br>
	 * If the specified field does not exist in the target type and its super type, this method will throw a <b>NoSuchFieldException</b>.
	 * @param <ObjectType> Target type.
	 * @param <ReturnType> Return type.
	 * @param object Target instance.
	 * @param fieldName Specified field name.
	 * @param returnClass Class object of the return type.
	 * @return Fetched <b>ReturnType</b> value.
	 */
	public static <ObjectType, ReturnType> ReturnType getField(ObjectType object, String fieldName, Class<ReturnType> returnClass) {
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
	 * @param <ObjectType> Target type.
	 * @param <ReturnType> Return type.
	 * @param object Target instance.
	 * @param field Specified field.
	 * @param returnClass Class object of the return type.
	 * @return Fetched <b>ReturnType</b> value.
	 */
	public static <ObjectType, ReturnType> ReturnType getField(ObjectType object, Field field, Class<ReturnType> returnClass) {
		if (object == null) {
			return null;
		}
		if (!returnClass.isAssignableFrom(field.getType()) && !returnClass.equals(Object.class)) {
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
	 * @param <ObjectType> Target type.
	 * @param object Target instance.
	 * @param fieldName Specified field name.
	 * @param value Value object to be set into the field.
	 */
	public static <ObjectType> void setField(ObjectType object, String fieldName, Object value) {
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
	 * @param <ObjectType> Target type.
	 * @param object Target instance.
	 * @param field Specified field.
	 * @param value Value object to be set into the field.
	 */
	public static <ObjectType> void setField(ObjectType object, Field field, Object value) {
		if (object == null) {
			return;
		}
		if (value != null && (!field.getType().isAssignableFrom(value.getClass()) && !field.getType().equals(Object.class))) {
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
	 * @param <ObjectType> Target type.
	 * @param objectClass Class object of the target type.
	 * @param fieldName Specified field name.
	 * @return Fetched field.
	 */
	public static <ObjectType> Field fetchField(Class<ObjectType> objectClass, String fieldName) {
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
	 * @param <ObjectType> Target type.
	 * @param objectClass Class object of the target type.
	 * @param methodName Specified method name.
	 * @param argumentTypes Class objects of the specified method's argument types.
	 * @return Fetched method.
	 */
	public static <ObjectType> Method fetchMethod(Class<ObjectType> objectClass, String methodName, Class<?>... argumentTypes) {
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
	 * Evaluates whether the object matches the target class.<br>
	 * @param object Target object.
	 * @param targetClass TargetClass.
	 * @return {@code true} if matches.
	 */
	public static boolean matchType(Object object, Class<?> targetClass) {
		if (targetClass.equals(byte.class) || targetClass.equals(Byte.class)) {
			return object instanceof Byte;
		} else if (targetClass.equals(short.class) || targetClass.equals(Short.class)) {
			return object instanceof Short;
		} else if (targetClass.equals(int.class) || targetClass.equals(Integer.class)) {
			return object instanceof Integer;
		} else if (targetClass.equals(long.class) || targetClass.equals(Long.class)) {
			return object instanceof Long;
		} else if (targetClass.equals(float.class) || targetClass.equals(Float.class)) {
			return object instanceof Float;
		} else if (targetClass.equals(double.class) || targetClass.equals(Double.class)) {
			return object instanceof Double;
		} else if (targetClass.equals(boolean.class) || targetClass.equals(Boolean.class)) {
			return object instanceof Boolean;
		} else if (targetClass.equals(char.class) || targetClass.equals(Character.class)) {
			return object instanceof Character;
		} else {
			return targetClass.isInstance(object);
		}
	}

	private static final List<Class<?>> TOP_SUPER_CLASSES = Collections.singletonList(Object.class);

}
