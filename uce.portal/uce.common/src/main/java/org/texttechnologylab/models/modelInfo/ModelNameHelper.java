package org.texttechnologylab.models.modelInfo;

import org.jetbrains.annotations.NotNull;

public class ModelNameHelper {

    private ModelNameHelper() {
        // Utility class, no instantiation allowed
    }

    @NotNull
    public static String getModelName(@NotNull Class<?> modelClass) {
        NamedModel namedModel = modelClass.getAnnotation(NamedModel.class);
        if (namedModel == null) {
            throw new IllegalArgumentException("Class " + modelClass.getName() + " is not annotated with @NamedModel");
        }
        String modelName = namedModel.name();
        if (modelName.isEmpty()) {
            throw new IllegalArgumentException("Model name cannot be empty for class " + modelClass.getName());
        }
        return modelName;
    }
}
