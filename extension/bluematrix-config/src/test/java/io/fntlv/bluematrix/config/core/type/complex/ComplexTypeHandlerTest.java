package io.fntlv.bluematrix.config.core.type.complex;

import io.fntlv.bluematrix.config.core.Configs;
import io.fntlv.bluematrix.config.core.type.exception.ConfigTypeHandlerException;
import io.fntlv.bluematrix.config.core.type.exception.ConfigTypeLoadException;
import io.fntlv.bluematrix.config.core.type.exception.ConfigTypeSaveException;
import io.fntlv.bluematrix.config.core.file.ConfigFile;
import io.fntlv.bluematrix.config.core.file.json.JsonConfigFile;
import io.fntlv.bluematrix.config.core.file.yaml.YamlConfigFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplexTypeHandlerTest {

    @TempDir
    File tempDir;

    @AfterEach
    void clearGlobalTypeHandlers() {
        Configs.typeHandlers().clear();
    }

    @Test
    void buildsStringHandlerWithChainedFunctions() {
        ComplexTypeHandler<Port> handler = ComplexTypeHandlers.forType(Port.class)
                .onStringSerialize(value -> Integer.toString(value.value))
                .onStringDeserialize((value, type) -> new Port(Integer.parseInt(value)));
        ConfigFile file = new YamlConfigFile(new File(tempDir, "config.yml"));

        handler.save(file.section("server.port"), new Port(25565));

        assertEquals("25565", file.getString("server.port"));
        assertEquals(25565, handler.load(file.section("server.port"), Port.class).value);
    }

    @Test
    void buildsConfigHandlerWithChainedFunctions() {
        ComplexTypeHandler<Point> handler = ComplexTypeHandlers.forType(Point.class)
                .onConfigSave((section, value) -> {
                    section.set("x", value.x);
                    section.set("y", value.y);
                })
                .onConfigLoad((section, type) -> new Point(section.getInt("x"), section.getInt("y")));
        ConfigFile file = new YamlConfigFile(new File(tempDir, "config.yml"));

        handler.save(file.section("spawn"), new Point(1, 2));

        assertEquals(1, file.getInt("spawn.x"));
        assertEquals(2, file.getInt("spawn.y"));
        assertEquals(2, handler.load(file.section("spawn"), Point.class).y);
    }

    @Test
    void registryFindsAssignableHandler() {
        ComplexTypeHandlerRegistry registry = new ComplexTypeHandlerRegistry();

        registry.register(ComplexTypeHandlers.forType(Number.class)
                .onStringDeserialize((value, type) -> Integer.parseInt(value)));

        assertTrue(registry.find(Integer.class).isPresent());
        assertFalse(registry.find(String.class).isPresent());
    }

    @Test
    void registryContainsDefaultHandlers() {
        ComplexTypeHandlerRegistry registry = new ComplexTypeHandlerRegistry();
        Class<?> linkedHashMapValuesType = new LinkedHashMap<>().values().getClass();

        assertTrue(registry.find(UUID.class).isPresent());
        assertTrue(registry.find(LinkedHashSet.class).isPresent());
        assertTrue(registry.find(linkedHashMapValuesType).isPresent());
    }

    @Test
    void clearRestoresDefaultHandlersAndClearAllRemovesEverything() {
        ComplexTypeHandlerRegistry registry = new ComplexTypeHandlerRegistry();

        registry.clearAll();
        assertFalse(registry.find(UUID.class).isPresent());

        registry.clear();
        assertTrue(registry.find(UUID.class).isPresent());
    }

    @Test
    void missingLoadFunctionFailsFast() {
        ComplexTypeHandler<Port> handler = ComplexTypeHandlers.forType(Port.class);
        ConfigFile file = new YamlConfigFile(new File(tempDir, "config.yml"));

        ConfigTypeHandlerException exception = assertThrows(ConfigTypeHandlerException.class,
                () -> handler.load(file.section("server.port"), Port.class));

        assertTrue(exception.getMessage().contains("Port"));
    }

    @Test
    void missingSaveFunctionFailsFast() {
        ComplexTypeHandler<Port> handler = ComplexTypeHandlers.forType(Port.class);
        ConfigFile file = new YamlConfigFile(new File(tempDir, "config.yml"));

        ConfigTypeHandlerException exception = assertThrows(ConfigTypeHandlerException.class,
                () -> handler.save(file.section("server.port"), new Port(25565)));

        assertTrue(exception.getMessage().contains("Port"));
    }

    @Test
    void missingStringFunctionsFailFast() {
        ComplexTypeHandler<Port> handler = ComplexTypeHandlers.forType(Port.class);

        assertThrows(ConfigTypeHandlerException.class,
                () -> handler.serializeString(new Port(25565)));
        assertThrows(ConfigTypeHandlerException.class,
                () -> handler.deserializeString("25565", Port.class));
    }

    @Test
    void stringListSerializationRequiresSerializeAndDeserializeFunctions() {
        ComplexTypeHandler<Port> serializeOnly = ComplexTypeHandlers.forType(Port.class)
                .onStringSerialize(value -> Integer.toString(value.value));
        ComplexTypeHandler<Port> deserializeOnly = ComplexTypeHandlers.forType(Port.class)
                .onStringDeserialize((value, type) -> new Port(Integer.parseInt(value)));
        ComplexTypeHandler<Port> both = portHandler();

        assertFalse(serializeOnly.canSerializeToStringList());
        assertFalse(deserializeOnly.canSerializeToStringList());
        assertTrue(both.canSerializeToStringList());
    }

    @Test
    void wrapsConfigLoadFailuresWithTypeAndPath() {
        ComplexTypeHandler<Port> handler = ComplexTypeHandlers.forType(Port.class)
                .onConfigLoad((section, type) -> {
                    throw new IllegalArgumentException("broken");
                });
        ConfigFile file = new YamlConfigFile(new File(tempDir, "config.yml"));

        ConfigTypeLoadException exception = assertThrows(ConfigTypeLoadException.class,
                () -> handler.load(file.section("server.port"), Port.class));

        assertTrue(exception.getMessage().contains("Port"));
        assertTrue(exception.getMessage().contains("server.port"));
    }

    @Test
    void wrapsConfigSaveFailuresWithTypeAndPath() {
        ComplexTypeHandler<Port> handler = ComplexTypeHandlers.forType(Port.class)
                .onConfigSave((section, value) -> {
                    throw new IllegalArgumentException("broken");
                });
        ConfigFile file = new YamlConfigFile(new File(tempDir, "config.yml"));

        ConfigTypeSaveException exception = assertThrows(ConfigTypeSaveException.class,
                () -> handler.save(file.section("server.port"), new Port(25565)));

        assertTrue(exception.getMessage().contains("Port"));
        assertTrue(exception.getMessage().contains("server.port"));
    }

    @Test
    void yamlConfigFileUsesRegisteredHandlerForSetAndGetOrSetDefault() {
        Configs.typeHandlers().register(pointHandler());

        assertPointHandlerIsUsed(new YamlConfigFile(new File(tempDir, "config.yml")));
    }

    @Test
    void jsonConfigFileUsesRegisteredHandlerForSetAndGetOrSetDefault() {
        Configs.typeHandlers().register(pointHandler());

        assertPointHandlerIsUsed(new JsonConfigFile(new File(tempDir, "config.json")));
    }

    @Test
    void yamlConfigFileUsesRegisteredHandlerForObjectLists() {
        Configs.typeHandlers().register(pointHandler());

        assertPointListHandlerIsUsed(new YamlConfigFile(new File(tempDir, "config.yml")));
    }

    @Test
    void jsonConfigFileUsesRegisteredHandlerForObjectLists() {
        Configs.typeHandlers().register(pointHandler());

        assertPointListHandlerIsUsed(new JsonConfigFile(new File(tempDir, "config.json")));
    }

    @Test
    void yamlConfigFileStoresStringSerializableObjectListsAsStringLists() {
        Configs.typeHandlers().register(portHandler());

        assertStringListHandlerIsUsed(new YamlConfigFile(new File(tempDir, "config.yml")));
    }

    @Test
    void jsonConfigFileStoresStringSerializableObjectListsAsStringLists() {
        Configs.typeHandlers().register(portHandler());

        assertStringListHandlerIsUsed(new JsonConfigFile(new File(tempDir, "config.json")));
    }

    @Test
    void yamlConfigFileClearsOldObjectListEntriesWhenReplacingWithShorterList() {
        Configs.typeHandlers().register(pointHandler());

        ConfigFile file = new YamlConfigFile(new File(tempDir, "config.yml"));
        assertObjectListReplacementClearsOldEntries(file);
    }

    @Test
    void jsonConfigFileClearsOldObjectListEntriesWhenReplacingWithShorterList() throws Exception {
        Configs.typeHandlers().register(pointHandler());

        File configFile = new File(tempDir, "config.json");
        ConfigFile file = new JsonConfigFile(configFile);
        assertObjectListReplacementClearsOldEntries(file);

        file.save();
        String content = new String(Files.readAllBytes(configFile.toPath()));
        assertFalse(content.contains("\"1\""));
        assertFalse(content.contains("null"));
    }

    @Test
    void yamlConfigFileUsesDefaultUuidHandler() {
        assertDefaultUuidHandlerIsUsed(new YamlConfigFile(new File(tempDir, "config.yml")));
    }

    @Test
    void jsonConfigFileUsesDefaultUuidHandler() {
        assertDefaultUuidHandlerIsUsed(new JsonConfigFile(new File(tempDir, "config.json")));
    }

    @Test
    void yamlConfigFileSavesSetAsListWithDefaultHandler() {
        assertDefaultSetHandlerIsUsed(new YamlConfigFile(new File(tempDir, "config.yml")));
    }

    @Test
    void jsonConfigFileSavesSetAsListWithDefaultHandler() {
        assertDefaultSetHandlerIsUsed(new JsonConfigFile(new File(tempDir, "config.json")));
    }

    @Test
    void yamlConfigFileSavesLinkedHashMapValuesAsListWithDefaultHandler() {
        assertDefaultLinkedHashMapValuesHandlerIsUsed(new YamlConfigFile(new File(tempDir, "config.yml")));
    }

    @Test
    void jsonConfigFileSavesLinkedHashMapValuesAsListWithDefaultHandler() {
        assertDefaultLinkedHashMapValuesHandlerIsUsed(new JsonConfigFile(new File(tempDir, "config.json")));
    }

    @Test
    void yamlConfigFileReadsTypedValuesAndLists() {
        Configs.typeHandlers().register(pointHandler());
        Configs.typeHandlers().register(portHandler());

        assertTypedGetIsUsed(new YamlConfigFile(new File(tempDir, "config.yml")));
    }

    @Test
    void jsonConfigFileReadsTypedValuesAndLists() {
        Configs.typeHandlers().register(pointHandler());
        Configs.typeHandlers().register(portHandler());

        assertTypedGetIsUsed(new JsonConfigFile(new File(tempDir, "config.json")));
    }

    private ComplexTypeHandler<Point> pointHandler() {
        return ComplexTypeHandlers.forType(Point.class)
                .onConfigSave((section, value) -> {
                    section.set("x", value.x);
                    section.set("y", value.y);
                })
                .onConfigLoad((section, type) -> new Point(section.getInt("x"), section.getInt("y")));
    }

    private ComplexTypeHandler<Port> portHandler() {
        return ComplexTypeHandlers.forType(Port.class)
                .onStringSerialize(value -> Integer.toString(value.value))
                .onStringDeserialize((value, type) -> new Port(Integer.parseInt(value)));
    }

    private void assertPointHandlerIsUsed(ConfigFile file) {
        file.set("spawn", new Point(1, 2));

        assertEquals(1, file.getInt("spawn.x"));
        assertEquals(2, file.getInt("spawn.y"));

        Point loaded = file.getOrSetDefault("spawn", new Point(0, 0));
        assertEquals(1, loaded.x);
        assertEquals(2, loaded.y);

        Point missing = file.getOrSetDefault("fallback", new Point(3, 4));
        assertEquals(3, missing.x);
        assertEquals(4, missing.y);
        assertEquals(3, file.getInt("fallback.x"));
        assertEquals(4, file.getInt("fallback.y"));
    }

    private void assertPointListHandlerIsUsed(ConfigFile file) {
        file.set("points", Arrays.asList(new Point(1, 2), new Point(3, 4)));

        assertEquals(1, file.getInt("points.0.x"));
        assertEquals(2, file.getInt("points.0.y"));
        assertEquals(3, file.getInt("points.1.x"));
        assertEquals(4, file.getInt("points.1.y"));

        List<Point> loaded = file.getOrSetDefault("points", Arrays.asList(new Point(0, 0)));
        assertEquals(2, loaded.size());
        assertEquals(1, loaded.get(0).x);
        assertEquals(4, loaded.get(1).y);
    }

    private void assertStringListHandlerIsUsed(ConfigFile file) {
        file.set("ports", Arrays.asList(new Port(25565), new Port(25566)));

        assertEquals(Arrays.asList("25565", "25566"), file.getStringList("ports"));

        List<Port> loaded = file.getOrSetDefault("ports", Arrays.asList(new Port(1)));
        assertEquals(2, loaded.size());
        assertEquals(25565, loaded.get(0).value);
        assertEquals(25566, loaded.get(1).value);
    }

    private void assertObjectListReplacementClearsOldEntries(ConfigFile file) {
        file.set("points", Arrays.asList(new Point(1, 2), new Point(3, 4)));
        file.set("points", Arrays.asList(new Point(5, 6)));

        assertEquals(5, file.getInt("points.0.x"));
        assertEquals(6, file.getInt("points.0.y"));
        assertFalse(file.contains("points.1.x"));
        assertFalse(file.contains("points.1.y"));
    }

    private void assertDefaultUuidHandlerIsUsed(ConfigFile file) {
        UUID uuid = UUID.randomUUID();

        file.set("id", uuid);

        assertEquals(uuid.toString(), file.getString("id"));
        assertEquals(uuid, file.getOrSetDefault("id", UUID.randomUUID()));
    }

    private void assertDefaultSetHandlerIsUsed(ConfigFile file) {
        LinkedHashSet<String> values = new LinkedHashSet<>(Arrays.asList("a", "b"));

        file.set("values", values);

        assertEquals(Arrays.asList("a", "b"), file.getStringList("values"));
    }

    private void assertDefaultLinkedHashMapValuesHandlerIsUsed(ConfigFile file) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("first", "a");
        values.put("second", "b");

        file.set("values", values.values());

        assertEquals(Arrays.asList("a", "b"), file.getStringList("values"));
    }

    private void assertTypedGetIsUsed(ConfigFile file) {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        file.set("spawn", new Point(1, 2));
        file.set("points", Arrays.asList(new Point(3, 4), new Point(5, 6)));
        file.set("ports", Arrays.asList(new Port(25565), new Port(25566)));
        file.set("ids", Arrays.asList(firstId, secondId));
        file.set("values", Arrays.asList("1", "2"));

        Point spawn = file.get("spawn", Point.class);
        assertEquals(1, spawn.x);
        assertEquals(2, spawn.y);

        List<Point> points = file.getList("points", Point.class);
        assertEquals(2, points.size());
        assertEquals(3, points.get(0).x);
        assertEquals(6, points.get(1).y);

        List<Port> ports = file.getList("ports", Port.class);
        assertEquals(2, ports.size());
        assertEquals(25565, ports.get(0).value);
        assertEquals(25566, ports.get(1).value);

        List<UUID> ids = file.getList("ids", UUID.class);
        assertEquals(Arrays.asList(firstId, secondId), ids);

        assertEquals(Arrays.asList(1, 2), file.getList("values", Integer.class));
        assertEquals(null, file.get("missing", Point.class));
        assertTrue(file.getList("missing", Point.class).isEmpty());
    }

    private static class Port {
        private final int value;

        private Port(int value) {
            this.value = value;
        }
    }

    private static class Point {
        private final int x;
        private final int y;

        private Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
