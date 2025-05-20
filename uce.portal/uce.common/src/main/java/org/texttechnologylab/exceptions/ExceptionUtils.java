package org.texttechnologylab.exceptions;

import org.texttechnologylab.models.imp.ImportLog;
import org.texttechnologylab.models.imp.LogStatus;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class ExceptionUtils {

    /**
     * Functional way of doing a whole try,catch for a single method that returns a value.
     * It returns NULL, should an error be caught!
     * @param function
     * @param logCallback
     * @return
     * @param <T>
     */
    public static <T> T tryCatchLog(Callable<T> function, Consumer<Exception> logCallback) {
        try {
            return function.call();
        } catch (Exception e) {
            logCallback.accept(e);
            return null;
        }
    }

    /**
     * Functional way of doing a whole try,catch for a single method that doesn't return a value.
     */
    public static void tryCatchLog(RunnableWithException function, Consumer<Exception> logCallback) {
        try {
            function.run();
        } catch (Exception e) {
            logCallback.accept(e);
        }
    }

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }
}
