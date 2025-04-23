package org.texttechnologylab.utils;

public class ClassUtils {

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
