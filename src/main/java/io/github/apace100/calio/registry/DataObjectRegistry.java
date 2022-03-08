package io.github.apace100.calio.registry;

import com.google.gson.*;
import io.github.apace100.calio.data.MultiJsonDataLoader;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataType;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.calio.util.OrderedResourceListeners;
import io.github.edwinmindcraft.calio.common.network.CalioNetwork;
import io.github.edwinmindcraft.calio.common.network.packet.S2CDataObjectRegistryPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class DataObjectRegistry<T extends DataObject<T>> {

	private static final HashMap<ResourceLocation, DataObjectRegistry<?>> REGISTRIES = new HashMap<>();
	private static final Set<ResourceLocation> AUTO_SYNC_SET = new HashSet<>();

	private final ResourceLocation registryId;
	private final Class<T> objectClass;

	private final HashMap<ResourceLocation, T> idToEntry = new HashMap<>();
	private final HashMap<T, ResourceLocation> entryToId = new HashMap<>();

	private final String factoryFieldName;
	private final HashMap<ResourceLocation, DataObjectFactory<T>> factoriesById = new HashMap<>();
	private final HashMap<DataObjectFactory<T>, ResourceLocation> factoryToId = new HashMap<>();

	private SerializableDataType<T> dataType;
	private SerializableDataType<T> registryDataType;

	private final Function<JsonElement, JsonElement> jsonPreprocessor;

	private DataObjectRegistry(ResourceLocation registryId, Class<T> objectClass, String factoryFieldName, Function<JsonElement, JsonElement> jsonPreprocessor) {
		this.registryId = registryId;
		this.objectClass = objectClass;
		this.factoryFieldName = factoryFieldName;
		this.jsonPreprocessor = jsonPreprocessor;
	}

	private DataObjectRegistry(ResourceLocation registryId, Class<T> objectClass, String factoryFieldName, Function<JsonElement, JsonElement> jsonPreprocessor, String dataFolder, boolean useLoadingPriority, BiConsumer<ResourceLocation, Exception> errorHandler) {
		this(registryId, objectClass, factoryFieldName, jsonPreprocessor);
		Loader loader = new Loader(dataFolder, useLoadingPriority, errorHandler);
		OrderedResourceListeners.register(loader, loader.getFabricId()).complete();
	}

	public ResourceLocation getRegistryId() {
		return this.registryId;
	}

	public ResourceLocation getId(T entry) {
		return this.entryToId.get(entry);
	}

	public DataObjectFactory<T> getFactory(ResourceLocation id) {
		return this.factoriesById.get(id);
	}

	public void registerFactory(ResourceLocation id, DataObjectFactory<T> factory) {
		this.factoriesById.put(id, factory);
		this.factoryToId.put(factory, id);
	}

	public void register(ResourceLocation id, T entry) {
		this.idToEntry.put(id, entry);
		this.entryToId.put(entry, id);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeInt(this.idToEntry.size());
		for (Map.Entry<ResourceLocation, T> entry : this.idToEntry.entrySet()) {
			buf.writeResourceLocation(entry.getKey());
			this.writeDataObject(buf, entry.getValue());
		}
	}

	public void writeDataObject(FriendlyByteBuf buf, T t) {
		DataObjectFactory<T> factory = t.getFactory();
		buf.writeResourceLocation(this.factoryToId.get(factory));
		SerializableData.Instance data = factory.toData(t);
		factory.getData().write(buf, data);
	}

	public void receive(FriendlyByteBuf buf) {
		this.clear();
		int entryCount = buf.readInt();
		for (int i = 0; i < entryCount; i++) {
			ResourceLocation entryId = buf.readResourceLocation();
			T entry = this.receiveDataObject(buf);
			this.register(entryId, entry);
		}
	}

	public T receiveDataObject(FriendlyByteBuf buf) {
		ResourceLocation factoryId = buf.readResourceLocation();
		DataObjectFactory<T> factory = this.getFactory(factoryId);
		SerializableData.Instance data = factory.getData().read(buf);
		return factory.fromData(data);
	}

	public T readDataObject(JsonElement element) {
		if (this.jsonPreprocessor != null) {
			element = this.jsonPreprocessor.apply(element);
		}
		if (!element.isJsonObject()) {
			throw new JsonParseException(
					"Could not read data object of type \"" + this.registryId +
					"\": expected a json object.");
		}
		JsonObject jsonObject = element.getAsJsonObject();
		String type = GsonHelper.getAsString(jsonObject, this.factoryFieldName);
		ResourceLocation factoryId = null;
		try {
			factoryId = new ResourceLocation(type);
		} catch (ResourceLocationException e) {
			throw new JsonParseException(
					"Could not read data object of type \"" + this.registryId +
					"\": invalid factory identifier (id: \"" + factoryId + "\").", e);
		}
		if (!this.factoriesById.containsKey(factoryId)) {
			throw new JsonParseException(
					"Could not read data object of type \"" + this.registryId +
					"\": unknown factory (id: \"" + factoryId + "\").");
		}
		DataObjectFactory<T> factory = this.getFactory(factoryId);
		SerializableData.Instance data = factory.getData().read(jsonObject);
		return factory.fromData(data);
	}

	public void sync(ServerPlayer player) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		this.write(buf);
		CalioNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CDataObjectRegistryPacket(this.registryId, buf));
	}

	public void clear() {
		this.idToEntry.clear();
		this.entryToId.clear();
	}

	@Nullable
	public T get(ResourceLocation id) {
		return this.idToEntry.get(id);
	}

	public Set<ResourceLocation> getIds() {
		return this.idToEntry.keySet();
	}

	public boolean containsId(ResourceLocation id) {
		return this.idToEntry.containsKey(id);
	}

	@NotNull
	public Iterator<T> iterator() {
		return this.idToEntry.values().iterator();
	}

	public SerializableDataType<T> dataType() {
		if (this.dataType == null) {
			this.dataType = this.createDataType();
		}
		return this.dataType;
	}

	public SerializableDataType<T> registryDataType() {
		if (this.registryDataType == null) {
			this.registryDataType = this.createRegistryDataType();
		}
		return this.registryDataType;
	}

	private SerializableDataType<T> createDataType() {
		return new SerializableDataType<>(this.objectClass, this::writeDataObject, this::receiveDataObject, this::readDataObject);
	}

	private SerializableDataType<T> createRegistryDataType() {
		return SerializableDataType.wrap(this.objectClass, SerializableDataTypes.IDENTIFIER, this::getId, this::get);
	}

	public static DataObjectRegistry<?> getRegistry(ResourceLocation registryId) {
		return REGISTRIES.get(registryId);
	}

	public static void performAutoSync(ServerPlayer player) {
		for (ResourceLocation registryId : AUTO_SYNC_SET) {
			DataObjectRegistry<?> registry = getRegistry(registryId);
			registry.sync(player);
		}
	}

	private class Loader extends MultiJsonDataLoader {

		private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
		private static final HashMap<ResourceLocation, Integer> LOADING_PRIORITIES = new HashMap<>();
		private final boolean useLoadingPriority;
		private final BiConsumer<ResourceLocation, Exception> errorHandler;

		public Loader(String dataFolder, boolean useLoadingPriority, BiConsumer<ResourceLocation, Exception> errorHandler) {
			super(GSON, dataFolder);
			this.useLoadingPriority = useLoadingPriority;
			this.errorHandler = errorHandler;
		}

		@Override
		protected void apply(Map<ResourceLocation, List<JsonElement>> data, ResourceManager manager, ProfilerFiller profiler) {
			DataObjectRegistry.this.clear();
			LOADING_PRIORITIES.clear();
			data.forEach((id, jel) -> {
				for (JsonElement je : jel) {
					try {
						SerializableData.CURRENT_NAMESPACE = id.getNamespace();
						SerializableData.CURRENT_PATH = id.getPath();
						JsonObject jo = je.getAsJsonObject();
						T t = DataObjectRegistry.this.readDataObject(je);
						if (this.useLoadingPriority) {
							int loadingPriority = GsonHelper.getAsInt(jo, "loading_priority", 0);
							if (!DataObjectRegistry.this.containsId(id) || LOADING_PRIORITIES.get(id) < loadingPriority) {
								LOADING_PRIORITIES.put(id, loadingPriority);
								DataObjectRegistry.this.register(id, t);
							}
						} else {
							DataObjectRegistry.this.register(id, t);
						}
					} catch (Exception e) {
						if (this.errorHandler != null) {
							this.errorHandler.accept(id, e);
						}
					}
				}
			});
		}

		//@Override
		public ResourceLocation getFabricId() {
			return DataObjectRegistry.this.registryId;
		}
	}

	public static class Builder<T extends DataObject<T>> {

		private final ResourceLocation registryId;
		private final Class<T> objectClass;
		private String factoryFieldName = "type";
		private boolean autoSync = false;
		private Function<JsonElement, JsonElement> jsonPreprocessor;
		private String dataFolder;
		private boolean readFromData = false;
		private boolean useLoadingPriority;
		private BiConsumer<ResourceLocation, Exception> errorHandler;

		public Builder(ResourceLocation registryId, Class<T> objectClass) {
			this.registryId = registryId;
			this.objectClass = objectClass;
			if (REGISTRIES.containsKey(registryId)) {
				throw new IllegalArgumentException("A data object registry with id \"" + registryId + "\" already exists.");
			}
		}

		public Builder<T> autoSync() {
			this.autoSync = true;
			return this;
		}

		public Builder<T> jsonPreprocessor(Function<JsonElement, JsonElement> nonJsonObjectHandler) {
			this.jsonPreprocessor = nonJsonObjectHandler;
			return this;
		}

		public Builder<T> factoryFieldName(String factoryFieldName) {
			this.factoryFieldName = factoryFieldName;
			return this;
		}

		public Builder<T> readFromData(String dataFolder, boolean useLoadingPriority) {
			this.readFromData = true;
			this.dataFolder = dataFolder;
			this.useLoadingPriority = useLoadingPriority;
			return this;
		}

		public Builder<T> dataErrorHandler(BiConsumer<ResourceLocation, Exception> handler) {
			this.errorHandler = handler;
			return this;
		}

		public DataObjectRegistry<T> buildAndRegister() {
			DataObjectRegistry<T> registry;
			if (this.readFromData) {
				registry = new DataObjectRegistry<>(this.registryId, this.objectClass, this.factoryFieldName, this.jsonPreprocessor, this.dataFolder, this.useLoadingPriority, this.errorHandler);
			} else {
				registry = new DataObjectRegistry<>(this.registryId, this.objectClass, this.factoryFieldName, this.jsonPreprocessor);
			}
			REGISTRIES.put(this.registryId, registry);
			if (this.autoSync) {
				AUTO_SYNC_SET.add(this.registryId);
			}
			return registry;
		}
	}
}
