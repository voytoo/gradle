package org.gradle.api.internal.tasks.compile.incremental;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;

/**
 * by Szczepan Faber, created at: 1/16/14
 */
public class ClassDependenciesAnalyzer {

    public Collection<String> getClassesUsedBy(InputStream input) throws IOException {
        ClassReader reader = new ClassReader(input);
        Collection<String> out = new LinkedList<String>();
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

    public Collection<String> getClassesUsedBy(File classFile) throws IOException {
        FileInputStream input = new FileInputStream(classFile);
        try {
            return getClassesUsedBy(input);
        } finally {
            input.close();
        }
    }
}
