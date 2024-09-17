package dev.lukebemish.moddingmetadata.modrinth;

import dev.lukebemish.moddingmetadata.CapabilityFabricModJsonMavenVersionRule;
import dev.lukebemish.moddingmetadata.CapabilitySameVersionRule;
import dev.lukebemish.moddingmetadata.Identifier;
import org.gradle.api.IsolatedAction;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler;
import org.gradle.api.initialization.Settings;

import java.util.List;
import java.util.function.Consumer;

public class ModrinthMetadataPlugin implements Plugin<Object> {
    private record Entry(String slug, String name, Identifier identifier) {}
    private static final List<Entry> FABRIC_ENTRIES = List.of(
            new Entry("fabric-api", "P7dR8mSH", new Identifier("net.fabricmc.fabric-api", "fabric-api"))
    );

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void apply(Object target) {
        ComponentMetadataHandler components;
        Consumer<IsolatedAction<Project>> projectConfig;
        if (target instanceof Project project) {
            components = project.getDependencies().getComponents();
            projectConfig = action -> action.execute(project);
        } else if (target instanceof Settings settings) {
            components = settings.getDependencyResolutionManagement().getComponents();
            projectConfig = action -> settings.getGradle().getLifecycle().beforeProject(action);
        } else {
            throw new IllegalArgumentException("Plugin must be applied to a project or settings object");
        }

        // Unfortunately we can't just target the one group, so we have to apply it to everything and filter in the rule
        components.all(ModrinthMetadataRule.class);

        projectConfig.accept(project -> {
            project.getConfigurations().configureEach(configuration -> {
                configuration.resolutionStrategy(strategy -> {
                    strategy.getCapabilitiesResolution().all(details -> {
                        if ("maven.modrinth".equals(details.getCapability().getGroup())) {
                            return;
                        }
                        var options = details.getCandidates().stream().filter(it ->
                                !(it.getId() instanceof ModuleComponentIdentifier moduleId) || !"maven.modrinth".equals(moduleId.getGroup())
                        ).toList();
                        if (options.size() == 1) {
                            details.select(options.get(0)).because("Selected as other available options were from the modrinth maven");
                        }
                    });
                });
            });
        });

        FABRIC_ENTRIES.forEach(entry -> {
            components.withModule("maven.modrinth:"+entry.slug, CapabilityFabricModJsonMavenVersionRule.class, rule -> {
                rule.params(entry.identifier);
            });
            components.withModule("maven.modrinth:"+entry.name, CapabilityFabricModJsonMavenVersionRule.class, rule -> {
                rule.params(entry.identifier);
            });
        });
    }
}
