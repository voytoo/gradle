package org.gradle.api.internal.tasks.compile.incremental;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.FileUtils;
import org.gradle.api.internal.tasks.compile.incremental.analyzer.ClassAnalysis;
import org.gradle.api.internal.tasks.compile.incremental.analyzer.ClassDependenciesAnalyzer;

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
        ClassNameProvider nameProvider = new ClassNameProvider(compiledClassesDir);
        while (output.hasNext()) {
            File classFile = (File) output.next();
            try {
                ClassAnalysis analysis = new ClassDependenciesAnalyzer().getClassAnalysis(classFile);
                String className = nameProvider.provideName(classFile);
                if (analysis.getClassDependencies() != null) {
                    classes.add(className);
                    for (String dependency : analysis.getClassDependencies()) {
                        if (!dependency.equals(className)) {
                            allDependents.put(dependency, className);
                        }
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

    public ClassDependents getActualDependents(String className) {
        Set<String> out = new HashSet<String>();
        Set<String> visited = new HashSet<String>();
        recurseDependents(visited, out, className);
        out.remove(className);
        return new ClassDependents(out);
    }

    private void recurseDependents(Set<String> visited, Collection<String> accumulator, String className) {
        if (!visited.add(className)) {
            return;
        }
        Collection<String> out = dependents.get(className);
        if (out != null && !out.isEmpty()) {
            for (String dependent : out) {
                if (!dependent.contains("$") && !dependent.equals(className)) { //naive
                    accumulator.add(dependent);
                }
                recurseDependents(visited, accumulator, dependent);
            }
        }
    }
}
