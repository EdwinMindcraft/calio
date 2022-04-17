package io.github.apace100.calio.data;

import io.github.apace100.calio.ClassUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class ClassDataRegistry<T> {

	private static final HashMap<Class<?>, ClassDataRegistry<?>> REGISTRIES = new HashMap<>();

	private final Class<T> clazz;
	private SerializableDataType<Class<? extends T>> dataType;
	private SerializableDataType<List<Class<? extends T>>> listDataType;

	private final HashMap<String, Class<? extends T>> directMappings = new HashMap<>();
	private final List<String> packages = new LinkedList<>();
	private final String classSuffix;

	protected ClassDataRegistry(Class<T> cls, String classSuffix) {
		this.clazz = cls;
		this.classSuffix = classSuffix;
	}

	public void addMapping(String className, Class<?> cls) {
		this.directMappings.put(className, ClassUtil.castClass(cls));
	}

	public void addPackage(String packagePath) {
		this.packages.add(packagePath);
	}

	public SerializableDataType<Class<? extends T>> getDataType() {
		if (this.dataType == null) {
			this.dataType = this.createDataType();
		}
		return this.dataType;
	}

	public SerializableDataType<List<Class<? extends T>>> getListDataType() {
		if (this.listDataType == null) {
			this.listDataType = SerializableDataType.list(this.getDataType());
		}
		return this.listDataType;
	}

	public Optional<Class<? extends T>> mapStringToClass(String str) {
		return this.mapStringToClass(str, new StringBuilder());
	}

	public Optional<Class<? extends T>> mapStringToClass(String str, StringBuilder failedClasses) {
		if (this.directMappings.containsKey(str)) {
			return Optional.of(this.directMappings.get(str));
		}
		try {
			return Optional.of(Class.forName(str).asSubclass(this.clazz));
		} catch (Exception e0) {
			failedClasses.append(str);
		}
		for (String pkg : this.packages) {
			String full = pkg + "." + str;
			try {
				return Optional.of(Class.forName(full).asSubclass(this.clazz));
			} catch (Exception e1) {
				failedClasses.append(", ");
				failedClasses.append(full);
			}
			full = pkg + "." + transformJsonToClass(str, this.classSuffix);
			try {
				return Optional.of(Class.forName(full).asSubclass(this.clazz));
			} catch (Exception e2) {
				failedClasses.append(", ");
				failedClasses.append(full);
			}
		}
		return Optional.empty();
	}

	private SerializableDataType<Class<? extends T>> createDataType() {
		return SerializableDataType.wrap(ClassUtil.castClass(Class.class), SerializableDataTypes.STRING,
				Class::getName, str -> {
					StringBuilder failedClasses = new StringBuilder();
					Optional<Class<? extends T>> optionalClass = this.mapStringToClass(str, failedClasses);
					if (optionalClass.isPresent()) {
						return optionalClass.get();
					}
					throw new RuntimeException("Specified class does not exist: \"" + str + "\". Looked at [" + failedClasses + "]");
				});
	}

	@SuppressWarnings("unchecked")
	public static <T> Optional<ClassDataRegistry<T>> get(Class<T> cls) {
		if (REGISTRIES.containsKey(cls)) {
			return Optional.of((ClassDataRegistry<T>) REGISTRIES.get(cls));
		} else {
			return Optional.empty();
		}
	}

	public static <T> ClassDataRegistry<T> getOrCreate(Class<T> cls, String classSuffix) {
		Optional<ClassDataRegistry<T>> ocdr = get(cls);
		if (ocdr.isPresent())
			return ocdr.get();
		ClassDataRegistry<T> cdr = new ClassDataRegistry<>(cls, classSuffix);
		REGISTRIES.put(cls, cdr);
		return cdr;
	}

	private static String transformJsonToClass(String jsonName, String classSuffix) {
		StringBuilder builder = new StringBuilder();
		boolean caps = true;
		//int capsOffset = 'A' - 'a';
		for (char c : jsonName.toCharArray()) {
			if (c == '_') {
				caps = true;
				continue;
			}
			if (caps) {
				builder.append(Character.toUpperCase(c));
				caps = false;
			} else {
				builder.append(c);
			}
		}
		builder.append(classSuffix);
		return builder.toString();
	}
}

