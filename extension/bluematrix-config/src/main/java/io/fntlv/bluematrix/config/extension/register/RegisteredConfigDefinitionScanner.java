package io.fntlv.bluematrix.config.extension.register;

import io.fntlv.bluematrix.config.extension.annotation.BlueConfig;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

final class RegisteredConfigDefinitionScanner {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(RegisteredConfigDefinitionScanner.class);

    List<RegisteredConfigField> scan(Class<?> configClass, String category) {
        List<Field> fields = Arrays.stream(configClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(BlueConfig.Field.class))
                .collect(Collectors.toList());

        if (fields.isEmpty()) {
            LOGGER.warn("Configuration class {} contains no @Field annotated fields",
                    configClass.getName());
            return new ArrayList<>();
        }

        LOGGER.debug("Found {} configuration fields in class {}",
                fields.size(), configClass.getSimpleName());

        List<RegisteredConfigField> registeredFields = new ArrayList<>();
        for (Field field : fields) {
            registeredFields.add(createRegisteredField(configClass, category, field));
        }
        return registeredFields;
    }

    private RegisteredConfigField createRegisteredField(Class<?> configClass,
                                                       String category,
                                                       Field field) {
        try {
            if (Modifier.isStatic(field.getModifiers())) {
                throw new ConfigDefinitionException("Configuration field must not be static");
            }
            if (Modifier.isFinal(field.getModifiers())) {
                throw new ConfigDefinitionException("Configuration field must not be final");
            }

            BlueConfig.Field fieldAnnotation = field.getAnnotation(BlueConfig.Field.class);
            String configPath = category + "." + fieldAnnotation.path();
            Class<?> listElementType = List.class.isAssignableFrom(field.getType())
                    ? resolveListElementType(field, configPath)
                    : null;

            field.setAccessible(true);
            return new RegisteredConfigField(
                    field,
                    configPath,
                    createDefaultValue(fieldAnnotation, field),
                    fieldAnnotation.comment(),
                    listElementType
            );
        } catch (ConfigInjectionException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Configuration injection failed: %s.%s (%s)",
                    configClass.getSimpleName(),
                    field.getName(),
                    e.getMessage());

            throw new ConfigInjectionException(errorMsg, e);
        }
    }

    private Object createDefaultValue(BlueConfig.Field fieldAnnotation, Field field) {
        String[] defaultValues = fieldAnnotation.defaultValue();
        if (List.class.isAssignableFrom(field.getType())) {
            return Arrays.asList(defaultValues);
        }
        return defaultValues.length == 0 ? null : defaultValues[0];
    }

    private Class<?> resolveListElementType(Field field, String configPath) {
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            throw new ConfigDefinitionException("List field has no generic type at path " + configPath);
        }

        Type actualType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        if (!(actualType instanceof Class<?>)) {
            throw new ConfigDefinitionException("Unsupported generic list type at path " + configPath + ": " + actualType);
        }
        return (Class<?>) actualType;
    }
}
