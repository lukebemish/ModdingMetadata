package dev.lukebemish.moddingmetadata;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.gradle.api.Action;
import org.gradle.api.artifacts.DirectDependenciesMetadata;
import org.gradle.api.artifacts.MutableVariantFilesMetadata;
import org.gradle.api.artifacts.VariantMetadata;
import org.gradle.api.artifacts.repositories.RepositoryResourceAccessor;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

@ApiStatus.Internal
public record Identifier(String group, String name) implements Serializable {
    private static final Gson GSON = new Gson();

    public void makeIntoDep(RepositoryResourceAccessor accessor, Consumer<Action<DirectDependenciesMetadata>> contextAction, String group, String name, String oldVersion) {
        accessor.withResource(group.replace('.', '/')+"/"+ name +"/"+ oldVersion +"/"+ name +"-"+ oldVersion +".jar", is -> {
            try (var jis = new JarInputStream(is)) {
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    if (entry.getName().equals("fabric.mod.json")) {
                        break;
                    }
                }

                if (entry != null) {
                    try (var reader = new InputStreamReader(jis)) {
                        var json = GSON.fromJson(reader, JsonObject.class);
                        var version = json.get("version").getAsString();
                        contextAction.accept(deps -> deps.add(Map.of(
                            "group", this.group(),
                            "name", this.name(),
                            "version", version
                        )));
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
