package io.github.apace100.calio;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class CodeTriggerCriterion extends SimpleCriterionTrigger<CodeTriggerCriterion.Conditions> {

	public static final CodeTriggerCriterion INSTANCE = new CodeTriggerCriterion();

	public static final ResourceLocation ID = new ResourceLocation("apacelib", "code_trigger");

	@NotNull
	@Override
	public ResourceLocation getId() {
		return ID;
	}

	@Override
	@NotNull
	protected Conditions createInstance(JsonObject jsonObject, @NotNull ContextAwarePredicate extended, @NotNull DeserializationContext context) {
		String triggerId = "empty";
		if (jsonObject.has("trigger_id")) {
			triggerId = jsonObject.get("trigger_id").getAsString();
		}
		return new CodeTriggerCriterion.Conditions(extended, triggerId);
	}

	public void trigger(ServerPlayer player, String triggeredId) {
		this.trigger(player, (conditions) -> conditions.matches(triggeredId));
	}

	public static class Conditions extends AbstractCriterionTriggerInstance {
		private final String triggerId;

		public Conditions(ContextAwarePredicate player, String triggerId) {
			super(CodeTriggerCriterion.ID, player);
			this.triggerId = triggerId;
		}

		public static CodeTriggerCriterion.Conditions trigger(String triggerId) {
			return new CodeTriggerCriterion.Conditions(ContextAwarePredicate.ANY, triggerId);
		}

		public boolean matches(String triggered) {
			return this.triggerId.equals(triggered);
		}

		@Override
		public @NotNull JsonObject serializeToJson(@NotNull SerializationContext context) {
			JsonObject jsonObject = super.serializeToJson(context);
			jsonObject.add("trigger_id", new JsonPrimitive(this.triggerId));
			return jsonObject;
		}
	}
}
