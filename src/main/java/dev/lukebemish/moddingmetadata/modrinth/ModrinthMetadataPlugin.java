package dev.lukebemish.moddingmetadata.modrinth;

import dev.lukebemish.moddingmetadata.CapabilityFabricModJsonMavenVersionRule;
import dev.lukebemish.moddingmetadata.CapabilitySameVersionRule;
import dev.lukebemish.moddingmetadata.Identifier;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler;
import org.gradle.api.initialization.Settings;

public class ModrinthMetadataPlugin implements Plugin<Object> {
    @Override
    public void apply(Object target) {
        ComponentMetadataHandler components;
        if (target instanceof Project project) {
            components = project.getDependencies().getComponents();
        } else if (target instanceof Settings settings) {
            components = settings.getDependencyResolutionManagement().getComponents();
        } else {
            throw new IllegalArgumentException("Plugin must be applied to a project or settings object");
        }

        // Unfortunately we can't just target the one group, so we have to apply it to everything and filter in the rule
        components.all(ModrinthMetadataRule.class);

        components.withModule("maven.modrinth:fabric-api", CapabilityFabricModJsonMavenVersionRule.class, rule -> {
            rule.params(new Identifier("net.fabricmc:fabric-api", "fabric-api"));
        });
        components.withModule("maven.modrinth:P7dR8mSH", CapabilityFabricModJsonMavenVersionRule.class, rule -> {
            rule.params(new Identifier("net.fabricmc:fabric-api", "fabric-api"));
        });
    }
}
