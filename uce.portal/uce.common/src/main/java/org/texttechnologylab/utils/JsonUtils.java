package org.texttechnologylab.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonUtils {

    public static Object convertJsonElement(JsonElement jsonElement) {
        if (jsonElement.isJsonObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                map.put(entry.getKey(), convertJsonElement(entry.getValue()));
            }
            return map;
        } else if (jsonElement.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                list.add(convertJsonElement(element));
            }
            return list;
        } else if (jsonElement.isJsonPrimitive()) {
            JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            if (primitive.isNumber()) return primitive.getAsNumber();
            if (primitive.isString()) return primitive.getAsString();
        }
        return null;
    }
}
