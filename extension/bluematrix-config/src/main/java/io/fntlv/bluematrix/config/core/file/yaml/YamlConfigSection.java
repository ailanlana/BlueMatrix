package io.fntlv.bluematrix.config.core.file.yaml;

import io.fntlv.bluematrix.config.core.section.DefaultConfigSection;
import org.simpleyaml.configuration.comments.CommentType;

import java.util.Set;

public class YamlConfigSection extends DefaultConfigSection {

    private final YamlConfigFile document;

    public YamlConfigSection(YamlConfigFile document, String path) {
        super(document, path);
        this.document = document;
    }

    // Yaml

    public YamlConfigSection getSection(String path) {
        return document.getSection(resolve(path));
    }

    public Set<String> getKeys() {
        return document.getKeys(path);
    }

    public Set<String> getKeys(String path) {
        return document.getKeys(resolve(path));
    }

    public Set<String> getKeys(String path, boolean deep) {
        return document.getKeys(resolve(path), deep);
    }

    public Set<YamlConfigSection> getSections() {
        return document.getSections(path);
    }

    public Set<YamlConfigSection> getSections(String path) {
        return document.getSections(resolve(path));
    }

    public Set<YamlConfigSection> getSections(String path, boolean deep) {
        return document.getSections(resolve(path), deep);
    }

    public String getComment(String path) {
        return document.getComment(resolve(path));
    }

    public String getComment(String path, CommentType type) {
        return document.getComment(resolve(path), type);
    }

    public void setComment(String path, String comment, CommentType type) {
        document.setComment(resolve(path), comment, type);
    }

    public boolean isSection(String path) {
        return document.isSection(resolve(path));
    }
}
