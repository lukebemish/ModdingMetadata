package dev.lukebemish.moddingmetadata;

import org.gradle.api.artifacts.CacheableRule;
import org.gradle.api.artifacts.ComponentMetadataContext;
import org.gradle.api.artifacts.ComponentMetadataRule;
import org.gradle.api.artifacts.MutableVariantFilesMetadata;
import org.gradle.api.artifacts.repositories.RepositoryResourceAccessor;
import org.jetbrains.annotations.ApiStatus;

import javax.inject.Inject;
import java.util.Map;

@CacheableRule
@ApiStatus.Internal
public abstract class FabricDirectsToRule implements ComponentMetadataRule {
    private final Identifier newLocation;

    @Inject
    public FabricDirectsToRule(Identifier newLocation) {
        this.newLocation = newLocation;
    }

    @Inject
    protected abstract RepositoryResourceAccessor getRepositoryResourceAccessor();

    @Override
    public void execute(ComponentMetadataContext context) {
        var group = context.getDetails().getId().getGroup();
        var name = context.getDetails().getId().getName();
        var oldVersion = context.getDetails().getId().getVersion();

        newLocation.fabricRecover(getRepositoryResourceAccessor(), identifier -> context.getDetails().allVariants(variant -> {
            variant.withFiles(MutableVariantFilesMetadata::removeAllFiles);
            variant.withDependencies(deps -> deps.add(Map.of(
                "group", identifier.identifier().group(),
                "name", identifier.identifier().name(),
                "version", identifier.version()
            )));
        }), group, name, oldVersion);
    }
}
