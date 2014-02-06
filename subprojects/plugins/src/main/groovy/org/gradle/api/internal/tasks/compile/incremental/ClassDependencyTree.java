package org.gradle.api.internal.tasks.compile.incremental;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.gradle.api.internal.tasks.compile.incremental.analyzer.ClassAnalysis;
import org.gradle.api.internal.tasks.compile.incremental.analyzer.ClassDependenciesAnalyzer;

import java.io.*;
import java.util.*;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * by Szczepan Faber, created at: 1/15/14
 */
public class ClassDependencyTree implements Serializable {

    private final Map<String, ClassDependents> dependents = new HashMap<String, ClassDependents>();

    public ClassDependencyTree(File compiledClassesDir) {
        this(compiledClassesDir, "");
    }

    ClassDependencyTree(File compiledClassesDir, String packagePrefix) {
        Iterator output = FileUtils.iterateFiles(compiledClassesDir, new String[]{"class"}, true);
        ClassNameProvider nameProvider = new ClassNameProvider(compiledClassesDir);
        while (output.hasNext()) {
            File classFile = (File) output.next();
            String className = nameProvider.provideName(classFile);
            if (!className.startsWith(packagePrefix)) {
                continue;
            }
            try {
                ClassAnalysis analysis = new ClassDependenciesAnalyzer().getClassAnalysis(className, classFile);
                for (String dependency : analysis.getClassDependencies()) {
                    if (!dependency.equals(className) && dependency.startsWith(packagePrefix)) {
                        getOrCreateDependentMapping(dependency).addClass(className);
                    }
                }
                if (analysis.isDependentToAll()) {
                    getOrCreateDependentMapping(className).setDependentToAll();
                }
            } catch (IOException e) {
                throw new RuntimeException("Problems extracting class dependency from " + classFile, e);
            }
        }
    }

    private ClassDependents getOrCreateDependentMapping(String dependency) {
        ClassDependents d = dependents.get(dependency);
        if (d == null) {
            d = new ClassDependents();
            dependents.put(dependency, d);
        }
        return d;
    }

    public void writeTo(File outputFile) {
        ClassDependencyTree target = this;
        DummySerializer.writeTargetTo(outputFile, target);
    }

    public static ClassDependencyTree loadFrom(File inputFile) {
        return (ClassDependencyTree) DummySerializer.readFrom(inputFile);
    }

    public Set<String> getActualDependents(String className) {
        Set<String> out = new HashSet<String>();
        Set<String> visited = new HashSet<String>();
        MutableBoolean isDependentToAll = new MutableBoolean(false);
        recurseDependents(visited, out, className, isDependentToAll);
        if (isDependentToAll.isTrue()) {
            return null;
        }
        out.remove(className);
        return out;
    }

    private void recurseDependents(Set<String> visited, Collection<String> accumulator, String className, MutableBoolean dependentToAll) {
        if (!visited.add(className)) {
            return;
        }
        ClassDependents out = dependents.get(className);
        if (out == null) {
            return;
        }
        if (out.isDependentToAll()) {
            dependentToAll.setValue(true);
            return;
        }
        for (String dependent : out.getDependentClasses()) {
            if (!dependent.contains("$") && !dependent.equals(className)) { //naive
                accumulator.add(dependent);
            }
            recurseDependents(visited, accumulator, dependent, dependentToAll);
        }
    }
}
