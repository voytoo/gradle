package org.gradle.api.internal.tasks.compile.incremental;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.*;
import java.util.*;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * by Szczepan Faber, created at: 1/15/14
 */
public class ClassDependencyTree implements Serializable {

    private final Map<String, Collection<String>> dependents = new HashMap<String, Collection<String>>();

    public ClassDependencyTree(File compiledClassesDir) {
        Iterator output = FileUtils.iterateFiles(compiledClassesDir, new String[]{"class"}, true);
        Multimap<String, String> allDependents = LinkedListMultimap.create();
        Set<String> classes = new HashSet<String>();
        while (output.hasNext()) {
            File classFile = (File) output.next();
            try {
                Set<String> classesUsed = getClassesUsedBy(classFile);
                String className = classFile.getName().replaceAll(".class", "");
                classes.add(className);
                for (String dependency : classesUsed) {
                    if (!dependency.equals(className)) {
                        allDependents.put(dependency, className);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Problems extracting class dependency from " + classFile, e);
            }
        }
        //go through all dependents and use only internal class dependencies (e.g. exclude dependencies like java.util.List, etc)
        for (String c : classes) {
            this.dependents.put(c, new LinkedList(allDependents.get(c)));
        }
    }

    private Set<String> getClassesUsedBy(File classFile) throws IOException {
        ClassReader reader;
        FileInputStream input = new FileInputStream(classFile);
        try {
            reader = new ClassReader(input);
        } finally {
            input.close();
        }

        Set<String> out = new HashSet<String>();
        char[] charBuffer = new char[reader.getMaxStringLength()];
        for (int i = 1; i < reader.getItemCount(); i++) {
            int itemOffset = reader.getItem(i);
            if (itemOffset > 0 && reader.readByte(itemOffset - 1) == 7) {
                // A CONSTANT_Class entry, read the class descriptor
                String classDescriptor = reader.readUTF8(itemOffset, charBuffer);
                Type type = Type.getObjectType(classDescriptor);
                while (type.getSort() == Type.ARRAY) {
                    type = type.getElementType();
                }
                if (type.getSort() != Type.OBJECT) {
                    // A primitive type
                    continue;
                }
                out.add(type.getClassName());
            }
        }
        return out;
    }

    public void writeTo(File outputFile) {
        try {
            FileOutputStream out = new FileOutputStream(outputFile);
            ObjectOutputStream objectStr = new ObjectOutputStream(out);
            objectStr.writeObject(this);
            objectStr.flush();
            objectStr.close();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Problems writing the class tree to the output file " + outputFile, e);
        }
    }

    public static ClassDependencyTree loadFrom(File inputFile) {
        FileInputStream in = null;
        ObjectInputStream objectStr = null;
        try {
            in = new FileInputStream(inputFile);
            objectStr = new ObjectInputStream(in);
            return (ClassDependencyTree) objectStr.readObject();
        } catch (Exception e) {
            throw new RuntimeException("Problems reading the class tree to the output file " + inputFile, e);
        } finally {
            closeQuietly(in);
            closeQuietly(objectStr);
        }
    }

    public Iterable<String> getDependents(String className) {
        List<String> out = new LinkedList<String>();
        recurseDependents(out, className);
        return out;
    }

    private void recurseDependents(List<String> accumulator, String className) {
        Collection<String> out = dependents.get(className);
        if (out != null && !out.isEmpty()) {
            accumulator.addAll(out);
            for (String dependent : out) {
                recurseDependents(accumulator, dependent);
            }
        }
    }
}
