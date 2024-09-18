package dev.lukebemish.moddingmetadata;

import com.google.gson.Gson;
import org.gradle.api.artifacts.CacheableRule;
import org.gradle.api.artifacts.ComponentMetadataContext;
import org.gradle.api.artifacts.ComponentMetadataRule;
import org.gradle.api.artifacts.repositories.RepositoryResourceAccessor;
import org.jetbrains.annotations.ApiStatus;

import javax.inject.Inject;

@CacheableRule
@ApiStatus.Internal
public abstract class FabricProvidesRule implements ComponentMetadataRule {
    private static final Gson GSON = new Gson();

    private final Identifier newCapability;

    @Inject
    public FabricProvidesRule(Identifier newCapability) {
        this.newCapability = newCapability;
    }

    @Inject
    protected abstract RepositoryResourceAccessor getRepositoryResourceAccessor();

    @Override
    public void execute(ComponentMetadataContext context) {
        var group = context.getDetails().getId().getGroup();
        var name = context.getDetails().getId().getName();
        var oldVersion = context.getDetails().getId().getVersion();

        newCapability.fabricRecover(getRepositoryResourceAccessor(), identifier -> context.getDetails().allVariants(variant -> {
            variant.withCapabilities(capabilities -> {
                capabilities.addCapability(identifier.group(), identifier.name(), identifier.version());
            });
        }), group, name, oldVersion);
    }
}
