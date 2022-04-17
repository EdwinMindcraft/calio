package io.github.apace100.calio;

import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.common.CalioCommon;
import io.github.edwinmindcraft.calio.common.CalioConfig;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

@Mod(CalioAPI.MODID)
public class Calio {

	/**
	 * This is a thing I've seen a certain modder whose name shan't be mentioned do.<br/>
	 * It's Reika, the modder in question is Reika.
	 */
	public static boolean isDebugMode() {
		return CalioConfig.COMMON.debugMode.get();
	}

	//static TagManagerGetter tagManagerGetter;

	public Calio() {
		CalioAPI.LOGGER.info("Calio {} initializing...", ModLoadingContext.get().getActiveContainer().getModInfo().getVersion());
		CalioCommon.initialize();
		CriteriaTriggers.register(CodeTriggerCriterion.INSTANCE);
		//tagManagerGetter = DistExecutor.safeRunForDist(() -> ClientTagManagerGetter::new, () -> ServerTagManagerGetter::new);
	}

	public static boolean hasNonItalicName(ItemStack stack) {
		if (stack.hasTag()) {
			CompoundTag display = stack.getTagElement("display");
			return display != null && display.getBoolean(NbtConstants.NON_ITALIC_NAME);
		}
		return false;
	}

	public static void setNameNonItalic(ItemStack stack) {
		if (stack != null)
			stack.getOrCreateTagElement("display").putBoolean(NbtConstants.NON_ITALIC_NAME, true);
	}

	public static boolean areEntityAttributesAdditional(ItemStack stack) {
		return stack.hasTag() && Objects.requireNonNull(stack.getTag()).contains(NbtConstants.ADDITIONAL_ATTRIBUTES) && stack.getTag().getBoolean(NbtConstants.ADDITIONAL_ATTRIBUTES);
	}

	/**
	 * Sets whether the item stack counts the entity attribute modifiers specified in its tag as additional,
	 * meaning they won't overwrite the equipment's inherent modifiers.
	 *
	 * @param stack The {@link ItemStack} to set the status of.
	 * @param additional The status to set.
	 */
	public static void setEntityAttributesAdditional(ItemStack stack, boolean additional) {
		if (stack != null) {
			if (additional)
				stack.getOrCreateTag().putBoolean(NbtConstants.ADDITIONAL_ATTRIBUTES, true);
			else if (stack.hasTag())
				Objects.requireNonNull(stack.getTag()).remove(NbtConstants.ADDITIONAL_ATTRIBUTES);
		}
	}

	public static <T> boolean areTagsEqual(RegistryKey<? extends Registry<T>> registryKey, TagKey<T> tag1, TagKey<T> tag2) {
		return areTagsEqual(tag1, tag2);
	}

	public static <T> boolean areTagsEqual(TagKey<T> tag1, TagKey<T> tag2) {
		if(tag1 == tag2) {
			return true;
		}
		if (tag1 == null || tag2 == null) {
			return false;
		}
		if(!tag1.registry().equals(tag2.registry())) {
			return false;
		}
		if(!tag1.id().equals(tag2.id())) {
			return false;
		}
		return true;
	}
}
