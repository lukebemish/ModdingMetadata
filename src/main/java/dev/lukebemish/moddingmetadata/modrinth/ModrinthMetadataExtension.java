package dev.lukebemish.moddingmetadata.modrinth;

import dev.lukebemish.moddingmetadata.CapabilityFabricModJsonMavenVersionRule;
import dev.lukebemish.moddingmetadata.Identifier;
import org.gradle.api.IsolatedAction;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler;

import javax.inject.Inject;
import java.util.function.Consumer;

public abstract class ModrinthMetadataExtension {
    @SuppressWarnings("UnstableApiUsage")
    private final Consumer<IsolatedAction<Project>> projectConfig;
    private final ComponentMetadataHandler components;

    @Inject
    public ModrinthMetadataExtension(Consumer<IsolatedAction<Project>> projectConfig, ComponentMetadataHandler components) {
        this.components = components;
        this.projectConfig = projectConfig;
    }

    void mapToMavenFabric(String slugOrId, String group, String name) {
        components.withModule("maven.modrinth:"+slugOrId, CapabilityFabricModJsonMavenVersionRule.class, rule -> {
            rule.params(new Identifier(group, name));
        });
    }
}
