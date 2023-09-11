package io.github.edwinmindcraft.calio.common.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;

public class ForgeConditionCodec implements Codec<ICondition> {

    public static final ForgeConditionCodec INSTANCE = new ForgeConditionCodec();

    @Override
    public <T> DataResult<Pair<ICondition, T>> decode(DynamicOps<T> ops, T input) {
        JsonElement json = ops.convertMap(JsonOps.INSTANCE, input);
        if (!json.isJsonObject())
            return DataResult.error(() -> "Forge Condition JSON is not a JsonObject");
        try {
            ICondition condition = CraftingHelper.getCondition(json.getAsJsonObject());
            return DataResult.success(Pair.of(condition, ops.empty()));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to deserialize Forge Condition JSON: " + e.getMessage());
        }
    }

    @Override
    public <T> DataResult<T> encode(ICondition input, DynamicOps<T> ops, T prefix) {
        try {
            JsonObject object = CraftingHelper.serialize(input);
            return DataResult.success(JsonOps.INSTANCE.convertTo(ops, object));
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to serialize Forge Condition JSON:" + e.getMessage());
        }
    }
}
