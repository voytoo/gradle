package org.gradle.api.internal.tasks.compile.incremental;

import java.io.File;
import java.util.List;

/**
 * by Szczepan Faber, created at: 1/30/14
 */
public class JarDeltaProvider {

    private List<String> changedSource;

    public JarDeltaProvider(File jarFile) {
        File classDelta = new File(jarFile + "-class-delta.bin");
        if (classDelta.isFile()) {
            changedSource = (List<String>) DummySerializer.readFrom(classDelta);
        }
    }

    public boolean isRebuildNeeded() {
        return changedSource == null;
    }

    public Iterable<String> getChangedClasses() {
        return changedSource;
    }
}
