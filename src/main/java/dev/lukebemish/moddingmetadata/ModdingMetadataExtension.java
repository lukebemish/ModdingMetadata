package dev.lukebemish.moddingmetadata;

import dev.lukebemish.moddingmetadata.modrinth.ModrinthMetadataRule;
import dev.lukebemish.moddingmetadata.modrinth.ModrinthMetadataSettings;
import org.gradle.api.Action;
import org.gradle.api.IsolatedAction;
import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;
import java.util.function.Consumer;

public abstract class ModdingMetadataExtension {
    @SuppressWarnings("UnstableApiUsage")
    private final Consumer<IsolatedAction<Project>> projectConfig;
    private final ComponentMetadataHandler components;

    @SuppressWarnings("UnstableApiUsage")
    @Inject
    public ModdingMetadataExtension(Consumer<IsolatedAction<Project>> projectConfig, ComponentMetadataHandler components) {
        this.projectConfig = projectConfig;
        this.components = components;
    }

    @Inject
    protected abstract ObjectFactory getObjectFactory();

    public void modrinth() {
        modrinth(it -> {});
    }

    public void modrinth(Action<ModrinthMetadataSettings> action) {
        var settings = getObjectFactory().newInstance(ModrinthMetadataSettings.class);
        action.execute(settings);

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
                            details.select(options.get(0)).because("Other available options were from the modrinth maven");
                        }
                    });
                });
            });
        });

        components.all(ModrinthMetadataRule.class, config -> {
            config.params(settings.getFabricModuleMaps().get());
        });

        settings.getFabricModuleMaps().get().forEach((slugOrId, to) -> components.withModule("maven.modrinth:"+slugOrId, FabricDirectsToRule.class, rule -> {
            rule.params(new Identifier(to.group(), to.name()));
        }));
        settings.getFabricModuleEquivalences().get().forEach((slugOrId, to) -> components.withModule("maven.modrinth:"+slugOrId, FabricProvidesRule.class, rule -> {
            rule.params(new Identifier(to.group(), to.name()));
        }));
    }
}
