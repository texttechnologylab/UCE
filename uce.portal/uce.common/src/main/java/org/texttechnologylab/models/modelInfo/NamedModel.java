package org.texttechnologylab.models.modelInfo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the name of a model.
 * This can be used to annotate classes that represent models
 * in the Text Technology Lab framework.
 * Use this to ensure consistency with the name of the model
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NamedModel {

    /**
     * The name of the model.
     *
     * @return the name of the model
     */
    String name();

}
