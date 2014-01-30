package org.gradle.api.internal.tasks.compile.incremental.analyzer;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

/**
 * by Szczepan Faber, created at: 1/21/14
 */
public class ClassDependenciesVisitor extends ClassVisitor {

    private final static int API = Opcodes.ASM4;
    boolean dependentToAll;

    public ClassDependenciesVisitor() {
        super(API);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if(isAnnotationType(interfaces)) {
            dependentToAll = true;
        }
    }

    private boolean isAnnotationType(String[] interfaces) {
        return interfaces.length == 1 && interfaces[0].equals("java/lang/annotation/Annotation");
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (isConstant(access) && !isPrivate(access)) {
            dependentToAll = true; //non-private const
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
