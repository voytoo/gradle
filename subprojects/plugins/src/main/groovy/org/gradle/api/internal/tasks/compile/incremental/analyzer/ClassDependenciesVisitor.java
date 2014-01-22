package org.gradle.api.internal.tasks.compile.incremental.analyzer;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

/**
 * by Szczepan Faber, created at: 1/21/14
 */
public class ClassDependenciesVisitor extends ClassVisitor {

    boolean containsNonPrivateConstant;

    public ClassDependenciesVisitor() {
        super(Opcodes.ASM4);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (isConstant(access) && !isPrivate(access)) {
            containsNonPrivateConstant = true; //non-private const
        }
        return null;
    }

    private static boolean isPrivate(int access) {
        return (access & Opcodes.ACC_PRIVATE) != 0;
    }

    private static boolean isConstant(int access) {
        return ((access & Opcodes.ACC_FINAL) != 0 && (access & Opcodes.ACC_STATIC) != 0);
    }
}
