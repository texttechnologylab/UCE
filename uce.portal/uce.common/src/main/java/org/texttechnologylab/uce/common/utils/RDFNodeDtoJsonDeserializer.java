package org.texttechnologylab.uce.common.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.texttechnologylab.uce.common.models.dto.rdf.RDFNodeDto;
import org.texttechnologylab.uce.common.models.dto.rdf.TripletDto;

import java.lang.reflect.Type;

/**
 * The returned RDF Triplets from the fuseki server are sometimes called "predicate" but also "pred".
 * Hence we need to handle all possible aliases while parsing. We do that with this custom deserializer.
 */
public class RDFNodeDtoJsonDeserializer implements JsonDeserializer<RDFNodeDto> {

    @Override
    public RDFNodeDto deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        JsonObject jsonObject = json.getAsJsonObject();
        var node = new RDFNodeDto();

        node.setSubject(getTripletDto(jsonObject, "subject", "sub", context));
        node.setPredicate(getTripletDto(jsonObject, "predicate", "pred", context));
        node.setObject(getTripletDto(jsonObject, "object", "obj", context));

        return node;
    }

    private TripletDto getTripletDto(JsonObject jsonObject, String primaryName, String aliasName, JsonDeserializationContext context) {
        if (jsonObject.has(primaryName)) {
            return context.deserialize(jsonObject.get(primaryName), TripletDto.class);
        } else if (jsonObject.has(aliasName)) {
            return context.deserialize(jsonObject.get(aliasName), TripletDto.class);
        }
        return null;
    }
}
