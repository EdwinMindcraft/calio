package io.github.apace100.calio.data;

import com.google.gson.*;
import com.google.gson.internal.LazilyParsedNumber;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.ClassUtil;
import io.github.apace100.calio.SerializationHelper;
import io.github.apace100.calio.util.ArgumentWrapper;
import io.github.apace100.calio.util.StatusEffectChance;
import io.github.apace100.calio.util.TagLike;
import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import io.github.edwinmindcraft.calio.common.util.DynamicIdentifier;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.EncoderException;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;

import java.util.*;

@SuppressWarnings("unused")
public final class SerializableDataTypes {

	public static final SerializableDataType<Integer> INT = new SerializableDataType<>(Integer.class, CalioCodecHelper.INT);

	public static final SerializableDataType<List<Integer>> INTS = SerializableDataType.list(INT);

	public static final SerializableDataType<Boolean> BOOLEAN = new SerializableDataType<>(Boolean.class, CalioCodecHelper.BOOL);

	public static final SerializableDataType<Float> FLOAT = new SerializableDataType<>(Float.class, CalioCodecHelper.FLOAT);

	public static final SerializableDataType<List<Float>> FLOATS = SerializableDataType.list(FLOAT);

	public static final SerializableDataType<Double> DOUBLE = new SerializableDataType<>(Double.class, CalioCodecHelper.DOUBLE);

	public static final SerializableDataType<String> STRING = new SerializableDataType<>(String.class, Codec.STRING);
	public static final SerializableDataType<List<String>> STRINGS = SerializableDataType.list(STRING);

	public static final SerializableDataType<Number> NUMBER = new SerializableDataType<>(
			Number.class,
			(buf, number) -> {
				if (number instanceof Double) {
					buf.writeByte(0);
					buf.writeDouble(number.doubleValue());
				} else if (number instanceof Float) {
					buf.writeByte(1);
					buf.writeFloat(number.floatValue());
				} else if (number instanceof Integer) {
					buf.writeByte(2);
					buf.writeInt(number.intValue());
				} else if (number instanceof Long) {
					buf.writeByte(3);
					buf.writeLong(number.longValue());
				} else {
					buf.writeByte(4);
					buf.writeUtf(number.toString());
				}
			},
			buf -> {
				byte type = buf.readByte();
				switch (type) {
					case 0:
						return buf.readDouble();
					case 1:
						return buf.readFloat();
					case 2:
						return buf.readInt();
					case 3:
						return buf.readLong();
					case 4:
						return new LazilyParsedNumber(buf.readUtf());
				}
				throw new RuntimeException("Could not receive number, unexpected type id \"" + type + "\" (allowed range: [0-4])");
			},
			je -> {
				if (je.isJsonPrimitive()) {
					JsonPrimitive primitive = je.getAsJsonPrimitive();
					if (primitive.isNumber()) {
						return primitive.getAsNumber();
					} else if (primitive.isBoolean()) {
						return primitive.getAsBoolean() ? 1 : 0;
					}
				}
				throw new JsonParseException("Expected a primitive");
			},
			JsonPrimitive::new);

	public static final SerializableDataType<List<Number>> NUMBERS = SerializableDataType.list(NUMBER);

	public static final SerializableDataType<Vec3> VECTOR = new SerializableDataType<>(Vec3.class, CalioCodecHelper.VEC3D.codec());

	public static final SerializableDataType<ResourceLocation> IDENTIFIER = new SerializableDataType<>(
			ResourceLocation.class,
			FriendlyByteBuf::writeResourceLocation,
			FriendlyByteBuf::readResourceLocation,
			DynamicIdentifier::of,
			x -> new JsonPrimitive(x.toString()));

	public static final SerializableDataType<List<ResourceLocation>> IDENTIFIERS = SerializableDataType.list(IDENTIFIER);

	public static final SerializableDataType<ResourceKey<Enchantment>> ENCHANTMENT = SerializableDataType.registryKey(Registries.ENCHANTMENT);

	public static final SerializableDataType<Attribute> ATTRIBUTE = SerializableDataType.registry(Attribute.class, BuiltInRegistries.ATTRIBUTE);

	public static final SerializableDataType<Holder<Attribute>> ATTRIBUTE_ENTRY = SerializableDataType.registryEntry(BuiltInRegistries.ATTRIBUTE);

	public static final SerializableDataType<List<Holder<Attribute>>> ATTRIBUTE_ENTRIES = SerializableDataType.list(ATTRIBUTE_ENTRY);


	public static final SerializableDataType<AttributeModifier.Operation> MODIFIER_OPERATION = SerializableDataType.enumValue(AttributeModifier.Operation.class);

	public static final SerializableDataType<AttributeModifier> ATTRIBUTE_MODIFIER = SerializableDataType.compound(AttributeModifier.class, new SerializableData()
					.add("id", IDENTIFIER)
					.add("value", DOUBLE)
					.add("operation", MODIFIER_OPERATION),
			data -> new AttributeModifier(
					data.getId("id"),
					data.getDouble("value"),
					data.get("operation")
			),
			(serializableData, modifier) -> {
				SerializableData.Instance inst = serializableData.new Instance();
				inst.set("id", modifier.id());
				inst.set("value", modifier.amount());
				inst.set("operation", modifier.operation());
				return inst;
			});

	public static final SerializableDataType<List<AttributeModifier>> ATTRIBUTE_MODIFIERS =
			SerializableDataType.list(ATTRIBUTE_MODIFIER);

	public static final SerializableDataType<Item> ITEM = SerializableDataType.registry(Item.class, BuiltInRegistries.ITEM);

	public static final SerializableDataType<MobEffect> STATUS_EFFECT = SerializableDataType.registry(MobEffect.class, BuiltInRegistries.MOB_EFFECT);

	public static final SerializableDataType<List<MobEffect>> STATUS_EFFECTS =
			SerializableDataType.list(STATUS_EFFECT);

	public static final SerializableDataType<Holder<MobEffect>> STATUS_EFFECT_ENTRY = SerializableDataType.registryEntry(BuiltInRegistries.MOB_EFFECT);

	public static final SerializableDataType<MobEffectInstance> STATUS_EFFECT_INSTANCE = new SerializableDataType<>(
			MobEffectInstance.class,
			SerializationHelper::writeStatusEffect,
			SerializationHelper::readStatusEffect,
			SerializationHelper::readStatusEffect,
			SerializationHelper::writeStatusEffect);

	public static final SerializableDataType<List<MobEffectInstance>> STATUS_EFFECT_INSTANCES =
			SerializableDataType.list(STATUS_EFFECT_INSTANCE);

	public static final SerializableDataType<TagKey<Item>> ITEM_TAG = SerializableDataType.tag(Registries.ITEM);

	public static final SerializableDataType<TagKey<Fluid>> FLUID_TAG = SerializableDataType.tag(Registries.FLUID);

	public static final SerializableDataType<TagKey<Block>> BLOCK_TAG = SerializableDataType.tag(Registries.BLOCK);

	public static final SerializableDataType<TagKey<EntityType<?>>> ENTITY_TAG = SerializableDataType.tag(Registries.ENTITY_TYPE);

	@Deprecated
	public static final SerializableDataType<Ingredient.Value> INGREDIENT_ENTRY = SerializableDataType.compound(ClassUtil.castClass(Ingredient.Value.class),
			new SerializableData()
					.add("item", ITEM, null)
					.add("tag", ITEM_TAG, null),
			dataInstance -> {
				boolean tagPresent = dataInstance.isPresent("tag");
				boolean itemPresent = dataInstance.isPresent("item");
				if (tagPresent == itemPresent) {
					throw new JsonParseException("An ingredient entry is either a tag or an item, " + (tagPresent ? "not both" : "one has to be provided."));
				}
				if (tagPresent) {
					TagKey<Item> tag = dataInstance.get("tag");
					return new Ingredient.TagValue(tag);
				} else {
					return new Ingredient.ItemValue(new ItemStack((Item) dataInstance.get("item")));
				}
			}, (data, entry) -> {
				SerializableData.Instance instance = data.new Instance();
				if (entry instanceof Ingredient.TagValue tag) {
					instance.set("tag", tag.tag());
				} else if (entry instanceof Ingredient.ItemValue item) {
					instance.set("item", item.item().getItem());
				}
				return instance;
			});

	@Deprecated
	public static final SerializableDataType<List<Ingredient.Value>> INGREDIENT_ENTRIES = SerializableDataType.list(INGREDIENT_ENTRY);

	public static final SerializableDataType<Ingredient> INGREDIENT = new SerializableDataType<>(Ingredient.class, Ingredient.CODEC);

	public static final SerializableDataType<Ingredient> INGREDIENT_NONEMPTY = new SerializableDataType<>(Ingredient.class, Ingredient.CODEC_NONEMPTY);

	public static final SerializableDataType<Block> BLOCK = SerializableDataType.registry(Block.class, BuiltInRegistries.BLOCK);

	public static final SerializableDataType<BlockState> BLOCK_STATE = SerializableDataType.wrap(BlockState.class, STRING,
			BlockStateParser::serialize,
			string -> {
				try {
					return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), new StringReader(string), false).blockState();
				} catch (CommandSyntaxException e) {
					throw new JsonParseException(e);
				}
			});


	public static final SerializableDataType<ResourceKey<DamageType>> DAMAGE_TYPE = SerializableDataType.registryKey(Registries.DAMAGE_TYPE);

	public static final SerializableDataType<EquipmentSlot> EQUIPMENT_SLOT = SerializableDataType.enumValue(EquipmentSlot.class);

	public static final SerializableDataType<SoundEvent> SOUND_EVENT = SerializableDataType.registry(SoundEvent.class, BuiltInRegistries.SOUND_EVENT);

	public static final SerializableDataType<EntityType<?>> ENTITY_TYPE = SerializableDataType.registry(ClassUtil.castClass(EntityType.class), BuiltInRegistries.ENTITY_TYPE);

	public static final SerializableDataType<ParticleType<?>> PARTICLE_TYPE = SerializableDataType.registry(ClassUtil.castClass(ParticleType.class), BuiltInRegistries.PARTICLE_TYPE);

	public static final StreamCodec<ByteBuf, CompoundTag> UNLIMITED_NBT_COMPOUND_PACKET_CODEC = ByteBufCodecs.compoundTagCodec(NbtAccounter::unlimitedHeap);

	public static final SerializableDataType<CompoundTag> NBT = new SerializableDataType<>(
			ClassUtil.castClass(CompoundTag.class),
			UNLIMITED_NBT_COMPOUND_PACKET_CODEC::encode,
			UNLIMITED_NBT_COMPOUND_PACKET_CODEC::decode,
			jsonElement -> Codec.withAlternative(CompoundTag.CODEC, TagParser.LENIENT_CODEC)
					.parse(JsonOps.INSTANCE, jsonElement)
					.getOrThrow(),
			nbtCompound -> CompoundTag.CODEC
					.encodeStart(JsonOps.INSTANCE, nbtCompound)
					.mapError(err -> "Couldn't serialize NBT compound to JSON (skipping): " + err)
					.resultOrPartial(CalioAPI.LOGGER::warn)
					.orElseGet(JsonObject::new)
	);

	public static final SerializableDataType<ParticleOptions> PARTICLE_EFFECT = SerializableDataType.compound(ParticleOptions.class,
			new SerializableData()
					.add("type", PARTICLE_TYPE)
					.add("params", NBT, null),
			data -> {

				ParticleType<? extends ParticleOptions> particleType = data.get("type");
				CompoundTag paramsNbt = data.get("params");

				ResourceLocation particleTypeId = Objects.requireNonNull(BuiltInRegistries.PARTICLE_TYPE.getKey(particleType));
				if (particleType instanceof SimpleParticleType simpleType) {
					return simpleType;
				}

				else if (paramsNbt == null || paramsNbt.isEmpty()) {
					throw new JsonSyntaxException("Expected parameters for particle effect \"" + particleTypeId + "\"");
				}

				else {
					paramsNbt.putString("type", particleTypeId.toString());
					return ParticleTypes.CODEC
							.parse(NbtOps.INSTANCE, paramsNbt)
							.getOrThrow();
				}

			},
			((serializableData, particleEffect) -> {

				SerializableData.Instance data = serializableData.new Instance();
				ParticleType<?> particleType = particleEffect.getType();

				data.set("type", particleType);
				data.set("params", ParticleTypes.CODEC
						.encodeStart(NbtOps.INSTANCE, particleEffect)
						.getOrThrow());

				return data;

			}));

	public static final SerializableDataType<ParticleOptions> PARTICLE_EFFECT_OR_TYPE = new SerializableDataType<>(ParticleOptions.class,
			PARTICLE_EFFECT::send,
			PARTICLE_EFFECT::receive,
			jsonElement -> {
				if (jsonElement.isJsonPrimitive() && jsonElement.getAsJsonPrimitive().isString()) {
					ParticleType<?> type = PARTICLE_TYPE.read(jsonElement);
					if (type instanceof ParticleOptions) {
						return (ParticleOptions) type;
					}
					throw new RuntimeException("Expected either a string with a parameter-less particle effect, or an object.");
				} else if (jsonElement.isJsonObject()) {
					return PARTICLE_EFFECT.read(jsonElement);
				}
				throw new RuntimeException("Expected either a string with a parameter-less particle effect, or an object.");
			},
			PARTICLE_EFFECT::write);

	public static final SerializableDataType<DataComponentPatch> COMPONENT_CHANGES = new SerializableDataType<>(
			ClassUtil.castClass(DataComponentPatch.class),
			DataComponentPatch.STREAM_CODEC::encode,
			DataComponentPatch.STREAM_CODEC::decode,
			jsonElement -> DataComponentPatch.CODEC
					.parse(JsonOps.INSTANCE, jsonElement)
					.getOrThrow(JsonParseException::new),
			componentChanges -> DataComponentPatch.CODEC
					.encodeStart(JsonOps.INSTANCE, componentChanges)
					.mapError(err -> "Failed to serialize component changes to JSON (skipping): " + err)
					.resultOrPartial(CalioAPI.LOGGER::warn)
					.orElseGet(JsonObject::new)
	);

	public static final SerializableDataType<ItemStack> ITEM_STACK = new SerializableDataType<>(ItemStack.class, RecordCodecBuilder.create(instance -> instance.group(
			ITEM.fieldOf("item").forGetter(ItemStack::getItem),
			CalioCodecHelper.INT.optionalFieldOf("amount", 1).forGetter(ItemStack::getCount),
			COMPONENT_CHANGES.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(ItemStack::getComponentsPatch)
	).apply(instance, (t1, t2, t3) -> {
		ItemStack stack = new ItemStack(t1, t2);
		stack.applyComponents(t3);
		return stack;
	})));

	public static final SerializableDataType<List<ItemStack>> ITEM_STACKS = SerializableDataType.list(ITEM_STACK);

	public static final SerializableDataType<Component> TEXT = new SerializableDataType<>(Component.class, ComponentSerialization.CODEC, ComponentSerialization.STREAM_CODEC);

	public static final SerializableDataType<List<Component>> TEXTS = SerializableDataType.list(TEXT);

	private static final Set<ResourceKey<Level>> VANILLA_DIMENSIONS = Set.of(
			Level.OVERWORLD,
			Level.NETHER,
			Level.END
	);

	public static SerializableDataType<ResourceKey<Level>> DIMENSION = SerializableDataType.registryKey(Registries.DIMENSION, VANILLA_DIMENSIONS);

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static final SerializableDataType<Recipe> RECIPE = new SerializableDataType<>(Recipe.class,
			(buffer, recipe) -> {
				ResourceLocation serializerName = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
				Validate.notNull(serializerName, "Recipe serializer %s was not registered.".formatted(recipe.getSerializer()));
				buffer.writeResourceLocation(serializerName);
				Recipe.STREAM_CODEC.encode(buffer, recipe);
			},
			(buffer) -> {
				ResourceLocation recipeSerializerId = buffer.readResourceLocation();
				RecipeSerializer<?> serializer = BuiltInRegistries.RECIPE_SERIALIZER.get(recipeSerializerId);
				Validate.notNull(serializer, "Missing recipe serializer: %s".formatted(recipeSerializerId));
				return Recipe.STREAM_CODEC.decode(buffer);
			},
			(jsonElement) -> {
				if (!jsonElement.isJsonObject()) {
					throw new RuntimeException("Expected recipe to be a JSON object.");
				}
				JsonObject json = jsonElement.getAsJsonObject();
				ResourceLocation recipeSerializerId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "type"));
				RecipeSerializer<?> serializer = BuiltInRegistries.RECIPE_SERIALIZER.get(recipeSerializerId);
				Validate.notNull(serializer, "Missing recipe serializer: %s".formatted(recipeSerializerId));
				return Recipe.CODEC.decode(JsonOps.INSTANCE, json).getOrThrow(s -> new EncoderException("Failed to deserialize recipe with serializer: %s".formatted(recipeSerializerId))).getFirst();
			},
			recipe -> {
				ResourceLocation recipeSerializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer());
				Validate.notNull(recipeSerializerId, "Serializer %s is not registered.".formatted(recipe.getSerializer().toString()));
				JsonElement element = Recipe.CODEC.encodeStart(JsonOps.INSTANCE, recipe).getOrThrow(s -> new EncoderException("Failed to serialize recipe with serializer: %s".formatted(recipeSerializerId)));
				if (!element.isJsonObject())
					throw new EncoderException("Serializer %s returns non-object elements which cannot be safely serialized.".formatted(recipeSerializerId));
				element.getAsJsonObject().addProperty("type", recipeSerializerId.toString());
				return element;
			}
	);

	public static final SerializableDataType<GameEvent> GAME_EVENT = SerializableDataType.registry(GameEvent.class, BuiltInRegistries.GAME_EVENT);

	public static final SerializableDataType<List<GameEvent>> GAME_EVENTS = SerializableDataType.list(GAME_EVENT);

	public static final SerializableDataType<TagKey<GameEvent>> GAME_EVENT_TAG = SerializableDataType.tag(Registries.GAME_EVENT);

	public static final SerializableDataType<Fluid> FLUID = SerializableDataType.registry(Fluid.class, BuiltInRegistries.FLUID);

	public static final SerializableDataType<FogType> CAMERA_SUBMERSION_TYPE = SerializableDataType.enumValue(FogType.class);

	public static final SerializableDataType<InteractionHand> HAND = SerializableDataType.enumValue(InteractionHand.class);

	public static final SerializableDataType<EnumSet<InteractionHand>> HAND_SET = SerializableDataType.enumSet(InteractionHand.class, HAND);

	public static final SerializableDataType<EnumSet<EquipmentSlot>> EQUIPMENT_SLOT_SET = SerializableDataType.enumSet(EquipmentSlot.class, EQUIPMENT_SLOT);

	public static final SerializableDataType<InteractionResult> ACTION_RESULT = SerializableDataType.enumValue(InteractionResult.class);

	public static final SerializableDataType<UseAnim> USE_ACTION = SerializableDataType.enumValue(UseAnim.class);

	public static final SerializableDataType<StatusEffectChance> STATUS_EFFECT_CHANCE =
			SerializableDataType.compound(StatusEffectChance.class, new SerializableData()
							.add("effect", STATUS_EFFECT_INSTANCE)
							.add("chance", FLOAT, 1.0F),
					(data) -> {
						StatusEffectChance sec = new StatusEffectChance();
						sec.statusEffectInstance = data.get("effect");
						sec.chance = data.getFloat("chance");
						return sec;
					},
					(data, csei) -> {
						SerializableData.Instance inst = data.new Instance();
						inst.set("effect", csei.statusEffectInstance);
						inst.set("chance", csei.chance);
						return inst;
					});

	public static final SerializableDataType<List<StatusEffectChance>> STATUS_EFFECT_CHANCES = SerializableDataType.list(STATUS_EFFECT_CHANCE);

	public static final SerializableDataType<FoodProperties.PossibleEffect> FOOD_STATUS_EFFECT_ENTRY = new SerializableDataType<>(
			ClassUtil.castClass(FoodProperties.PossibleEffect.class),
			FoodProperties.PossibleEffect.CODEC,
			FoodProperties.PossibleEffect.STREAM_CODEC
	);

	public static final SerializableDataType<List<FoodProperties.PossibleEffect>> FOOD_STATUS_EFFECT_ENTRIES = SerializableDataType.list(FOOD_STATUS_EFFECT_ENTRY);

	public static final SerializableDataType<FoodProperties> FOOD_COMPONENT = SerializableDataType.compound(FoodProperties.class, new SerializableData()
					.add("hunger", INT)
					.add("saturation", FLOAT)
					.add("always_edible", BOOLEAN, false)
					.add("snack", BOOLEAN, false)
					.add("effect", FOOD_STATUS_EFFECT_ENTRY, null)
					.add("effects", FOOD_STATUS_EFFECT_ENTRIES, null)
					.add("using_converts_to", ITEM_STACK, null),
			(data) -> {
				FoodProperties.Builder builder = new FoodProperties.Builder()
						.nutrition(data.getInt("hunger"))
						.saturationModifier(data.getFloat("saturation"));

				if (data.getBoolean("always_edible")) {
					builder.alwaysEdible();
				}

				if (data.getBoolean("snack")) {
					builder.fast();
				}

				data.<FoodProperties.PossibleEffect>ifPresent("effect", effectEntry ->
						builder.effect(effectEntry.effect(), effectEntry.probability())
				);

				data.<List<FoodProperties.PossibleEffect>>ifPresent("effects", effectEntries -> effectEntries.forEach(effectEntry ->
						builder.effect(effectEntry.effect(), effectEntry.probability())
				));

				if (data.isPresent("using_converts_to")) {
					builder.usingConvertsTo(data.get("using_converts_to"));
				}

				return builder.build();
			},
			(data, fc) -> {
				SerializableData.Instance inst = data.new Instance();
				inst.set("hunger", fc.nutrition());
				inst.set("saturation", fc.saturation());
				inst.set("always_edible", fc.canAlwaysEat());
				inst.set("snack", fc.eatDurationTicks());
				inst.set("effect", null);
				List<StatusEffectChance> statusEffectChances = new LinkedList<>();
				fc.effects().forEach(pair -> {
					StatusEffectChance sec = new StatusEffectChance();
					sec.statusEffectInstance = pair.effect();
					sec.chance = pair.probability();
					statusEffectChances.add(sec);
				});
				if (!statusEffectChances.isEmpty()) {
					inst.set("effects", statusEffectChances);
				} else {
					inst.set("effects", null);
				}
				return inst;
			});

	public static final SerializableDataType<Direction> DIRECTION = SerializableDataType.enumValue(Direction.class);

	public static final SerializableDataType<EnumSet<Direction>> DIRECTION_SET = SerializableDataType.enumSet(Direction.class, DIRECTION);

	public static final SerializableDataType<Class<?>> CLASS = SerializableDataType.wrap(ClassUtil.castClass(Class.class), SerializableDataTypes.STRING,
			Class::getName,
			str -> {
				try {
					return Class.forName(str);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException("Specified class does not exist: \"" + str + "\".");
				}
			});

	public static final SerializableDataType<ClipContext.Block> SHAPE_TYPE = SerializableDataType.enumValue(ClipContext.Block.class);

	public static final SerializableDataType<ClipContext.Fluid> FLUID_HANDLING = SerializableDataType.enumValue(ClipContext.Fluid.class);

	public static final SerializableDataType<Explosion.BlockInteraction> DESTRUCTION_TYPE = SerializableDataType.enumValue(Explosion.BlockInteraction.class);

	public static final SerializableDataType<Direction.Axis> AXIS = SerializableDataType.enumValue(Direction.Axis.class);

	public static final SerializableDataType<EnumSet<Direction.Axis>> AXIS_SET = SerializableDataType.enumSet(Direction.Axis.class, AXIS);

	public static final SerializableDataType<ArgumentWrapper<NbtPathArgument.NbtPath>> NBT_PATH =
			SerializableDataType.argumentType(NbtPathArgument.nbtPath());

	public static final SerializableDataType<ClipContext.Block> RAYCAST_SHAPE_TYPE = SerializableDataType.enumValue(ClipContext.Block.class);

	public static final SerializableDataType<ClipContext.Fluid> RAYCAST_FLUID_HANDLING = SerializableDataType.enumValue(ClipContext.Fluid.class);

	public static final SerializableDataType<Stat<?>> STAT = SerializableDataType.compound(ClassUtil.castClass(Stat.class),
			new SerializableData()
					.add("type", SerializableDataType.registry(ClassUtil.castClass(StatType.class), BuiltInRegistries.STAT_TYPE))
					.add("id", SerializableDataTypes.IDENTIFIER),
			data -> {
				StatType statType = data.get("type");
				Registry<?> statRegistry = statType.getRegistry();
				ResourceLocation statId = data.get("id");
				if (statRegistry.containsKey(statId)) {
					Object statObject = statRegistry.get(statId);
					return statType.get(statObject);
				}
				throw new IllegalArgumentException("Desired stat \"" + statId + "\" does not exist in stat type ");
			},
			(data, stat) -> {
				SerializableData.Instance inst = data.new Instance();
				inst.set("type", stat.getType());
				Registry reg = stat.getType().getRegistry();
				ResourceLocation statId = reg.getKey(stat.getValue());
				inst.set("id", statId);
				return inst;
			});

	public static final SerializableDataType<TagKey<Biome>> BIOME_TAG = SerializableDataType.tag(Registries.BIOME);

	public static final SerializableDataType<TagLike<Item>> ITEM_TAG_LIKE = SerializableDataType.tagLike(BuiltInRegistries.ITEM);

	public static final SerializableDataType<TagLike<Block>> BLOCK_TAG_LIKE = SerializableDataType.tagLike(BuiltInRegistries.BLOCK);

	public static final SerializableDataType<TagLike<EntityType<?>>> ENTITY_TYPE_TAG_LIKE = SerializableDataType.tagLike(BuiltInRegistries.ENTITY_TYPE);

}
