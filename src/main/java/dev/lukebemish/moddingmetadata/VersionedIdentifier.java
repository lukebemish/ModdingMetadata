package dev.lukebemish.moddingmetadata;

public record VersionedIdentifier(Identifier identifier, String version) {
    public String group() {
        return identifier.group();
    }

    public String name() {
        return identifier.name();
    }
}
