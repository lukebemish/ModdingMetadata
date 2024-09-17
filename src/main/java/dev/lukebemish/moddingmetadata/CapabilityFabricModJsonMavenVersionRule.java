package dev.lukebemish.moddingmetadata;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.gradle.api.artifacts.CacheableRule;
import org.gradle.api.artifacts.ComponentMetadataContext;
import org.gradle.api.artifacts.ComponentMetadataRule;
import org.gradle.api.artifacts.repositories.RepositoryResourceAccessor;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

@CacheableRule
public abstract class CapabilityFabricModJsonMavenVersionRule implements ComponentMetadataRule {
    private static final Gson GSON = new Gson();
    
    private final Identifier newCapability;

    @Inject
    public CapabilityFabricModJsonMavenVersionRule(Identifier newCapability) {
        this.newCapability = newCapability;
    }

    @Inject
    protected abstract RepositoryResourceAccessor getRepositoryResourceAccessor();

    @Override
    public void execute(ComponentMetadataContext context) {
        var group = context.getDetails().getId().getGroup();
        var name = context.getDetails().getId().getName();
        var oldVersion = context.getDetails().getId().getVersion();
        
        getRepositoryResourceAccessor().withResource(group.replace('.', '/')+"/"+name+"/"+oldVersion+"/"+name+"-"+oldVersion+".jar", is -> {
            try (var jis = new JarInputStream(is)) {
                JarEntry entry = null;
                while ((entry = jis.getNextJarEntry()) != null) {
                    if (entry.getName().equals("fabric.mod.json")) {
                        break;
                    }
                }
                if (entry != null) {
                    try (var reader = new InputStreamReader(jis)) {
                        var json = GSON.fromJson(reader, JsonObject.class);
                        var version = json.get("version").getAsString();
                        context.getDetails().allVariants(variant -> {
                            variant.withCapabilities(capabilities -> {
                                capabilities.addCapability(newCapability.group(), newCapability.name(), version);
                            });
                        });
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
