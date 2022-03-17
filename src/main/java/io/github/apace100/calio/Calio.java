package io.github.apace100.calio;

import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.apace100.calio.util.ClientTagManagerGetter;
import io.github.apace100.calio.util.ServerTagManagerGetter;
import io.github.apace100.calio.util.TagManagerGetter;
import io.github.edwinmindcraft.calio.common.CalioCommon;
import io.github.edwinmindcraft.calio.common.CalioConfig;
import io.github.edwinmindcraft.calio.common.network.CalioNetwork;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod(CalioAPI.MODID)
public class Calio {

	/**
	 * This is a thing I've seen a certain modder whose name shan't be mentioned do.<br/>
	 * It's Reika, the modder in question is Reika.
	 */
	public static boolean isDebugMode() {
		return CalioConfig.COMMON.debugMode.get();
	}

	public static final ResourceLocation PACKET_SHARE_ITEM = new ResourceLocation("calio", "share_item");

	static TagManagerGetter tagManagerGetter;

	public Calio() {
		CalioAPI.LOGGER.info("Calio {} initializing...", ModLoadingContext.get().getActiveContainer().getModInfo().getVersion());
		CalioCommon.initialize();
		CalioNetwork.register();
		CriteriaTriggers.register(CodeTriggerCriterion.INSTANCE);
		tagManagerGetter = DistExecutor.safeRunForDist(() -> ClientTagManagerGetter::new, () -> ServerTagManagerGetter::new);
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
		return stack.hasTag() && stack.getTag().contains(NbtConstants.ADDITIONAL_ATTRIBUTES) && stack.getTag().getBoolean(NbtConstants.ADDITIONAL_ATTRIBUTES);
	}

	/**
	 * Sets whether the item stack counts the entity attribute modifiers specified in its tag as additional,
	 * meaning they won't overwrite the equipment's inherent modifiers.
	 *
	 * @param stack
	 * @param additional
	 */
	public static void setEntityAttributesAdditional(ItemStack stack, boolean additional) {
		if (stack != null) {
			if (additional) {
				stack.getOrCreateTag().putBoolean(NbtConstants.ADDITIONAL_ATTRIBUTES, true);
			} else {
				if (stack.hasTag()) {
					stack.getTag().remove(NbtConstants.ADDITIONAL_ATTRIBUTES);
				}
			}
		}
	}

	public static TagContainer getTagManager() {
		return tagManagerGetter.get();
	}

	public static <T> boolean areTagsEqual(ResourceKey<? extends Registry<T>> registryKey, Tag<T> tag1, Tag<T> tag2) {
		if (tag1 == tag2) {
			return true;
		}
		if (tag1 == null || tag2 == null) {
			return false;
		}
		TagContainer tagManager = Calio.getTagManager();
		try {
			ResourceLocation id1;
			if (tag1 instanceof Tag.Named)
				id1 = ((Tag.Named<?>) tag1).getName();
			else
				id1 = tagManager.getIdOrThrow(registryKey, tag1, RuntimeException::new);
			ResourceLocation id2;
			if (tag2 instanceof Tag.Named)
				id2 = ((Tag.Named<?>) tag2).getName();
			else
				id2 = tagManager.getIdOrThrow(registryKey, tag2, RuntimeException::new);
			return id1.equals(id2);
		} catch (Exception e) {
			return false;
		}
	}
}
