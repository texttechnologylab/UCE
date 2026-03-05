package org.texttechnologylab.uce.web.render.spec;

import com.google.gson.Gson;
import org.texttechnologylab.uce.web.render.RenderException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

final class SpecLoader {

    private static final String FILE_PREFIX = "FILE::";
    private static final String CLASSPATH_PREFIX = "CLASSPATH::";

    private final Gson gson = new Gson();

    RenderSpec load(String specPath) throws RenderException {
        if (specPath == null || specPath.isBlank()) {
            throw new RenderException("Render mode specPath is missing/blank.");
        }

        String json;
        try {
            if (specPath.startsWith(FILE_PREFIX)) {
                var path = specPath.substring(FILE_PREFIX.length());
                json = Files.readString(Path.of(path), StandardCharsets.UTF_8);
            } else {
                var resource = specPath.startsWith(CLASSPATH_PREFIX)
                        ? specPath.substring(CLASSPATH_PREFIX.length())
                        : specPath;
                var inputStream = getClass().getClassLoader().getResourceAsStream(resource);
                if (inputStream == null) {
                    throw new RenderException("Spec resource not found in classpath: " + resource);
                }
                try (var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    json = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                }
            }
        } catch (RenderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RenderException("Failed to load spec: " + specPath, ex);
        }

        try {
            var spec = gson.fromJson(json, RenderSpec.class);
            if (spec == null || spec.getMiddle() == null || spec.getMiddle().getTemplate() == null) {
                throw new RenderException("Spec must define middle.template.");
            }
            return spec;
        } catch (RenderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RenderException("Failed to parse spec JSON: " + specPath, ex);
        }
    }
}

