package fr.deitycube.launcher.minecraft.library;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MinecraftLibrary {

    private String name;
    private Downloads downloads;
    private List<Rule> rules;

    public String getName() {
        return name;
    }

    public Downloads getDownloads() {
        return downloads;
    }

    public List<Rule> getRules() {
        return rules;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Downloads {

        private Artifact artifact;
        private Classifiers classifiers;

        public Artifact getArtifact() {
            return artifact;
        }

        public Classifiers getClassifiers() {
            return classifiers;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Artifact {

        private String path;
        private String sha1;
        private long size;
        private String url;

        public String getPath() {
            return path;
        }

        public String getSha1() {
            return sha1;
        }

        public long getSize() {
            return size;
        }

        public String getUrl() {
            return url;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rule {

        private String action;
        private OS os;

        public String getAction() {
            return action;
        }

        public OS getOs() {
            return os;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OS {

        private String name;

        public String getName() {
            return name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Classifiers {

        @JsonProperty("natives-windows")
        private Artifact nativesWindows;

        public Artifact getNativesWindows() {
            return nativesWindows;
        }
    }
}
