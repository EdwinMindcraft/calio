package io.github.apace100.calio;

public final class ClassUtil {
	@SuppressWarnings("unchecked")
	public static <T> Class<T> castClass(Class<?> aClass) {
		return (Class<T>) aClass;
	}

	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static <T> Class<T> get(T... array) {
		return (Class<T>) array.getClass().getComponentType();
	}
}
