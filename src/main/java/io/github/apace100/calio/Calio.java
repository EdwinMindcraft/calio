package io.github.apace100.calio;

import io.github.edwinmindcraft.calio.api.CalioAPI;
import io.github.edwinmindcraft.calio.common.CalioCommon;
import io.github.edwinmindcraft.calio.common.CalioConfig;
import io.github.edwinmindcraft.calio.common.util.ComponentConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;

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

	public Calio(IEventBus bus) {
		CalioAPI.LOGGER.info("Calio {} initializing...", ModLoadingContext.get().getActiveContainer().getModInfo().getVersion());
		CalioCommon.initialize(bus);
		// FIXME: Register Criteria Triggers through DeferredRegister.
		//CriteriaTriggers.register(CodeTriggerCriterion.INSTANCE);
		//tagManagerGetter = DistExecutor.safeRunForDist(() -> ClientTagManagerGetter::new, () -> ServerTagManagerGetter::new);
	}

	public static boolean hasNonItalicName(ItemStack stack) {
		return stack.has(ComponentConstants.NON_ITALIC_NAME.get());
	}

	public static void setNameNonItalic(ItemStack stack) {
		if (stack != null) {
			DataComponentPatch.Builder builder = DataComponentPatch.builder();
			builder.set(ComponentConstants.NON_ITALIC_NAME.get(), Unit.INSTANCE);
			stack.applyComponents(builder.build());
		}
	}

	public static boolean areEntityAttributesAdditional(ItemStack stack) {
		return stack.has(ComponentConstants.ADDITIONAL_ATTRIBUTES);
	}

	/**
	 * Sets whether the item stack counts the entity attribute modifiers specified in its tag as additional,
	 * meaning they won't overwrite the equipment's inherent modifiers.
	 *
	 * @param stack      The {@link ItemStack} to set the status of.
	 * @param additional The status to set.
	 */
	public static void setEntityAttributesAdditional(ItemStack stack, boolean additional) {
		if (stack != null) {
			if (additional) {
				DataComponentPatch.Builder builder = DataComponentPatch.builder();
				builder.set(ComponentConstants.ADDITIONAL_ATTRIBUTES.get(), Unit.INSTANCE);
				stack.applyComponents(builder.build());
			} else if (stack.has(ComponentConstants.ADDITIONAL_ATTRIBUTES)) {
				stack.remove(ComponentConstants.ADDITIONAL_ATTRIBUTES);
			}
		}
	}

	/**
	 * @deprecated Are tags equals is strictly equivalent to {@link TagKey#equals(Object)}
	 */
	@Deprecated
	public static <T> boolean areTagsEqual(ResourceKey<? extends Registry<T>> registryKey, TagKey<T> tag1, TagKey<T> tag2) {
		return areTagsEqual(tag1, tag2);
	}

	/**
	 * @deprecated Are tags equals is strictly equivalent to {@link TagKey#equals(Object)}
	 */
	@Deprecated
	public static <T> boolean areTagsEqual(TagKey<T> tag1, TagKey<T> tag2) {
		if (tag1 == tag2) {
			return true;
		}
		if (tag1 == null || tag2 == null) {
			return false;
		}
		if (!tag1.registry().equals(tag2.registry())) {
			return false;
		}
		return tag1.location().equals(tag2.location());
	}
}
