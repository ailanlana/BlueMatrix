package io.fntlv.bluematrix.core.module.registration.provider;

import io.fntlv.bluematrix.core.module.ModuleInfo;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

final class JarModuleMetadataReader {
    Optional<JarModuleMetadata> read(InputStream input) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }
        try (DataInputStream dataInput = new DataInputStream(input)) {
            ClassFile classFile = new ClassFile(dataInput);
            AnnotationsAttribute annotations = (AnnotationsAttribute) classFile.getAttribute(AnnotationsAttribute.visibleTag);
            if (annotations == null) {
                return Optional.empty();
            }
            Annotation moduleInfo = annotations.getAnnotation(ModuleInfo.class.getName());
            if (moduleInfo == null) {
                return Optional.empty();
            }
            String id = stringValue(moduleInfo, "id");
            String name = stringValue(moduleInfo, "name");
            return Optional.of(new JarModuleMetadata(
                    classFile.getName(),
                    id,
                    name,
                    stringArrayValue(moduleInfo, "repositories"),
                    stringArrayValue(moduleInfo, "libraries")
            ));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read class metadata", e);
        }
    }

    private String stringValue(Annotation annotation, String memberName) {
        MemberValue value = annotation.getMemberValue(memberName);
        if (!(value instanceof StringMemberValue)) {
            throw new IllegalArgumentException("@ModuleInfo." + memberName + " is missing or not a string");
        }
        return ((StringMemberValue) value).getValue();
    }

    private String[] stringArrayValue(Annotation annotation, String memberName) {
        MemberValue value = annotation.getMemberValue(memberName);
        if (value == null) {
            return new String[0];
        }
        if (value instanceof StringMemberValue) {
            return new String[]{((StringMemberValue) value).getValue()};
        }
        if (!(value instanceof ArrayMemberValue)) {
            throw new IllegalArgumentException("@ModuleInfo." + memberName + " is not a string array");
        }
        MemberValue[] values = ((ArrayMemberValue) value).getValue();
        if (values == null || values.length == 0) {
            return new String[0];
        }
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if (!(values[i] instanceof StringMemberValue)) {
                throw new IllegalArgumentException("@ModuleInfo." + memberName + " contains a non-string value");
            }
            result[i] = ((StringMemberValue) values[i]).getValue();
        }
        return result;
    }
}
