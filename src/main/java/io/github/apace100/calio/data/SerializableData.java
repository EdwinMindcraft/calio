package io.github.apace100.calio.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class SerializableData extends MapCodec<SerializableData.Instance> {

	// Should be set to the current namespace of the file that is being read. Allows using * in identifiers.
	public static String CURRENT_NAMESPACE;

	// Should be set to the current path of the file that is being read. Allows using * in identifiers.
	public static String CURRENT_PATH;

	private final HashMap<String, Entry<?>> dataFields = new HashMap<>();

	public SerializableData add(String name, SerializableDataType<?> type) {
		this.dataFields.put(name, new Entry<>(type));
		return this;
	}

	public <T> SerializableData add(String name, SerializableDataType<T> type, T defaultValue) {
		this.dataFields.put(name, new Entry<>(type, defaultValue));
		return this;
	}

	public <T> SerializableData addFunctionedDefault(String name, SerializableDataType<T> type, Function<Instance, T> defaultFunction) {
		this.dataFields.put(name, new Entry<>(type, defaultFunction));
		return this;
	}

	public void write(FriendlyByteBuf buffer, Instance instance) {
		this.dataFields.forEach((name, entry) -> {
			try {
				boolean isPresent = instance.get(name) != null;
				if (entry.hasDefault && entry.defaultValue == null) {
					buffer.writeBoolean(isPresent);
				}
				if (isPresent) {
					entry.dataType.send(buffer, instance.get(name));
				}
			} catch (DataException e) {
				throw e.prepend(name);
			} catch (Exception e) {
				throw new DataException(DataException.Phase.WRITING, name, e);
			}
		});
	}

	public Instance read(FriendlyByteBuf buffer) {
		Instance instance = new Instance();
		this.dataFields.forEach((name, entry) -> {
			try {
				boolean isPresent = true;
				if (entry.hasDefault && entry.defaultValue == null) {
					isPresent = buffer.readBoolean();
				}
				instance.set(name, isPresent ? entry.dataType.receive(buffer) : null);
			} catch (DataException e) {
				throw e.prepend(name);
			} catch (Exception e) {
				throw new DataException(DataException.Phase.RECEIVING, name, e);
			}
		});
		return instance;
	}

	public Instance read(JsonObject jsonObject) {
		Instance instance = new Instance();
		this.dataFields.forEach((name, entry) -> {
			try {
				if (!jsonObject.has(name)) {
					if (entry.hasDefault()) {
						instance.set(name, entry.getDefault(instance));
					} else {
						throw new JsonSyntaxException("JSON requires field: " + name);
					}
				} else {
					instance.set(name, entry.dataType.read(jsonObject.get(name)));
				}
			} catch (DataException e) {
				throw e.prepend(name);
			} catch (Exception e) {
				throw new DataException(DataException.Phase.READING, name, e);
			}
		});
		return instance;
	}

	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops) {
		return Stream.empty();
	}

	@Override
	public <T> DataResult<Instance> decode(DynamicOps<T> ops, MapLike<T> input) {
		DataResult<MapLike<T>> map = DataResult.success(input);
		return map.flatMap(fields -> {
			DataResult<Instance> result = DataResult.success(new Instance());
			for (Map.Entry<String, Entry<?>> entry : this.dataFields.entrySet()) {
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
		input.data.forEach((key, value) -> {
			Entry entry = this.dataFields.get(key);
			prefix.add(key, entry.dataType.encodeStart(ops, value));
		});
		return prefix;
	}

	private static class Entry<T> {
		public final SerializableDataType<T> dataType;
		public final T defaultValue;
		private final Function<Instance, T> defaultFunction;
		private final boolean hasDefault;
		private final boolean hasDefaultFunction;

		public Entry(SerializableDataType<T> dataType) {
			this.dataType = dataType;
			this.defaultValue = null;
			this.defaultFunction = null;
			this.hasDefault = false;
			this.hasDefaultFunction = false;
		}

		public Entry(SerializableDataType<T> dataType, T defaultValue) {
			this.dataType = dataType;
			this.defaultValue = defaultValue;
			this.defaultFunction = null;
			this.hasDefault = true;
			this.hasDefaultFunction = false;
		}

		public Entry(SerializableDataType<T> dataType, Function<Instance, T> defaultFunction) {
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
				return this.defaultFunction.apply(dataInstance);
			} else if (this.hasDefault) {
				return this.defaultValue;
			} else {
				throw new IllegalStateException("Tried to access default value of serializable data entry, when no default was provided.");
			}
		}
	}

	public class Instance {
		private final HashMap<String, Object> data = new HashMap<>();

		public Instance() {

		}

		public boolean isPresent(String name) {
			if (SerializableData.this.dataFields.containsKey(name)) {
				Entry<?> entry = SerializableData.this.dataFields.get(name);
				if (entry.hasDefault && entry.defaultValue == null) {
					return this.get(name) != null;
				}
			}
			return true;
		}

		public void set(String name, Object value) {
			this.data.put(name, value);
		}

		public Object get(String name) {
			if (!this.data.containsKey(name)) {
				throw new RuntimeException("Tried to get field \"" + name + "\" from data, which did not exist.");
			}
			return this.data.get(name);
		}

		public int getInt(String name) {
			return (int) this.get(name);
		}

		public boolean getBoolean(String name) {
			return (boolean) this.get(name);
		}

		public float getFloat(String name) {
			return (float) this.get(name);
		}

		public double getDouble(String name) {
			return (double) this.get(name);
		}

		public String getString(String name) {
			return (String) this.get(name);
		}

		public ResourceLocation getId(String name) {
			return (ResourceLocation) this.get(name);
		}

		public AttributeModifier getModifier(String name) {
			return (AttributeModifier) this.get(name);
		}
	}
}