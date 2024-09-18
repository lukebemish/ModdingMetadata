package dev.lukebemish.moddingmetadata;

import org.gradle.api.IsolatedAction;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.ExtensionAware;

import java.util.function.Consumer;

public class ModdingMetadataPlugin implements Plugin<ExtensionAware> {
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void apply(ExtensionAware target) {
        ComponentMetadataHandler components;
        Consumer<IsolatedAction<Project>> projectConfig;
        if (target instanceof Project project) {
            components = project.getDependencies().getComponents();
            projectConfig = action -> action.execute(project);
        } else if (target instanceof Settings settings) {
            components = settings.getDependencyResolutionManagement().getComponents();
            projectConfig = action -> settings.getGradle().getLifecycle().beforeProject(action);
            projectConfig.accept(project -> {
                var projectComponents = project.getDependencies().getComponents();
                Consumer<IsolatedAction<Project>> projectProjectconfig = action -> action.execute(project);
                project.getExtensions().create("moddingMetadata", ModdingMetadataExtension.class, projectProjectconfig, projectComponents);
            });
        } else {
            throw new IllegalArgumentException("Plugin must be applied to a project or settings object");
        }

        target.getExtensions().create("moddingMetadata", ModdingMetadataExtension.class, projectConfig, components);
    }
}
