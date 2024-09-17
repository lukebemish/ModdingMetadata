package dev.lukebemish.moddingmetadata;

import org.gradle.api.artifacts.CacheableRule;
import org.gradle.api.artifacts.ComponentMetadataContext;
import org.gradle.api.artifacts.ComponentMetadataRule;

import javax.inject.Inject;

@CacheableRule
public abstract class CapabilitySameVersionRule implements ComponentMetadataRule {
    private final Identifier newCapability;

    @Inject
    public CapabilitySameVersionRule(Identifier newCapability) {
        this.newCapability = newCapability;
    }

    @Override
    public void execute(ComponentMetadataContext context) {
        var version = context.getDetails().getId().getVersion();
        context.getDetails().allVariants(variant -> {
            variant.withCapabilities(capabilities -> {
                capabilities.addCapability(newCapability.group(), newCapability.name(), version);
            });
        });
    }
}
