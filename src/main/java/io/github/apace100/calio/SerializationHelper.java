package io.github.apace100.calio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

public class SerializationHelper {

	public static TagKey<Fluid> getFluidTagFromId(ResourceLocation id) {
		return Objects.requireNonNull(ForgeRegistries.FLUIDS.tags()).createTagKey(id);
	}

	//TODO Check if this is enough
	public static TagKey<Block> getBlockTagFromId(ResourceLocation id) {
		return Objects.requireNonNull(ForgeRegistries.BLOCKS.tags()).createTagKey(id);
	}

    // Use SerializableDataTypes.ATTRIBUTE_MODIFIER instead
    @Deprecated
    public static AttributeModifier readAttributeModifier(JsonElement jsonElement) {
        if(jsonElement.isJsonObject()) {
            JsonObject json = jsonElement.getAsJsonObject();
            String name = GsonHelper.getAsString(json, "name", "Unnamed attribute modifier");
            String operation = GsonHelper.getAsString(json, "operation").toUpperCase(Locale.ROOT);
            double value = GsonHelper.getAsFloat(json, "value");
            return new AttributeModifier(name, value, AttributeModifier.Operation.valueOf(operation));
        }
        throw new JsonSyntaxException("Attribute modifier needs to be a JSON object.");
    }

	// Use SerializableDataTypes.ATTRIBUTE_MODIFIER instead
	@Deprecated
	public static JsonElement writeAttributeModifier(AttributeModifier modifier) {
		JsonObject obj = new JsonObject();
		obj.addProperty("name", modifier.getName());
		obj.addProperty("operation", modifier.getOperation().name());
		obj.addProperty("value", modifier.getAmount());
		return obj;
	}

	// Use SerializableDataTypes.ATTRIBUTE_MODIFIER instead
	@Deprecated
	public static AttributeModifier readAttributeModifier(FriendlyByteBuf buf) {
		String modName = buf.readUtf();
		double modValue = buf.readDouble();
		int operation = buf.readInt();
		return new AttributeModifier(modName, modValue, AttributeModifier.Operation.fromValue(operation));
	}

	// Use SerializableDataTypes.ATTRIBUTE_MODIFIER instead
	@Deprecated
	public static void writeAttributeModifier(FriendlyByteBuf buf, AttributeModifier modifier) {
		buf.writeUtf(modifier.getName());
		buf.writeDouble(modifier.getAmount());
		buf.writeInt(modifier.getOperation().toValue());
	}

	public static MobEffectInstance readStatusEffect(JsonElement jsonElement) {
		if (jsonElement.isJsonObject()) {
			JsonObject json = jsonElement.getAsJsonObject();
			String effect = GsonHelper.getAsString(json, "effect");
			MobEffect effectOptional = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(effect));
			if (effectOptional == null)
				throw new JsonSyntaxException("Error reading status effect: could not find status effect with id: " + effect);
			int duration = GsonHelper.getAsInt(json, "duration", 100);
			int amplifier = GsonHelper.getAsInt(json, "amplifier", 0);
			boolean ambient = GsonHelper.getAsBoolean(json, "is_ambient", false);
			boolean showParticles = GsonHelper.getAsBoolean(json, "show_particles", true);
			boolean showIcon = GsonHelper.getAsBoolean(json, "show_icon", true);
			return new MobEffectInstance(effectOptional, duration, amplifier, ambient, showParticles, showIcon);
		} else {
			throw new JsonSyntaxException("Expected status effect to be a json object.");
		}
	}

	public static JsonElement writeStatusEffect(MobEffectInstance instance) {
		JsonObject object = new JsonObject();
		ResourceLocation registryName = ForgeRegistries.MOB_EFFECTS.getKey(instance.getEffect());
		Validate.notNull(registryName, "Unregistered mob effect: %s", instance.getEffect());
		object.addProperty("effect", registryName.toString());
		object.addProperty("duration", instance.getDuration());
		object.addProperty("amplifier", instance.getAmplifier());
		object.addProperty("is_ambient", instance.isAmbient());
		object.addProperty("show_particles", instance.isVisible());
		object.addProperty("show_icon", instance.showIcon());
		return object;
	}

	public static MobEffectInstance readStatusEffect(FriendlyByteBuf buf) {
		ResourceLocation effect = buf.readResourceLocation();
		int duration = buf.readInt();
		int amplifier = buf.readInt();
		boolean ambient = buf.readBoolean();
		boolean showParticles = buf.readBoolean();
		boolean showIcon = buf.readBoolean();
		MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(effect);
		Validate.notNull(mobEffect, "Missing mob effect: %s", effect);
		return new MobEffectInstance(mobEffect, duration, amplifier, ambient, showParticles, showIcon);
	}

	public static void writeStatusEffect(FriendlyByteBuf buf, MobEffectInstance statusEffectInstance) {
		ResourceLocation registryName = ForgeRegistries.MOB_EFFECTS.getKey(statusEffectInstance.getEffect());
		Validate.notNull(registryName, "Unregistered mob effect: %s".formatted(statusEffectInstance.getEffect()));
		buf.writeResourceLocation(registryName);
		buf.writeInt(statusEffectInstance.getDuration());
		buf.writeInt(statusEffectInstance.getAmplifier());
		buf.writeBoolean(statusEffectInstance.isAmbient());
		buf.writeBoolean(statusEffectInstance.isVisible());
		buf.writeBoolean(statusEffectInstance.showIcon());
	}

	public static <T extends Enum<T>> HashMap<String, T> buildEnumMap(Class<T> enumClass, Function<T, String> enumToString) {
		HashMap<String, T> map = new HashMap<>();
		for (T enumConstant : enumClass.getEnumConstants()) {
			map.put(enumToString.apply(enumConstant), enumConstant);
		}
		return map;
	}

	@SuppressWarnings("deprecation")
	public static <T extends ParticleOptions> T loadParticle(ParticleType<T> type, String parameters) {
		//There is no way around deserializers right now.
		ParticleOptions.Deserializer<T> factory = type.getDeserializer();
		try {
			return factory.fromCommand(type, new StringReader(" " + parameters));
		} catch (CommandSyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
