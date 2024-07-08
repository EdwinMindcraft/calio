package io.github.apace100.calio.registry;

import com.google.gson.*;
import io.github.apace100.calio.ClassUtil;
import io.github.apace100.calio.data.IdentifiableMultiJsonDataLoader;
import io.github.apace100.calio.data.MultiJsonDataContainer;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataType;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.edwinmindcraft.calio.common.network.packet.S2CDataObjectRegistryPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DataObjectRegistry<T extends DataObject<T>> {

	private static final HashMap<ResourceLocation, DataObjectRegistry<?>> REGISTRIES = new HashMap<>();
	private static final Set<ResourceLocation> AUTO_SYNC_SET = new HashSet<>();

	private final ResourceLocation registryId;
	private final Class<T> objectClass;

	private final HashMap<ResourceLocation, T> idToEntry = new HashMap<>();
	private final HashMap<T, ResourceLocation> entryToId = new HashMap<>();
	private final HashMap<ResourceLocation, T> staticEntries = new HashMap<>();

	private final String factoryFieldName;
	private final Supplier<DataObjectFactory<T>> defaultFactory;
	private final HashMap<ResourceLocation, DataObjectFactory<T>> factoriesById = new HashMap<>();
	private final HashMap<DataObjectFactory<T>, ResourceLocation> factoryToId = new HashMap<>();

	private SerializableDataType<T> dataType;
	private SerializableDataType<List<T>> listDataType;
	private SerializableDataType<T> registryDataType;
	private SerializableDataType<Supplier<T>> lazyDataType;

	private final Function<JsonElement, JsonElement> jsonPreprocessor;

	private Loader loader;

	private DataObjectRegistry(ResourceLocation registryId, Class<T> objectClass, String factoryFieldName, Supplier<DataObjectFactory<T>> defaultFactory, Function<JsonElement, JsonElement> jsonPreprocessor) {
		this.registryId = registryId;
		this.objectClass = objectClass;
		this.factoryFieldName = factoryFieldName;
		this.defaultFactory = defaultFactory;
		this.jsonPreprocessor = jsonPreprocessor;
	}

	private DataObjectRegistry(ResourceLocation registryId, Class<T> objectClass, String factoryFieldName, Supplier<DataObjectFactory<T>> defaultFactory, Function<JsonElement, JsonElement> jsonPreprocessor, String dataFolder, boolean useLoadingPriority, BiConsumer<ResourceLocation, Exception> errorHandler, TriConsumer<ResourceLocation, String, Exception> legacyErrorHandler, PackType resourceType) {
		this(registryId, objectClass, factoryFieldName, defaultFactory, jsonPreprocessor);
		this.loader = new Loader(dataFolder, useLoadingPriority, errorHandler, legacyErrorHandler, resourceType);
	}

	/**
	 * Returns the resource reload listener which loads the data from datapacks.
	 * This is not registered automatically, thus you need to register it, preferably
	 * in an ordered resource listener entrypoint.
	 */
	public PreparableReloadListener getLoader() {
		return this.loader;
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

	public ResourceLocation getFactoryId(DataObjectFactory<T> factory) {
		return this.factoryToId.get(factory);
	}

	public void registerFactory(ResourceLocation id, DataObjectFactory<T> factory) {
		this.factoriesById.put(id, factory);
		this.factoryToId.put(factory, id);
	}

	public void register(ResourceLocation id, T entry) {
		this.idToEntry.put(id, entry);
		this.entryToId.put(entry, id);
	}

	public void registerStatic(ResourceLocation id, T entry) {
		this.staticEntries.put(id, entry);
		this.register(id, entry);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeInt(this.idToEntry.size());
		for (Map.Entry<ResourceLocation, T> entry : this.idToEntry.entrySet()) {
			if (this.staticEntries.containsKey(entry.getKey())) {
				// Static entries are added from code by mods,
				// so they will not be synced to clients (as
				// clients are assumed to have the same mods).
				continue;
			}
			buf.writeResourceLocation(entry.getKey());
			this.sendDataObject(buf, entry.getValue());
		}
	}

	public void sendDataObject(FriendlyByteBuf buf, T t) {
		DataObjectFactory<T> factory = t.getFactory();
		buf.writeResourceLocation(this.getFactoryId(factory));
		SerializableData.Instance data = factory.toData(t);
		factory.getData().write(buf, data);
	}

	public void receive(FriendlyByteBuf buf) {
		this.receive(buf, Runnable::run);
	}

	public void receive(FriendlyByteBuf buf, Consumer<Runnable> scheduler) {
		int entryCount = buf.readInt();
		HashMap<ResourceLocation, T> entries = new HashMap<>(entryCount);
		for (int i = 0; i < entryCount; i++) {
			ResourceLocation entryId = buf.readResourceLocation();
			T entry = this.receiveDataObject(buf);
			entries.put(entryId, entry);
		}
		scheduler.accept(() -> {
			this.clear();
			entries.forEach(this::register);
		});
	}

	public T receiveDataObject(FriendlyByteBuf buf) {
		ResourceLocation factoryId = buf.readResourceLocation();
		DataObjectFactory<T> factory = this.getFactory(factoryId);
		SerializableData.Instance data = factory.getData().read(buf);
		return factory.fromData(data);
	}

	public JsonElement writeDataObject(T t) {
		return t.getFactory().getData().write(t.getFactory().toData(t));
	}


	public T readDataObject(JsonElement element) {
		if (this.jsonPreprocessor != null) {
			element = this.jsonPreprocessor.apply(element);
		}
		if (!element.isJsonObject()) {
			throw new JsonParseException("Could not read data object of type \"%s\": expected a json object.".formatted(this.registryId));
		}
		JsonObject jsonObject = element.getAsJsonObject();
		if (!jsonObject.has(this.factoryFieldName) && this.defaultFactory == null) {
			throw new JsonParseException("Could not read data object of type \"%s\": no factory identifier provided (expected key: \"%s\").".formatted(this.registryId, this.factoryFieldName));
		}
		DataObjectFactory<T> factory;
		if (jsonObject.has(this.factoryFieldName)) {
			String type = GsonHelper.getAsString(jsonObject, this.factoryFieldName);
			ResourceLocation factoryId = ResourceLocation.tryParse(type);
			if (factoryId == null)
				throw new JsonParseException("Could not read data object of type \"%s\": invalid factory identifier (id: \"%s\").".formatted(this.registryId, type));
			DataObjectFactory<T> temp = this.getFactory(factoryId);
			if (temp == null) {
				throw new JsonParseException("Could not read data object of type \"%s\": unknown factory (id: \"%s\").".formatted(this.registryId, factoryId));
			}
			factory = temp;
		} else {
			factory = this.defaultFactory.get();
			if (factory == null)
				throw new JsonParseException("Could not read data object of type \"%s\": default factory was missing.".formatted(this.registryId));
		}
		SerializableData.Instance data = factory.getData().read(jsonObject);
		return factory.fromData(data);
	}

	public void sync(ServerPlayer player) {
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		this.write(buf);
		PacketDistributor.sendToPlayer(player, new S2CDataObjectRegistryPacket(this.registryId, buf));
	}

	public void clear() {
		this.idToEntry.clear();
		this.entryToId.clear();
		this.staticEntries.forEach(this::register);
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

	public SerializableDataType<List<T>> listDataType() {
		if (this.dataType == null)
			this.dataType = this.createDataType();
		if (this.listDataType == null)
			this.listDataType = SerializableDataType.list(this.dataType);
		return this.listDataType;
	}

	public SerializableDataType<T> registryDataType() {
		if (this.registryDataType == null)
			this.registryDataType = this.createRegistryDataType();
		return this.registryDataType;
	}

	public SerializableDataType<Supplier<T>> lazyDataType() {
		if (this.lazyDataType == null)
			this.lazyDataType = this.createLazyDataType();
		return this.lazyDataType;
	}

	public SerializableDataType<Supplier<T>> createLazyDataType() {
		return SerializableDataType.wrap(ClassUtil.castClass(Supplier.class),
				SerializableDataTypes.IDENTIFIER, lazy -> this.getId(lazy.get()), id -> () -> this.get(id));
	}

	private SerializableDataType<T> createDataType() {
		return new SerializableDataType<>(this.objectClass, this::sendDataObject, this::receiveDataObject, this::readDataObject, this::writeDataObject);
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

	private class Loader extends IdentifiableMultiJsonDataLoader {

		private static final HashMap<ResourceLocation, Integer> LOADING_PRIORITIES = new HashMap<>();
		private static final Gson GSON = new GsonBuilder()
				.setPrettyPrinting()
				.create();


		private final BiConsumer<ResourceLocation, Exception> legacyErrorHandler;
		private final TriConsumer<ResourceLocation, String, Exception> errorHandler;

		private final boolean useLoadingPriority;

		public Loader(String dataFolder, boolean useLoadingPriority, @Nullable BiConsumer<ResourceLocation, Exception> legacyErrorHandler, @Nullable TriConsumer<ResourceLocation, String, Exception> errorHandler, @Nullable PackType resourceType) {
			super(GSON, dataFolder, resourceType);
			this.useLoadingPriority = useLoadingPriority;
			this.legacyErrorHandler = legacyErrorHandler;
			this.errorHandler = errorHandler;
		}

		@Override
		protected void apply(MultiJsonDataContainer prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
			DataObjectRegistry.this.clear();
			LOADING_PRIORITIES.clear();
			prepared.forEach((packName, id, je) -> {
				try {
					SerializableData.CURRENT_NAMESPACE = id.getNamespace();
					SerializableData.CURRENT_PATH = id.getPath();

					if (!(je instanceof JsonObject jo)) {
						throw new JsonSyntaxException("Expected a JSON object");
					}

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
						this.errorHandler.accept(id, packName, e);
					}
					if (this.legacyErrorHandler != null) {
						this.legacyErrorHandler.accept(id, e);
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

		private Supplier<DataObjectFactory<T>> defaultFactory;
		private Function<JsonElement, JsonElement> jsonPreprocessor;

		private String dataFolder;
		private String factoryFieldName = "type";

		@Nullable
		private BiConsumer<ResourceLocation, Exception> legacyErrorHandler;
		@Nullable
		private TriConsumer<ResourceLocation, String, Exception> errorHandler;

		@Nullable
		private PackType resourceType;

		private boolean autoSync = false;
		private boolean readFromData = false;
		private boolean useLoadingPriority;

		public Builder(ResourceLocation registryId, Class<T> objectClass) {
			this.registryId = registryId;
			this.objectClass = objectClass;
			if (DataObjectRegistry.REGISTRIES.containsKey(registryId)) {
				throw new IllegalArgumentException("A data object registry with id \"" + registryId + "\" already exists.");
			}
		}

		public Builder<T> autoSync() {
			this.autoSync = true;
			return this;
		}

		public Builder<T> defaultFactory(DataObjectFactory<T> factory) {
			this.defaultFactory = () -> factory;
			return this;
		}

		public Builder<T> defaultFactory(Supplier<? extends DataObjectFactory<T>> factory) {
			this.defaultFactory = factory::get;
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

		/**
		 *  <p>Use {@link #dataErrorHandler(TriConsumer)} instead.</p>
		 */
		@Deprecated
		public Builder<T> dataErrorHandler(BiConsumer<ResourceLocation, Exception> handler) {
			this.legacyErrorHandler = handler;
			return this;
		}

		public Builder<T> dataErrorHandler(TriConsumer<ResourceLocation, String, Exception> handler) {
			this.errorHandler = handler;
			return this;
		}

		public Builder<T> resourceType(PackType resourceType) {
			this.resourceType = resourceType;
			return this;
		}

		public DataObjectRegistry<T> buildAndRegister() {
			DataObjectRegistry<T> registry;
			if (this.readFromData) {
				registry = new DataObjectRegistry<>(this.registryId, this.objectClass, this.factoryFieldName, this.defaultFactory, this.jsonPreprocessor, this.dataFolder, this.useLoadingPriority, this.legacyErrorHandler, this.errorHandler, this.resourceType);
			} else {
				registry = new DataObjectRegistry<>(this.registryId, this.objectClass, this.factoryFieldName, this.defaultFactory, this.jsonPreprocessor);
			}
			DataObjectRegistry.REGISTRIES.put(this.registryId, registry);
			if (this.autoSync) {
				DataObjectRegistry.AUTO_SYNC_SET.add(this.registryId);
			}
			return registry;
		}
	}
}
