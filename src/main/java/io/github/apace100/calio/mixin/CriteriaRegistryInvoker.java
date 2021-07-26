package io.github.apace100.calio.mixin;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;

/**
 * On forge, use {@link CriteriaTriggers#register(CriterionTrigger)}
 * On fabric use an access widener
 */
@Deprecated
public interface CriteriaRegistryInvoker {

	/**
	 * @deprecated Use {@link CriteriaTriggers#register(CriterionTrigger)} instead
	 */
	@Deprecated
	static <T extends CriterionTrigger<?>> T callRegister(T object) {
		return CriteriaTriggers.register(object);
	}
}
