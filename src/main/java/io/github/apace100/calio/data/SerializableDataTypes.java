package io.github.apace100.calio.data;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LazilyParsedNumber;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.Calio;
import io.github.apace100.calio.ClassUtil;
import io.github.apace100.calio.SerializationHelper;
import io.github.apace100.calio.util.ArgumentWrapper;
import io.github.apace100.calio.util.StatusEffectChance;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.ResourceLocationException;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.Validate;

import java.util.*;

@SuppressWarnings("unused")
public final class SerializableDataTypes {

	public static final SerializableDataType<Integer> INT = new SerializableDataType<>(Integer.class, Codec.INT);

	public static final SerializableDataType<List<Integer>> INTS = SerializableDataType.list(INT);

	public static final SerializableDataType<Boolean> BOOLEAN = new SerializableDataType<>(Boolean.class, Codec.BOOL);

	public static final SerializableDataType<Float> FLOAT = new SerializableDataType<>(Float.class, Codec.FLOAT);

	public static final SerializableDataType<List<Float>> FLOATS = SerializableDataType.list(FLOAT);

	public static final SerializableDataType<Double> DOUBLE = new SerializableDataType<>(Double.class, Codec.DOUBLE);

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
			(json) -> {
				String idString = json.getAsString();
				if (idString.contains(":")) {
					String[] idSplit = idString.split(":");
					if (idSplit.length != 2) {
						throw new ResourceLocationException("Incorrect number of `:` in identifier: \"" + idString + "\".");
					}
					if (idSplit[0].contains("*")) {
						if (SerializableData.CURRENT_NAMESPACE != null) {
							idSplit[0] = idSplit[0].replace("*", SerializableData.CURRENT_NAMESPACE);
						} else {
							throw new ResourceLocationException("Identifier may not contain a `*` in the namespace when read here.");
						}
					}
					if (idSplit[1].contains("*")) {
						if (SerializableData.CURRENT_PATH != null) {
							idSplit[1] = idSplit[1].replace("*", SerializableData.CURRENT_PATH);
						} else {
							throw new ResourceLocationException("Identifier may only contain a `*` in the path inside of powers.");
						}
					}
					idString = idSplit[0] + ":" + idSplit[1];
				} else {
					if (idString.contains("*")) {
						if (SerializableData.CURRENT_PATH != null) {
							idString = idString.replace("*", SerializableData.CURRENT_PATH);
						} else {
							throw new ResourceLocationException("Identifier may only contain a `*` in the path inside of powers.");
						}
					}
				}
				return new ResourceLocation(idString);
			},
			x -> new JsonPrimitive(x.toString()));

	public static final SerializableDataType<List<ResourceLocation>> IDENTIFIERS = SerializableDataType.list(IDENTIFIER);

	public static final SerializableDataType<Enchantment> ENCHANTMENT = SerializableDataType.registry(Enchantment.class, ForgeRegistries.ENCHANTMENTS);

	public static final SerializableDataType<DamageSource> DAMAGE_SOURCE = SerializableDataType.compound(DamageSource.class, new SerializableData()
					.add("name", STRING)
					.add("bypasses_armor", BOOLEAN, false)
					.add("fire", BOOLEAN, false)
					.add("unblockable", BOOLEAN, false)
					.add("magic", BOOLEAN, false)
					.add("out_of_world", BOOLEAN, false)
					.add("projectile", BOOLEAN, false)
					.add("explosive", BOOLEAN, false),
			(data) -> {
				DamageSource damageSource = new DamageSource(data.getString("name"));
				if (data.getBoolean("bypasses_armor")) damageSource.bypassArmor();
				if (data.getBoolean("fire")) damageSource.setIsFire();
				if (data.getBoolean("unblockable")) damageSource.bypassMagic();
				if (data.getBoolean("magic")) damageSource.setMagic();
				if (data.getBoolean("out_of_world")) damageSource.bypassInvul();
				if (data.getBoolean("projectile")) damageSource.setProjectile();
				if (data.getBoolean("explosive")) damageSource.setExplosion();
				return damageSource;
			},
			(data, ds) -> {
				SerializableData.Instance inst = data.new Instance();
				inst.set("name", ds.getMsgId());
				inst.set("fire", ds.isFire());
				inst.set("unblockable", ds.isBypassMagic());
				inst.set("bypasses_armor", ds.isBypassArmor());
				inst.set("out_of_world", ds.isBypassInvul());
				inst.set("magic", ds.isMagic());
				inst.set("projectile", ds.isProjectile());
				inst.set("explosive", ds.isExplosion());
				return inst;
			});

	public static final SerializableDataType<Attribute> ATTRIBUTE = SerializableDataType.registry(Attribute.class, ForgeRegistries.ATTRIBUTES);

	public static final SerializableDataType<AttributeModifier.Operation> MODIFIER_OPERATION = SerializableDataType.enumValue(AttributeModifier.Operation.class);

	public static final SerializableDataType<AttributeModifier> ATTRIBUTE_MODIFIER = SerializableDataType.compound(AttributeModifier.class, new SerializableData()
					.add("name", STRING, "Unnamed attribute modifier")
					.add("operation", MODIFIER_OPERATION)
					.add("value", DOUBLE),
			data -> new AttributeModifier(
					data.getString("name"),
					data.getDouble("value"),
					data.get("operation")
			),
			(serializableData, modifier) -> {
				SerializableData.Instance inst = serializableData.new Instance();
				inst.set("name", modifier.getName());
				inst.set("value", modifier.getAmount());
				inst.set("operation", modifier.getOperation());
				return inst;
			});

	public static final SerializableDataType<List<AttributeModifier>> ATTRIBUTE_MODIFIERS =
			SerializableDataType.list(ATTRIBUTE_MODIFIER);

	public static final SerializableDataType<Item> ITEM = SerializableDataType.registry(Item.class, ForgeRegistries.ITEMS);

	public static final SerializableDataType<MobEffect> STATUS_EFFECT = SerializableDataType.registry(MobEffect.class, ForgeRegistries.MOB_EFFECTS);

	public static final SerializableDataType<List<MobEffect>> STATUS_EFFECTS =
			SerializableDataType.list(STATUS_EFFECT);

	public static final SerializableDataType<MobEffectInstance> STATUS_EFFECT_INSTANCE = new SerializableDataType<>(
			MobEffectInstance.class,
			SerializationHelper::writeStatusEffect,
			SerializationHelper::readStatusEffect,
			SerializationHelper::readStatusEffect,
			SerializationHelper::writeStatusEffect);

	public static final SerializableDataType<List<MobEffectInstance>> STATUS_EFFECT_INSTANCES =
			SerializableDataType.list(STATUS_EFFECT_INSTANCE);

	public static final SerializableDataType<TagKey<Item>> ITEM_TAG = SerializableDataType.tag(Registry.ITEM_REGISTRY);

	public static final SerializableDataType<TagKey<Fluid>> FLUID_TAG = SerializableDataType.tag(Registry.FLUID_REGISTRY);

	public static final SerializableDataType<TagKey<Block>> BLOCK_TAG = SerializableDataType.tag(Registry.BLOCK_REGISTRY);

	public static final SerializableDataType<TagKey<EntityType<?>>> ENTITY_TAG = SerializableDataType.tag(Registry.ENTITY_TYPE_REGISTRY);

	/*public static final SerializableDataType<HolderSet<Item>> INGREDIENT_ENTRY = new SerializableDataType<>(ClassUtil.castClass(List.class), RecordCodecBuilder.create(instance -> instance.group(
			CalioCodecHelper.optionalField(ITEM, "item").forGetter(x -> x.size() == 1 ? Optional.of(x.get(0)) : Optional.empty()),
			CalioCodecHelper.optionalField(ITEM_TAG, "tag").forGetter(items -> {
				if (items.size() == 1)
					return Optional.empty();
				Registry.ITEM.getTagOrEmpty(items).forEach();
				TagContainer tagManager = Calio.getTagManager();
				TagCollection<Item> tagGroup = tagManager.getOrEmpty(Registry.ITEM_REGISTRY);
				Collection<ResourceLocation> possibleTags = tagGroup.getMatchingTags(items.get(0));
				for (int i = 1; i < items.size() && possibleTags.size() > 1; i++) {
					possibleTags.removeAll(tagGroup.getMatchingTags(items.get(i)));
				}
				if (possibleTags.size() != 1) {
					throw new IllegalStateException("Couldn't transform item list to a single tag");
				}
				return possibleTags.stream().findFirst().map(tagGroup::getTag);
			})
	).apply(instance, (item, itemTag) -> itemTag.map(Tag::getValues).or(() -> item.map(ImmutableList::of)).orElseGet(ImmutableList::of))));

	public static final SerializableDataType<List<List<Item>>> INGREDIENT_ENTRIES = SerializableDataType.list(INGREDIENT_ENTRY);

	// An alternative version of an ingredient deserializer which allows `minecraft:air`
	public static final SerializableDataType<Ingredient> INGREDIENT = new SerializableDataType<>(
			Ingredient.class,
			(buffer, ingredient) -> ingredient.toNetwork(buffer),
			Ingredient::fromNetwork,
			jsonElement -> {
				List<List<Item>> itemLists = INGREDIENT_ENTRIES.read(jsonElement);
				List<ItemStack> items = new LinkedList<>();
				itemLists.forEach(itemList -> itemList.forEach(item -> items.add(new ItemStack(item))));
				return Ingredient.of(items.stream());
			},
			Ingredient::toJson);*/

	// The regular vanilla Minecraft ingredient.
	public static final SerializableDataType<Ingredient> VANILLA_INGREDIENT = new SerializableDataType<>(
			Ingredient.class,
			(buffer, ingredient) -> ingredient.toNetwork(buffer),
			Ingredient::fromNetwork,
			Ingredient::fromJson,
			Ingredient::toJson);

	public static final SerializableDataType<Block> BLOCK = SerializableDataType.registry(Block.class, ForgeRegistries.BLOCKS);

	public static final SerializableDataType<BlockState> BLOCK_STATE = SerializableDataType.wrap(BlockState.class, STRING,
			BlockStateParser::serialize,
			string -> {
				try {
					return (new BlockStateParser(new StringReader(string), false)).parse(false).getState();
				} catch (CommandSyntaxException e) {
					throw new JsonParseException(e);
				}
			});

	public static final SerializableDataType<MobType> ENTITY_GROUP =
			SerializableDataType.mapped(MobType.class, HashBiMap.create(ImmutableMap.of(
					"default", MobType.UNDEFINED,
					"undead", MobType.UNDEAD,
					"arthropod", MobType.ARTHROPOD,
					"illager", MobType.ILLAGER,
					"aquatic", MobType.WATER
			)));

	public static final SerializableDataType<EquipmentSlot> EQUIPMENT_SLOT = SerializableDataType.enumValue(EquipmentSlot.class);

	public static final SerializableDataType<SoundEvent> SOUND_EVENT = SerializableDataType.registry(SoundEvent.class, ForgeRegistries.SOUND_EVENTS);

	public static final SerializableDataType<EntityType<?>> ENTITY_TYPE = SerializableDataType.registry(ClassUtil.castClass(EntityType.class), ForgeRegistries.ENTITIES);

	public static final SerializableDataType<ParticleType<?>> PARTICLE_TYPE = SerializableDataType.registry(ClassUtil.castClass(ParticleType.class), ForgeRegistries.PARTICLE_TYPES);

	public static final SerializableDataType<ParticleOptions> PARTICLE_EFFECT = SerializableDataType.compound(ParticleOptions.class,
			new SerializableData()
					.add("type", PARTICLE_TYPE)
					.add("params", STRING, ""),
			dataInstance -> SerializationHelper.loadParticle(dataInstance.get("type"), dataInstance.getString("params")),
			((serializableData, particleEffect) -> {
				SerializableData.Instance data = serializableData.new Instance();
				data.set("type", particleEffect.getType());
				String params = particleEffect.writeToString();
				int spaceIndex = params.indexOf(' ');
				if (spaceIndex > -1) {
					params = params.substring(spaceIndex + 1);
				} else {
					params = "";
				}
				data.set("params", params);
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

	public static final SerializableDataType<CompoundTag> NBT = SerializableDataType.wrap(CompoundTag.class, SerializableDataTypes.STRING,
			CompoundTag::toString,
			(str) -> {
				try {
					return new TagParser(new StringReader(str)).readStruct();
				} catch (CommandSyntaxException e) {
					throw new JsonSyntaxException("Could not parse NBT tag, exception: " + e.getMessage());
				}
			});

	public static final SerializableDataType<ItemStack> ITEM_STACK = new SerializableDataType<>(ItemStack.class, RecordCodecBuilder.create(instance -> instance.group(
			ITEM.fieldOf("item").forGetter(ItemStack::getItem),
			CalioCodecHelper.optionalField(Codec.INT, "amount", 1).forGetter(ItemStack::getCount),
			CalioCodecHelper.optionalField(NBT, "tag").forGetter(x -> Optional.ofNullable(x.getTag()))
	).apply(instance, (t1, t2, t3) -> {
		ItemStack itemStack = new ItemStack(t1, t2);
		t3.ifPresent(itemStack::setTag);
		return itemStack;
	})));

	public static final SerializableDataType<List<ItemStack>> ITEM_STACKS = SerializableDataType.list(ITEM_STACK);

	public static final SerializableDataType<Component> TEXT = new SerializableDataType<>(Component.class,
			(buffer, text) -> buffer.writeUtf(Component.Serializer.toJson(text)),
			(buffer) -> Component.Serializer.fromJson(buffer.readUtf()),
			Component.Serializer::fromJson,
			Component.Serializer::toJsonTree);

	public static final SerializableDataType<List<Component>> TEXTS = SerializableDataType.list(TEXT);

	public static SerializableDataType<ResourceKey<Level>> DIMENSION = SerializableDataType.wrap(
			ClassUtil.castClass(ResourceKey.class),
			SerializableDataTypes.IDENTIFIER,
			ResourceKey::location, identifier -> ResourceKey.create(Registry.DIMENSION_REGISTRY, identifier)
	);

	// It is theoretically possible to support recipe serialization, but it's a mess.
	// To do this, we need to keep an additional list functions designed to build RecipeJsonProvider
	// from recipes, which is possible, but time consuming to setup, and prone to breaking if another
	// mod's recipe type is used.
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static final SerializableDataType<Recipe> RECIPE = new SerializableDataType<>(Recipe.class,
			(buffer, recipe) -> {
				ResourceLocation serializerName = recipe.getSerializer().getRegistryName();
				Validate.notNull(serializerName, "Recipe serializer %s was not registered.".formatted(recipe.getSerializer()));
				buffer.writeResourceLocation(serializerName);
				buffer.writeResourceLocation(recipe.getId());
				recipe.getSerializer().toNetwork(buffer, recipe);
			},
			(buffer) -> {
				ResourceLocation recipeSerializerId = buffer.readResourceLocation();
				ResourceLocation recipeId = buffer.readResourceLocation();
				RecipeSerializer<?> serializer = ForgeRegistries.RECIPE_SERIALIZERS.getValue(recipeSerializerId);
				Validate.notNull(serializer, "Missing recipe serializer: %s".formatted(recipeSerializerId));
				return serializer.fromNetwork(recipeId, buffer);
			},
			(jsonElement) -> {
				if (!jsonElement.isJsonObject()) {
					throw new RuntimeException("Expected recipe to be a JSON object.");
				}
				JsonObject json = jsonElement.getAsJsonObject();
				ResourceLocation recipeSerializerId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "type"));
				ResourceLocation recipeId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "id"));
				RecipeSerializer<?> serializer = ForgeRegistries.RECIPE_SERIALIZERS.getValue(recipeSerializerId);
				Validate.notNull(serializer, "Missing recipe serializer: %s".formatted(recipeSerializerId));
				Validate.notNull(recipeId, "Missing recipe id.");
				return serializer.fromJson(recipeId, json);
			});

	public static final SerializableDataType<GameEvent> GAME_EVENT = SerializableDataType.registry(GameEvent.class, Registry.GAME_EVENT);

	public static final SerializableDataType<List<GameEvent>> GAME_EVENTS = SerializableDataType.list(GAME_EVENT);

	public static final SerializableDataType<TagKey<GameEvent>> GAME_EVENT_TAG = SerializableDataType.tag(Registry.GAME_EVENT_REGISTRY);

	public static final SerializableDataType<Fluid> FLUID = SerializableDataType.registry(Fluid.class, ForgeRegistries.FLUIDS);

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

	public static final SerializableDataType<FoodProperties> FOOD_COMPONENT = SerializableDataType.compound(FoodProperties.class, new SerializableData()
					.add("hunger", INT)
					.add("saturation", FLOAT)
					.add("meat", BOOLEAN, false)
					.add("always_edible", BOOLEAN, false)
					.add("snack", BOOLEAN, false)
					.add("effect", STATUS_EFFECT_CHANCE, null)
					.add("effects", STATUS_EFFECT_CHANCES, null),
			(data) -> {
				FoodProperties.Builder builder = new FoodProperties.Builder().nutrition(data.getInt("hunger")).saturationMod(data.getFloat("saturation"));
				if (data.getBoolean("meat")) {
					builder.meat();
				}
				if (data.getBoolean("always_edible")) {
					builder.alwaysEat();
				}
				if (data.getBoolean("snack")) {
					builder.fast();
				}
				data.<StatusEffectChance>ifPresent("effect", sec -> builder.effect(() -> sec.statusEffectInstance, sec.chance));
				data.<List<StatusEffectChance>>ifPresent("effects", secs -> secs.forEach(sec -> builder.effect(() -> sec.statusEffectInstance, sec.chance)));
				return builder.build();
			},
			(data, fc) -> {
				SerializableData.Instance inst = data.new Instance();
				inst.set("hunger", fc.getNutrition());
				inst.set("saturation", fc.getSaturationModifier());
				inst.set("meat", fc.isMeat());
				inst.set("always_edible", fc.canAlwaysEat());
				inst.set("snack", fc.isFastFood());
				inst.set("effect", null);
				List<StatusEffectChance> statusEffectChances = new LinkedList<>();
				fc.getEffects().forEach(pair -> {
					StatusEffectChance sec = new StatusEffectChance();
					sec.statusEffectInstance = pair.getFirst();
					sec.chance = pair.getSecond();
					statusEffectChances.add(sec);
				});
				if (statusEffectChances.size() > 0) {
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

	private static final HashMap<String, Material> MATERIAL_MAP;

	static {
		MATERIAL_MAP = new HashMap<>();
		MATERIAL_MAP.put("air", Material.AIR);
		MATERIAL_MAP.put("structure_void", Material.STRUCTURAL_AIR);
		MATERIAL_MAP.put("portal", Material.PORTAL);
		MATERIAL_MAP.put("carpet", Material.CLOTH_DECORATION);
		MATERIAL_MAP.put("plant", Material.PLANT);
		MATERIAL_MAP.put("underwater_plant", Material.WATER_PLANT);
		MATERIAL_MAP.put("replaceable_plant", Material.REPLACEABLE_PLANT);
		MATERIAL_MAP.put("nether_shoots", Material.REPLACEABLE_FIREPROOF_PLANT);
		MATERIAL_MAP.put("replaceable_underwater_plant", Material.REPLACEABLE_WATER_PLANT);
		MATERIAL_MAP.put("water", Material.WATER);
		MATERIAL_MAP.put("bubble_column", Material.BUBBLE_COLUMN);
		MATERIAL_MAP.put("lava", Material.LAVA);
		MATERIAL_MAP.put("snow_layer", Material.TOP_SNOW);
		MATERIAL_MAP.put("fire", Material.FIRE);
		MATERIAL_MAP.put("decoration", Material.DECORATION);
		MATERIAL_MAP.put("cobweb", Material.WEB);
		MATERIAL_MAP.put("sculk", Material.SCULK);
		MATERIAL_MAP.put("redstone_lamp", Material.BUILDABLE_GLASS);
		MATERIAL_MAP.put("organic_product", Material.CLAY);
		MATERIAL_MAP.put("soil", Material.DIRT);
		MATERIAL_MAP.put("solid_organic", Material.GRASS);
		MATERIAL_MAP.put("dense_ice", Material.ICE_SOLID);
		MATERIAL_MAP.put("aggregate", Material.SAND);
		MATERIAL_MAP.put("sponge", Material.SPONGE);
		MATERIAL_MAP.put("shulker_box", Material.SHULKER_SHELL);
		MATERIAL_MAP.put("wood", Material.WOOD);
		MATERIAL_MAP.put("nether_wood", Material.NETHER_WOOD);
		MATERIAL_MAP.put("bamboo_sapling", Material.BAMBOO_SAPLING);
		MATERIAL_MAP.put("bamboo", Material.BAMBOO);
		MATERIAL_MAP.put("wool", Material.WOOL);
		MATERIAL_MAP.put("tnt", Material.EXPLOSIVE);
		MATERIAL_MAP.put("leaves", Material.LEAVES);
		MATERIAL_MAP.put("glass", Material.GLASS);
		MATERIAL_MAP.put("ice", Material.ICE);
		MATERIAL_MAP.put("cactus", Material.CACTUS);
		MATERIAL_MAP.put("stone", Material.STONE);
		MATERIAL_MAP.put("metal", Material.METAL);
		MATERIAL_MAP.put("snow_block", Material.SNOW);
		MATERIAL_MAP.put("repair_station", Material.HEAVY_METAL);
		MATERIAL_MAP.put("barrier", Material.BARRIER);
		MATERIAL_MAP.put("piston", Material.PISTON);
		MATERIAL_MAP.put("moss_block", Material.MOSS);
		MATERIAL_MAP.put("gourd", Material.VEGETABLE);
		MATERIAL_MAP.put("egg", Material.EGG);
		MATERIAL_MAP.put("cake", Material.CAKE);
		MATERIAL_MAP.put("amethyst", Material.AMETHYST);
		MATERIAL_MAP.put("powder_snow", Material.POWDER_SNOW);
	}

	public static final SerializableDataType<Material> MATERIAL = SerializableDataType.mapped(Material.class, HashBiMap.create(MATERIAL_MAP));

	public static final SerializableDataType<List<Material>> MATERIALS = SerializableDataType.list(MATERIAL);

	public static final SerializableDataType<ArgumentWrapper<NbtPathArgument.NbtPath>> NBT_PATH =
			SerializableDataType.argumentType(NbtPathArgument.nbtPath());
}
