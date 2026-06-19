package io.fntlv.bluematrix.loader.library;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public final class BlueLibrary {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String checksum;
    private final List<Relocation> relocations;

    BlueLibrary(String groupId, String artifactId, String version, String checksum) {
        this(groupId, artifactId, version, checksum, Collections.emptyList());
    }

    private BlueLibrary(String groupId, String artifactId, String version, String checksum, List<Relocation> relocations) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.checksum = checksum;
        this.relocations = Collections.unmodifiableList(new ArrayList<>(relocations));
    }

    public boolean hasChecksum() {
        return checksum != null && !checksum.trim().isEmpty();
    }

    public boolean hasRelocations() {
        return !relocations.isEmpty();
    }

    public BlueLibrary relocate(String pattern, String relocatedPattern) {
        Relocation relocation = new Relocation(pattern, relocatedPattern);
        List<Relocation> nextRelocations = new ArrayList<>(relocations);
        nextRelocations.add(relocation);
        return new BlueLibrary(groupId, artifactId, version, checksum, nextRelocations);
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }

    @Getter
    public static final class Relocation {
        private final String pattern;
        private final String relocatedPattern;

        private Relocation(String pattern, String relocatedPattern) {
            if (pattern == null || pattern.trim().isEmpty()) {
                throw new IllegalArgumentException("pattern cannot be blank");
            }
            if (relocatedPattern == null || relocatedPattern.trim().isEmpty()) {
                throw new IllegalArgumentException("relocatedPattern cannot be blank");
            }
            this.pattern = pattern.trim();
            this.relocatedPattern = relocatedPattern.trim();
        }
    }
}
