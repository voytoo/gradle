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
        Multimap<String, String> allDependents = LinkedListMultimap.create();
        Set<String> classes = new HashSet<String>();
        Set<String> allDependentClasses = new HashSet<String>();
        ClassNameProvider nameProvider = new ClassNameProvider(compiledClassesDir);
        while (output.hasNext()) {
            File classFile = (File) output.next();
            String className = nameProvider.provideName(classFile);
            if (!className.startsWith(packagePrefix)) {
                continue;
            }
            try {
                ClassAnalysis analysis = new ClassDependenciesAnalyzer().getClassAnalysis(className, classFile);
                classes.add(className);
                for (String dependency : analysis.getClassDependencies()) {
                    if (!dependency.equals(className)) {
                        allDependents.put(dependency, className);
                    }
                }
                if (analysis.isDependentToAll()) {
                    allDependentClasses.add(className);
                }
            } catch (IOException e) {
                throw new RuntimeException("Problems extracting class dependency from " + classFile, e);
            }
        }
        //go through all dependents and use only internal class dependencies (e.g. exclude dependencies like java.util.List, etc)
        for (String c : classes) {
            ClassDependents d = allDependentClasses.contains(c)? new ClassDependents(null) : new ClassDependents(allDependents.get(c));
            this.dependents.put(c, d);
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
