package io.fntlv.bluematrix.config.extension.register;

import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.extension.annotation.BlueConfig;
import io.fntlv.bluematrix.logging.BlueLogger;
import io.fntlv.bluematrix.logging.BlueLoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

class RegisteredConfigBinder {
    private static final BlueLogger LOGGER = BlueLoggerFactory.getLogger(RegisteredConfigBinder.class);

    RegisteredConfig create(Object configInstance,
                            Class<?> configClass,
                            ConfigFile file,
                            String category) {
        List<Field> fields = Arrays.stream(configClass.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(BlueConfig.Field.class))
                .collect(Collectors.toList());

        if (fields.isEmpty()) {
            logger().warn("Configuration class {} contains no @Field annotated fields",
                    configClass.getName());
            return new RegisteredConfig(configClass, configInstance, file, new ArrayList<>());
        }

        logger().debug("Found {} configuration fields in class {}",
                fields.size(), configClass.getSimpleName());

        List<RegisteredConfigField> registeredFields = new ArrayList<>();
        for (Field field : fields) {
            registeredFields.add(createRegisteredField(configClass, category, field));
        }
        return new RegisteredConfig(configClass, configInstance, file, registeredFields);
    }

    void load(RegisteredConfig config) {
        for (RegisteredConfigField field : config.fields()) {
            loadConfigField(config.file(), config, field);
        }
    }

    void save(RegisteredConfig config) {
        for (RegisteredConfigField field : config.fields()) {
            saveConfigField(config.file(), config, field);
        }
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

    private void loadConfigField(ConfigFile file, RegisteredConfig config, RegisteredConfigField field) {
        try {
            boolean existed = file.contains(field.path());
            file.setDefault(field.path(), field.defaultValue(), field.comment());
            Object finalValue = readFieldValue(file, field);
            field.field().set(config.instance(), finalValue);
            if (!existed) {
                file.set(field.path(), toStoredValue(finalValue));
            }

            logger().debug("Injected configuration [{}] => {}.{} (Type: {})",
                    field.path(),
                    config.type().getSimpleName(),
                    field.field().getName(),
                    field.field().getType().getSimpleName());
        } catch (ConfigInjectionException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Configuration injection failed: %s.%s (%s)",
                    config.type().getSimpleName(),
                    field.field().getName(),
                    e.getMessage());

            throw new ConfigInjectionException(errorMsg, e);
        }
    }

    private void saveConfigField(ConfigFile file, RegisteredConfig config, RegisteredConfigField field) {
        try {
            file.set(field.path(), toStoredValue(field.field().get(config.instance())));
        } catch (ConfigInjectionException e) {
            throw e;
        } catch (Exception e) {
            String errorMsg = String.format("Configuration save failed: %s.%s (%s)",
                    config.type().getSimpleName(),
                    field.field().getName(),
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

    private Object readFieldValue(ConfigFile file, RegisteredConfigField field) {
        if (field.list()) {
            return file.getList(field.path(), field.listElementType());
        }
        return file.get(field.path(), field.field().getType());
    }

    private Object toStoredValue(Object value) {
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof Collection<?>) {
            List<Object> stored = new ArrayList<>();
            boolean containsEnum = false;
            for (Object item : (Collection<?>) value) {
                if (item instanceof Enum<?>) {
                    containsEnum = true;
                    stored.add(((Enum<?>) item).name());
                } else {
                    stored.add(item);
                }
            }
            return containsEnum ? stored : value;
        }
        return value;
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

    private BlueLogger logger() {
        return LOGGER;
    }
}
