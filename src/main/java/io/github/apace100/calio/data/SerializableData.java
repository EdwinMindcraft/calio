package io.github.apace100.calio.data;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class SerializableData extends MapCodec<SerializableData.Instance> {

	// Should be set to the current namespace of the file that is being read. Allows using * in identifiers.
	public static String CURRENT_NAMESPACE;

	// Should be set to the current path of the file that is being read. Allows using * in identifiers.
	public static String CURRENT_PATH;

	private final LinkedHashMap<String, Field<?>> dataFields = new LinkedHashMap<>();

	public SerializableData add(String name, SerializableDataType<?> type) {
		this.dataFields.put(name, new Field<>(type));
		return this;
	}

	public <T> SerializableData add(String name, SerializableDataType<T> type, T defaultValue) {
		this.dataFields.put(name, new Field<>(type, defaultValue));
		return this;
	}

	public <T> SerializableData addFunctionedDefault(String name, SerializableDataType<T> type, Function<Instance, T> defaultFunction) {
		this.dataFields.put(name, new Field<>(type, defaultFunction));
		return this;
	}

	public void write(FriendlyByteBuf buffer, Instance instance) {
		buffer.writeWithCodec(this.codec(), instance);
	}

	public Instance read(FriendlyByteBuf buffer) {
		return buffer.readWithCodec(this.codec());
	}

	public Instance read(JsonObject jsonObject) {
		Instance instance = new Instance();
		this.dataFields.forEach((name, field) -> {
			try {
				if (!jsonObject.has(name)) {
					if (field.hasDefault()) {
						instance.set(name, field.getDefault(instance));
					} else {
						throw new JsonSyntaxException("JSON requires field: " + name);
					}
				} else {
					instance.set(name, field.dataType.read(jsonObject.get(name)));
				}
			} catch (DataException e) {
				throw e.prepend(name);
			} catch (Exception e) {
				throw new DataException(DataException.Phase.READING, name, e);
			}
		});
		return instance;
	}

	public SerializableData copy() {
		SerializableData copy = new SerializableData();
		copy.dataFields.putAll(this.dataFields);
		return copy;
	}

	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		return Stream.empty();
	}

	public Iterable<String> getFieldNames() {
		return ImmutableSet.copyOf(this.dataFields.keySet());
	}

	public Field<?> getField(String fieldName) {
		if (!this.dataFields.containsKey(fieldName)) {
			throw new IllegalArgumentException("SerializableData contains no field with name \"" + fieldName + "\".");
		} else {
			return this.dataFields.get(fieldName);
		}
	}


	@Override
	public <T> DataResult<Instance> decode(DynamicOps<T> ops, MapLike<T> input) {
		DataResult<MapLike<T>> map = DataResult.success(input);
		return map.flatMap(fields -> {
			DataResult<Instance> result = DataResult.success(new Instance());
			for (Map.Entry<String, Field<?>> entry : this.dataFields.entrySet()) {
				result = result.flatMap(x -> {
					T t = fields.get(entry.getKey());
					if (t == null) {
						if (!entry.getValue().hasDefault())
							return DataResult.error("Missing required field: " + entry.getKey());
						x.set(entry.getKey(), entry.getValue().getDefault(x));
						return DataResult.success(x);
					} else {
						return entry.getValue().dataType.decode(ops, t).map(Pair::getFirst).map(obj -> {
							x.set(entry.getKey(), obj);
							return x;
						});
					}
				});
			}
			return result;
		});
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public <T> RecordBuilder<T> encode(Instance input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
		for (Map.Entry<String, Object> data : input.data.entrySet()) {
			String key = data.getKey();
			Object value = data.getValue();
			Field field = this.dataFields.get(key);
			if (value != null)
				prefix.add(key, field.dataType.encodeStart(ops, value));
			else if (!field.hasDefault())
				prefix.add(key, DataResult.error("Missing required field: " + key));
		}
		return prefix;
	}

	public static class Field<T> {
		private final SerializableDataType<T> dataType;
		private final T defaultValue;
		private final Function<Instance, T> defaultFunction;
		private final boolean hasDefault;
		private final boolean hasDefaultFunction;

		public Field(SerializableDataType<T> dataType) {
			this.dataType = dataType;
			this.defaultValue = null;
			this.defaultFunction = null;
			this.hasDefault = false;
			this.hasDefaultFunction = false;
		}

		public Field(SerializableDataType<T> dataType, @Nullable T defaultValue) {
			this.dataType = dataType;
			this.defaultValue = defaultValue;
			this.defaultFunction = null;
			this.hasDefault = true;
			this.hasDefaultFunction = false;
		}

		public Field(SerializableDataType<T> dataType, @NotNull Function<Instance, T> defaultFunction) {
			this.dataType = dataType;
			this.defaultValue = null;
			this.defaultFunction = defaultFunction;
			this.hasDefault = false;
			this.hasDefaultFunction = true;
		}

		public boolean hasDefault() {
			return this.hasDefault || this.hasDefaultFunction;
		}

		public T getDefault(Instance dataInstance) {
			if (this.hasDefaultFunction) {
				Validate.notNull(this.defaultFunction, "Serializable data field was marked has having a default function, but had none.");
				return this.defaultFunction.apply(dataInstance);
			} else if (this.hasDefault) {
				return this.defaultValue;
			} else {
				throw new IllegalStateException("Tried to access default value of serializable data entry, when no default was provided.");
			}
		}

		public SerializableDataType<T> getDataType() {
			return this.dataType;
		}
	}

	public class Instance {
		private final HashMap<String, Object> data = new HashMap<>();

		public Instance() {

		}

		public boolean isPresent(String name) {
			if (SerializableData.this.dataFields.containsKey(name)) {
				Field<?> field = SerializableData.this.dataFields.get(name);
				if (field.hasDefault && field.defaultValue == null) {
					return this.get(name) != null;
				}
			}
			return data.containsKey(name);
		}

		public <T> void ifPresent(String name, Consumer<T> consumer) {
			if (this.isPresent(name))
				consumer.accept(this.get(name));
		}

		public void set(String name, Object value) {
			this.data.put(name, value);
		}

		@SuppressWarnings("unchecked")
		public <T> T get(String name) {
			if (!this.data.containsKey(name)) {
				throw new RuntimeException("Tried to get field \"" + name + "\" from data, which did not exist.");
			}
			return (T) this.data.get(name);
		}

		public int getInt(String name) {
			return this.get(name);
		}

		public boolean getBoolean(String name) {
			return this.get(name);
		}

		public float getFloat(String name) {
			return this.get(name);
		}

		public double getDouble(String name) {
			return this.get(name);
		}

		public String getString(String name) {
			return this.get(name);
		}

		public ResourceLocation getId(String name) {
			return this.get(name);
		}

		public AttributeModifier getModifier(String name) {
			return this.get(name);
		}
	}
}