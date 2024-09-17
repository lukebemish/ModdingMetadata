package dev.lukebemish.moddingmetadata;

import java.io.Serializable;

public record Identifier(String group, String name) implements Serializable {
}
