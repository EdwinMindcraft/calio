package io.github.apace100.calio.util;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Iterator;
import java.util.Map;

public class JsonTextFormatter {

    private static final ChatFormatting NULL_COLOR = ChatFormatting.LIGHT_PURPLE;
    private static final ChatFormatting NAME_COLOR = ChatFormatting.AQUA;
    private static final ChatFormatting STRING_COLOR = ChatFormatting.GREEN;
    private static final ChatFormatting NUMBER_COLOR = ChatFormatting.GOLD;
    private static final ChatFormatting BOOLEAN_COLOR = ChatFormatting.BLUE;
    private static final ChatFormatting TYPE_SUFFIX_COLOR = ChatFormatting.RED;

    private final String indent;
    private final int indentOffset;

    private Component result;
    private boolean root;

    public JsonTextFormatter(String indent) {
        this(indent, 1);
    }

    protected JsonTextFormatter(String indent, int indentOffset) {
        this.indent = indent;
        this.indentOffset = Math.max(0, indentOffset);
        this.result = CommonComponents.EMPTY;
        this.root = true;
    }

    public Component apply(JsonElement jsonElement) {

        if (!handleJsonElement(jsonElement)) {
            throw new JsonParseException("The format of the specified JSON element is not supported!");
        }

        return this.result;

    }

    protected Component apply(JsonElement jsonElement, boolean rootElement) {
        this.root = rootElement;
        return apply(jsonElement);
    }

    protected final boolean handleJsonElement(JsonElement jsonElement) {

        if (jsonElement instanceof JsonArray jsonArray) {
            visitArray(jsonArray);
            return true;
        }

        else if (jsonElement instanceof JsonObject jsonObject) {
            visitObject(jsonObject);
            return true;
        }

        else if (jsonElement instanceof JsonPrimitive jsonPrimitive) {
            visitPrimitive(jsonPrimitive);
            return true;
        }

        else if (jsonElement instanceof JsonNull) {
            this.result = Component.literal("null").withStyle(NULL_COLOR);
            return true;
        }

        return false;

    }

    public void visitArray(JsonArray jsonArray) {

        if (jsonArray.isEmpty()) {
            this.result = Component.literal("[]");
            return;
        }

        MutableComponent result = Component.literal("[");
        if (!indent.isEmpty()) {
            result.append("\n");
        }

        Iterator<JsonElement> iterator = jsonArray.iterator();
        while (iterator.hasNext()) {

            JsonElement jsonElement = iterator.next();
            result
                .append(Strings.repeat(indent, indentOffset))
                .append(new JsonTextFormatter(indent, indentOffset + 1).apply(jsonElement, false));

            if (iterator.hasNext()) {
                result.append(!indent.isEmpty() ? ",\n" : ", ");
            }

        }

        if (!indent.isEmpty()) {
            result.append("\n");
        }

        if (!root) {
            result.append(Strings.repeat(indent, indentOffset - 1));
        }

        result.append("]");
        this.result = result;

    }

    public void visitObject(JsonObject jsonObject) {

        if (jsonObject.keySet().isEmpty()) {
            this.result = Component.literal("{}");
            return;
        }

        MutableComponent result = Component.literal("{");
        if (!indent.isEmpty()) {
            result.append("\n");
        }

        Iterator<Map.Entry<String, JsonElement>> iterator = jsonObject.entrySet().iterator();
        while (iterator.hasNext()) {

            Map.Entry<String, JsonElement> entry = iterator.next();
            Component name = Component.literal(entry.getKey()).withStyle(NAME_COLOR);

            result
                .append(Strings.repeat(indent, indentOffset))
                .append(name).append(": ")
                .append(new JsonTextFormatter(indent, indentOffset + 1).apply(entry.getValue(), false));

            if (iterator.hasNext()) {
                result.append(!indent.isEmpty() ? ",\n" : ", ");
            }

        }

        if (!indent.isEmpty()) {
            result.append("\n");
        }

        if (!root) {
            result.append(Strings.repeat(indent, indentOffset - 1));
        }

        result.append("}");
        this.result = result;

    }

    public void visitPrimitive(JsonPrimitive jsonPrimitive) {

        if (!handlePrimitive(jsonPrimitive)) {
            throw new JsonParseException("Specified JSON primitive is not supported!");
        }

    }

    protected final boolean handlePrimitive(JsonPrimitive jsonPrimitive) {

        if (jsonPrimitive.isBoolean()) {
            this.result = Component.literal(String.valueOf(jsonPrimitive.getAsBoolean())).withStyle(BOOLEAN_COLOR);
            return true;
        }

        else if (jsonPrimitive.isString()) {
            this.result = Component.literal("\"" + jsonPrimitive.getAsString() + "\"").withStyle(STRING_COLOR);
            return true;
        }

        else if (jsonPrimitive.isNumber()) {

            Number number = jsonPrimitive.getAsNumber();
            Component numberText;

            if (number instanceof Integer i) {
                numberText = Component.literal(String.valueOf(i)).withStyle(NUMBER_COLOR);
            }

            else if (number instanceof Long l) {
                numberText = Component.literal(String.valueOf(l)).withStyle(NUMBER_COLOR)
                    .append(Component.literal("L").withStyle(TYPE_SUFFIX_COLOR));
            }

            else if (number instanceof Float f) {
                numberText = Component.literal(String.valueOf(f)).withStyle(NUMBER_COLOR)
                    .append(Component.literal("F").withStyle(TYPE_SUFFIX_COLOR));
            }

            else if (number instanceof Double d) {
                numberText = Component.literal(String.valueOf(d)).withStyle(NUMBER_COLOR)
                    .append(Component.literal("D").withStyle(TYPE_SUFFIX_COLOR));
            }

            else if (number instanceof Byte b) {
                numberText = Component.literal(String.valueOf(b)).withStyle(NUMBER_COLOR)
                    .append(Component.literal("B")).withStyle(TYPE_SUFFIX_COLOR);
            }

            else if (number instanceof Short s) {
                numberText = Component.literal(String.valueOf(s)).withStyle(NUMBER_COLOR)
                    .append(Component.literal("S")).withStyle(TYPE_SUFFIX_COLOR);
            }

            else {
                return false;
            }

            this.result = numberText;
            return true;

        }

        return false;

    }

}