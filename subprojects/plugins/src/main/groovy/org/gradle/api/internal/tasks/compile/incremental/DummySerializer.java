package org.gradle.api.internal.tasks.compile.incremental;

import java.io.*;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * by Szczepan Faber, created at: 1/30/14
 */
public class DummySerializer {
    public static void writeTargetTo(File outputFile, Object target) {
        try {
            FileOutputStream out = new FileOutputStream(outputFile);
            ObjectOutputStream objectStr = new ObjectOutputStream(out);
            objectStr.writeObject(target);
            objectStr.flush();
            objectStr.close();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Problems writing to the output file " + outputFile, e);
        }
    }

    public static Object readFrom(File inputFile) {
        FileInputStream in = null;
        ObjectInputStream objectStr = null;
        try {
            in = new FileInputStream(inputFile);
            objectStr = new ObjectInputStream(in);
            return objectStr.readObject();
        } catch (Exception e) {
            throw new RuntimeException("Problems reading the class tree to the output file " + inputFile, e);
        } finally {
            closeQuietly(in);
            closeQuietly(objectStr);
        }
    }
}
