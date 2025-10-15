package org.texttechnologylab.uce.common.models.corpus;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class UCEMetadataValueTypeOrdinalAdapter extends TypeAdapter<UCEMetadataValueType> {
    @Override
    public void write(JsonWriter writer, UCEMetadataValueType valueType) throws IOException {
        writer.value(valueType.ordinal());
    }

    @Override
    public UCEMetadataValueType read(JsonReader reader) throws IOException {
        int ordinal = reader.nextInt();
        return UCEMetadataValueType.values()[ordinal];
    }
}
