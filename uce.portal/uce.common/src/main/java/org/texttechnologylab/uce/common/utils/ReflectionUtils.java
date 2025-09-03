package org.texttechnologylab.uce.common.utils;

import org.apache.uima.jcas.tcas.Annotation;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;
import org.texttechnologylab.uce.common.annotations.Taxonsystem;
import org.texttechnologylab.uce.common.annotations.Typesystem;

import javax.persistence.Table;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ReflectionUtils {

    /**
     * This holds all types of classes within the org.texttechnologylab.models, that have a Typesystem annotation
     * and were hence created from Typesystems.
     */
    private static final Set<Class<?>> TYPESYSTEM_MODEL_CLASSES;
    static {
        var reflections = new Reflections(new ConfigurationBuilder()
                .forPackage("org.texttechnologylab.models")
                .addScanners(Scanners.TypesAnnotated)
        );
        TYPESYSTEM_MODEL_CLASSES = reflections.getTypesAnnotatedWith(Typesystem.class);
    }

    /**
     * Given any CAS Annotation, we want to find the UCE class which was *built* using that Typesystem. This type is then
     * returned.
     */
    public static Class<?> findModelClassForCASAnnotation(Annotation toAnnotation) {
        Class<?> targetClass = toAnnotation.getClass();
        for (Class<?> modelClass : TYPESYSTEM_MODEL_CLASSES) {
            Typesystem ts = modelClass.getAnnotation(Typesystem.class);
            if (ts != null) {
                for (Class<?> type : ts.types()) {
                    if (type.isAssignableFrom(targetClass)) {
                        return modelClass;
                    }
                }
            }
        }
        return null;
    }

    public static String getTableAnnotationName(Class<?> modelClass) {
        if (modelClass == null) return null;
        var table = modelClass.getAnnotation(Table.class);
        return table != null ? table.name() : null;
    }

    public static List<String> getTaxonSystemTypes(Class<?> modelClass) {
        if (modelClass == null) return null;
        var taxonsystem = modelClass.getAnnotation(Taxonsystem.class);
        return taxonsystem != null ? Arrays.asList(taxonsystem.types()) : null;
    }

    public static <T> Class<? extends T> getClassFromClassName(String name, Class<T> baseType) throws ClassNotFoundException {
        Class<?> rawClass = Class.forName(name);

        if (!baseType.isAssignableFrom(rawClass)) {
            throw new IllegalArgumentException("Class " + name + " does not extend or implement " + baseType.getName());
        }

        @SuppressWarnings("unchecked")
        Class<? extends T> typedClass = (Class<? extends T>) rawClass;

        return typedClass;
    }

}
