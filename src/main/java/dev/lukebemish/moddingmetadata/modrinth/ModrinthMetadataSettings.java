package dev.lukebemish.moddingmetadata.modrinth;

import dev.lukebemish.moddingmetadata.Identifier;
import org.gradle.api.provider.MapProperty;

import javax.inject.Inject;

public abstract class ModrinthMetadataSettings {
    public abstract MapProperty<String, Identifier> getFabricModuleMaps();
    public abstract MapProperty<String, Identifier> getFabricModuleEquivalences();
    public abstract MapProperty<String, Identifier> getModuleEquivalences();

    @Inject
    public ModrinthMetadataSettings() {
        fabricMapsTo("fabric-api", "net.fabricmc.fabric-api", "fabric-api");
        fabricMapsTo("P7dR8mSH", "net.fabricmc.fabric-api", "fabric-api");
    }

    public void fabricMapsTo(String slugOrId, String group, String name) {
        this.getFabricModuleMaps().put(slugOrId, new Identifier(group, name));
    }

    public void fabricEquivalent(String slugOrId, String group, String name) {
        this.getFabricModuleEquivalences().put(slugOrId, new Identifier(group, name));
    }

    public void equivalent(String slugOrId, String group, String name) {
        this.getModuleEquivalences().put(slugOrId, new Identifier(group, name));
    }
}
