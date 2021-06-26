package io.github.apace100.calio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.fluid.Fluid;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;

public class SerializationHelper {

    public static Tag<Fluid> getFluidTagFromId(Identifier id) {
        return FluidTags.getTagGroup().getTag(id);
    }

    public static Tag<Block> getBlockTagFromId(Identifier id) {
        return TagRegistry.block(id);
    }

    public static EntityAttributeModifier readAttributeModifier(JsonElement jsonElement) {
        if(jsonElement.isJsonObject()) {
            JsonObject json = jsonElement.getAsJsonObject();
            String name = JsonHelper.getString(json, "name", "Unnamed attribute modifier");
            String operation = JsonHelper.getString(json, "operation").toUpperCase();
            double value = JsonHelper.getFloat(json, "value");
            return new EntityAttributeModifier(name, value, EntityAttributeModifier.Operation.valueOf(operation));
        }
        throw new JsonSyntaxException("Attribute modifier needs to be a JSON object.");
    }

    public static JsonElement writeAttributeModifier(EntityAttributeModifier modifier) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", modifier.getName());
        obj.addProperty("operation", modifier.getOperation().name());
        obj.addProperty("value", modifier.getValue());
        return obj;
    }

    public static EntityAttributeModifier readAttributeModifier(PacketByteBuf buf) {
        String modName = buf.readString(32767);
        double modValue = buf.readDouble();
        int operation = buf.readInt();
        return new EntityAttributeModifier(modName, modValue, EntityAttributeModifier.Operation.fromId(operation));
    }

    public static void writeAttributeModifier(PacketByteBuf buf, EntityAttributeModifier modifier) {
        buf.writeString(modifier.getName());
        buf.writeDouble(modifier.getValue());
        buf.writeInt(modifier.getOperation().getId());
    }

    public static StatusEffectInstance readStatusEffect(JsonElement jsonElement) {
        if(jsonElement.isJsonObject()) {
            JsonObject json = jsonElement.getAsJsonObject();
            String effect = JsonHelper.getString(json, "effect");
            Optional<StatusEffect> effectOptional = Registry.STATUS_EFFECT.getOrEmpty(Identifier.tryParse(effect));
            if(!effectOptional.isPresent()) {
                throw new JsonSyntaxException("Error reading status effect: could not find status effect with id: " + effect);
            }
            int duration = JsonHelper.getInt(json, "duration", 100);
            int amplifier = JsonHelper.getInt(json, "amplifier", 0);
            boolean ambient = JsonHelper.getBoolean(json, "is_ambient", false);
            boolean showParticles = JsonHelper.getBoolean(json, "show_particles", true);
            boolean showIcon = JsonHelper.getBoolean(json, "show_icon", true);
            return new StatusEffectInstance(effectOptional.get(), duration, amplifier, ambient, showParticles, showIcon);
        } else {
            throw new JsonSyntaxException("Expected status effect to be a json object.");
        }
    }

    public static JsonElement writeStatusEffect(StatusEffectInstance instance) {
        JsonObject object = new JsonObject();
        object.addProperty("effect", Registry.STATUS_EFFECT.getId(instance.getEffectType()).toString());
        object.addProperty("duration", instance.getDuration());
        object.addProperty("amplifier", instance.getAmplifier());
        object.addProperty("is_ambient", instance.isAmbient());
        object.addProperty("show_particles", instance.shouldShowParticles());
        object.addProperty("show_icon", instance.shouldShowIcon());
        return object;
    }

    public static StatusEffectInstance readStatusEffect(PacketByteBuf buf) {
        Identifier effect = buf.readIdentifier();
        int duration = buf.readInt();
        int amplifier = buf.readInt();
        boolean ambient = buf.readBoolean();
        boolean showParticles = buf.readBoolean();
        boolean showIcon = buf.readBoolean();
        return new StatusEffectInstance(Registry.STATUS_EFFECT.get(effect), duration, amplifier, ambient, showParticles, showIcon);
    }

    public static void writeStatusEffect(PacketByteBuf buf, StatusEffectInstance statusEffectInstance) {
        buf.writeIdentifier(Registry.STATUS_EFFECT.getId(statusEffectInstance.getEffectType()));
        buf.writeInt(statusEffectInstance.getDuration());
        buf.writeInt(statusEffectInstance.getAmplifier());
        buf.writeBoolean(statusEffectInstance.isAmbient());
        buf.writeBoolean(statusEffectInstance.shouldShowParticles());
        buf.writeBoolean(statusEffectInstance.shouldShowIcon());
    }

    public static <T extends Enum<T>> HashMap<String, T> buildEnumMap(Class<T> enumClass, Function<T, String> enumToString) {
        HashMap<String, T> map = new HashMap<>();
        for (T enumConstant : enumClass.getEnumConstants()) {
            map.put(enumToString.apply(enumConstant), enumConstant);
        }
        return map;
    }
}
