package io.github.apace100.calio.data;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.Calio;
import io.github.apace100.calio.ClassUtil;
import io.github.apace100.calio.SerializationHelper;
import io.github.apace100.calio.util.IdentifiedTag;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.*;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public final class SerializableDataTypes {

	public static final SerializableDataType<Integer> INT = new SerializableDataType<>(Integer.class, Codec.INT);

	public static final SerializableDataType<Boolean> BOOLEAN = new SerializableDataType<>(Boolean.class, Codec.BOOL);

	public static final SerializableDataType<Float> FLOAT = new SerializableDataType<>(Float.class, Codec.FLOAT);

	public static final SerializableDataType<Double> DOUBLE = new SerializableDataType<>(Double.class, Codec.DOUBLE);

	public static final SerializableDataType<String> STRING = new SerializableDataType<>(String.class, Codec.STRING);

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

	public static final SerializableDataType<Enchantment> ENCHANTMENT = SerializableDataType.registry(Enchantment.class, Registry.ENCHANTMENT);

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

	public static final SerializableDataType<Attribute> ATTRIBUTE = SerializableDataType.registry(Attribute.class, Registry.ATTRIBUTE);

	public static final SerializableDataType<AttributeModifier.Operation> MODIFIER_OPERATION = SerializableDataType.enumValue(AttributeModifier.Operation.class);

	public static final SerializableDataType<AttributeModifier> ATTRIBUTE_MODIFIER = SerializableDataType.compound(AttributeModifier.class, new SerializableData()
					.add("name", STRING, "Unnamed attribute modifier")
					.add("operation", MODIFIER_OPERATION)
					.add("value", DOUBLE),
			data -> new AttributeModifier(
					data.getString("name"),
					data.getDouble("value"),
					(AttributeModifier.Operation) data.get("operation")
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

	public static final SerializableDataType<Item> ITEM = SerializableDataType.registry(Item.class, Registry.ITEM);

	public static final SerializableDataType<MobEffect> STATUS_EFFECT = SerializableDataType.registry(MobEffect.class, Registry.MOB_EFFECT);

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

	public static final SerializableDataType<Tag<Item>> ITEM_TAG = SerializableDataType.wrap(ClassUtil.castClass(Tag.class), IDENTIFIER,
			item -> Calio.getTagManager().getIdOrThrow(Registry.ITEM_REGISTRY, item, () -> new JsonSyntaxException("Unknown fluid tag")),
			ItemTags::createOptional);

	public static final SerializableDataType<Tag<Fluid>> FLUID_TAG = SerializableDataType.wrap(ClassUtil.castClass(Tag.class), IDENTIFIER,
			fluid -> Calio.getTagManager().getIdOrThrow(Registry.FLUID_REGISTRY, fluid, () -> new JsonSyntaxException("Unknown fluid tag")),
			FluidTags::createOptional);

	public static final SerializableDataType<Tag<Block>> BLOCK_TAG = SerializableDataType.wrap(ClassUtil.castClass(Tag.class), IDENTIFIER,
			block -> Calio.getTagManager().getIdOrThrow(Registry.BLOCK_REGISTRY, block, () -> new JsonSyntaxException("Unknown block tag")),
			BlockTags::createOptional);

	public static final SerializableDataType<Tag<EntityType<?>>> ENTITY_TAG = SerializableDataType.wrap(ClassUtil.castClass(Tag.class), SerializableDataTypes.IDENTIFIER,
			tag -> Calio.getTagManager().getIdOrThrow(Registry.ENTITY_TYPE_REGISTRY, tag, RuntimeException::new),
			EntityTypeTags::createOptional);

	public static final SerializableDataType<List<Item>> INGREDIENT_ENTRY = new SerializableDataType<>(ClassUtil.castClass(List.class), RecordCodecBuilder.create(instance -> instance.group(
			CalioCodecHelper.optionalField(ITEM, "item").forGetter(x -> x.size() == 1 ? Optional.of(x.get(0)) : Optional.empty()),
			CalioCodecHelper.optionalField(ITEM_TAG, "tag").forGetter(items -> {
				if (items.size() == 1)
					return Optional.empty();
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
			Ingredient::toJson);

	// The regular vanilla Minecraft ingredient.
	public static final SerializableDataType<Ingredient> VANILLA_INGREDIENT = new SerializableDataType<>(
			Ingredient.class,
			(buffer, ingredient) -> ingredient.toNetwork(buffer),
			Ingredient::fromNetwork,
			Ingredient::fromJson,
			Ingredient::toJson);

	public static final SerializableDataType<Block> BLOCK = SerializableDataType.registry(Block.class, Registry.BLOCK);

	public static final SerializableDataType<MobType> ENTITY_GROUP =
			SerializableDataType.mapped(MobType.class, HashBiMap.create(ImmutableMap.of(
					"default", MobType.UNDEFINED,
					"undead", MobType.UNDEAD,
					"arthropod", MobType.ARTHROPOD,
					"illager", MobType.ILLAGER,
					"aquatic", MobType.WATER
			)));

	public static final SerializableDataType<EquipmentSlot> EQUIPMENT_SLOT = SerializableDataType.enumValue(EquipmentSlot.class);

	public static final SerializableDataType<SoundEvent> SOUND_EVENT = SerializableDataType.registry(SoundEvent.class, Registry.SOUND_EVENT);

	public static final SerializableDataType<EntityType<?>> ENTITY_TYPE = SerializableDataType.registry(ClassUtil.castClass(EntityType.class), Registry.ENTITY_TYPE);

	public static final SerializableDataType<ParticleType<?>> PARTICLE_TYPE = SerializableDataType.registry(ClassUtil.castClass(ParticleType.class), Registry.PARTICLE_TYPE);

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
	// It is theoretically possible to support recipe serialization, but it's a mess.
	// To do this, we need to keep an additional list functions designed to build RecipeJsonProvider
	// from recipes, which is possible, but time consuming to setup, and prone to breaking if another
	// mod's recipe type is used.
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static final SerializableDataType<Recipe> RECIPE = new SerializableDataType<>(Recipe.class,
			(buffer, recipe) -> {
				buffer.writeResourceLocation(recipe.getSerializer().getRegistryName());
				buffer.writeResourceLocation(recipe.getId());
				recipe.getSerializer().toNetwork(buffer, recipe);
			},
			(buffer) -> {
				ResourceLocation recipeSerializerId = buffer.readResourceLocation();
				ResourceLocation recipeId = buffer.readResourceLocation();
				RecipeSerializer serializer = ForgeRegistries.RECIPE_SERIALIZERS.getValue(recipeSerializerId);
				return serializer.fromNetwork(recipeId, buffer);
			},
			(jsonElement) -> {
				if (!jsonElement.isJsonObject()) {
					throw new RuntimeException("Expected recipe to be a JSON object.");
				}
				JsonObject json = jsonElement.getAsJsonObject();
				ResourceLocation recipeSerializerId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "type"));
				ResourceLocation recipeId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "id"));
				RecipeSerializer serializer = ForgeRegistries.RECIPE_SERIALIZERS.getValue(recipeSerializerId);
				return serializer.fromJson(recipeId, json);
			});
	public static final SerializableDataType<GameEvent> GAME_EVENT = SerializableDataType.registry(GameEvent.class, Registry.GAME_EVENT);
	public static final SerializableDataType<List<GameEvent>> GAME_EVENTS =
			SerializableDataType.list(GAME_EVENT);
	public static final SerializableDataType<Tag<GameEvent>> GAME_EVENT_TAG = SerializableDataType.wrap(ClassUtil.castClass(Tag.class), SerializableDataTypes.IDENTIFIER,
			tag -> Calio.getTagManager().getIdOrThrow(Registry.GAME_EVENT_REGISTRY, tag, RuntimeException::new),
			id -> new IdentifiedTag<>(Registry.GAME_EVENT_REGISTRY, id));
	public static SerializableDataType<ResourceKey<Level>> DIMENSION = SerializableDataType.wrap(
			ClassUtil.castClass(ResourceKey.class),
			SerializableDataTypes.IDENTIFIER,
			ResourceKey::location, identifier -> ResourceKey.create(Registry.DIMENSION_REGISTRY, identifier)
	);
}
