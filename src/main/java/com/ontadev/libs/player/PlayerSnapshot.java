package com.ontadev.libs.player;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.UUID;


@Accessors(fluent = true)
@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode(of = "uuid")
public final class PlayerSnapshot {
    private final UUID uuid;
    private final String name;

    @Override
    public String toString() {
        return String.format("{\"uuid\": \"%s\", \"name\": \"%s\"}", uuid, name);
    }
}