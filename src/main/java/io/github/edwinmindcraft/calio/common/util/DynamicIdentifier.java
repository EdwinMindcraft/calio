package io.github.edwinmindcraft.calio.common.util;

import com.google.gson.JsonElement;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;

// FORGE: Don't extend ResourceLocation.
public class DynamicIdentifier {

    public static ResourceLocation of(JsonElement jsonElement) {
        return of(jsonElement.getAsString());
    }

    public static ResourceLocation of(String idString) {
        return of(idString, ResourceLocation.DEFAULT_NAMESPACE);
    }

    public static ResourceLocation of(String idString, String defaultNamespace) {

        String[] namespaceAndPath = splitWithNamespace(idString, defaultNamespace);
        if (namespaceAndPath[0].contains("*")) {
            if (SerializableData.CURRENT_NAMESPACE != null) {
                namespaceAndPath[0] = namespaceAndPath[0].replace("*", SerializableData.CURRENT_NAMESPACE);
            } else {
                throw new ResourceLocationException("Identifiers may only contain '*' in its namespace in data loaders that support it.");
            }
        }

        if (namespaceAndPath[1].contains("*")) {
            if (SerializableData.CURRENT_PATH != null) {
                namespaceAndPath[1] = namespaceAndPath[1].replace("*", SerializableData.CURRENT_PATH);
            } else {
                throw new ResourceLocationException("Identifiers may only contain '*' in its path in data loaders that support it.");
            }
        }

        return ResourceLocation.fromNamespaceAndPath(namespaceAndPath[0], namespaceAndPath[1]);

    }

    public static String[] splitWithNamespace(String idString, String defaultNamespace) {

        String[] namespaceAndPath = idString.split(String.valueOf(ResourceLocation.NAMESPACE_SEPARATOR));
        if (namespaceAndPath.length > 2) {
            throw new ResourceLocationException("Identifier \"" + idString + "\" must only have one \":\" separating its namespace and path.");
        }

        if (namespaceAndPath.length == 1) {
            return new String[]{defaultNamespace, namespaceAndPath[0]};
        }

        return namespaceAndPath;

    }

}
