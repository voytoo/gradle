package org.gradle.api.internal.tasks.compile.incremental.analyzer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * by Szczepan Faber, created at: 1/16/14
 */
public class ClassDependenciesAnalyzer {

    public ClassAnalysis getClassAnalysis(InputStream input) throws IOException {
        ClassReader reader = new ClassReader(input);
        ClassDependenciesVisitor visitor = new ClassDependenciesVisitor();
        reader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        if (visitor.containsNonPrivateConstant) {
            return new ClassAnalysis(null);
        } else {
            return new ClassAnalysis(getClassDependencies(reader));
        }
    }

    private List<String> getClassDependencies(ClassReader reader) {
        List<String> out = new LinkedList<String>();
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
                String name = type.getClassName();
                if (!name.startsWith("java.lang")) { //let's filter out the sdk
                    out.add(name);
                }
            }
        }
        return out;
    }

    public ClassAnalysis getClassAnalysis(File classFile) throws IOException {
        FileInputStream input = new FileInputStream(classFile);
        try {
            return getClassAnalysis(input);
        } finally {
            input.close();
        }
    }
}
