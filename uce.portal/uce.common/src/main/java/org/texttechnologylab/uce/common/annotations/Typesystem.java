package org.texttechnologylab.uce.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that stores the original TypeSystems used within CAS, that the appended model class is built from
 * A single entity within UCE can potentially exist from multiple TypeSystems (see Lemma e.g.)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Typesystem {
    /**
     * The list containing the TypeSystem classes .
     */
    Class<?>[] types();

    /**
     * Whether the TypeSystems are optional, or the model can be created without them (e.g. Page)
     */
    boolean optional() default false;
}
