package org.gradle.api.internal.tasks.compile.incremental.analyzer;

import org.objectweb.asm.*;

import java.util.HashSet;
import java.util.Set;

/**
 * by Szczepan Faber, created at: 1/21/14
 */
public class ClassDependenciesVisitor extends ClassVisitor {

    boolean dependentToAll;
    Set<String> annotations = new HashSet<String>();
    private ClassRelevancyFilter filter;

    public ClassDependenciesVisitor(ClassRelevancyFilter filter) {
        super(Opcodes.ASM4);
        this.filter = filter;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        String annotationClassName = Type.getType(desc).getClassName();
        if (filter.isRelevant(annotationClassName)) {
            annotations.add(annotationClassName);
        }
        return null;
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
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr);    //To change body of overridden methods use File | Settings | File Templates.
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
