package fr.deitycube.launcher.neoforge;

public final class MavenArtifact {

    private final String group;
    private final String artifact;
    private final String version;
    private final String classifier;
    private final String extension;

    public MavenArtifact(
            String group,
            String artifact,
            String version
    ) {
        this(
                group,
                artifact,
                version,
                null,
                "jar"
        );
    }

    public MavenArtifact(
            String group,
            String artifact,
            String version,
            String classifier
    ) {
        this(
                group,
                artifact,
                version,
                classifier,
                "jar"
        );
    }

    public MavenArtifact(
            String group,
            String artifact,
            String version,
            String classifier,
            String extension
    ) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.classifier = classifier;
        this.extension = extension;
    }

    public String getGroup() {
        return group;
    }

    public String getArtifact() {
        return artifact;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getExtension() {
        return extension;
    }

    public String getFileName() {

        StringBuilder fileName =
                new StringBuilder();

        fileName.append(artifact);
        fileName.append("-");
        fileName.append(version);

        if (classifier != null
                && !classifier.isBlank()) {

            fileName.append("-");
            fileName.append(classifier);
        }

        fileName.append(".");
        fileName.append(extension);

        return fileName.toString();
    }

    public String getRelativePath() {

        return group.replace('.', '/')
                + "/"
                + artifact
                + "/"
                + version
                + "/"
                + getFileName();
    }

    public static MavenArtifact parse(String coordinate) {

        if (coordinate == null || coordinate.isBlank()) {
            throw new IllegalArgumentException(
                    "Coordonnée Maven vide."
            );
        }

        String value = coordinate;

        if (value.startsWith("[")
                && value.endsWith("]")) {

            value = value.substring(
                    1,
                    value.length() - 1
            );
        }

        String extension = "jar";

        int atIndex = value.lastIndexOf('@');

        if (atIndex >= 0) {

            extension =
                    value.substring(
                            atIndex + 1
                    );

            value =
                    value.substring(
                            0,
                            atIndex
                    );
        }

        String[] parts = value.split(":");

        if (parts.length != 3
                && parts.length != 4) {

            throw new IllegalArgumentException(
                    "Coordonnée Maven invalide : "
                            + coordinate
            );
        }

        String group = parts[0];
        String artifact = parts[1];
        String version = parts[2];

        String classifier =
                parts.length == 4
                        ? parts[3]
                        : null;

        return new MavenArtifact(
                group,
                artifact,
                version,
                classifier,
                extension
        );
    }

    @Override
    public String toString() {

        StringBuilder result =
                new StringBuilder();

        result.append(group);
        result.append(":");
        result.append(artifact);
        result.append(":");
        result.append(version);

        if (classifier != null
                && !classifier.isBlank()) {

            result.append(":");
            result.append(classifier);
        }

        if (!"jar".equals(extension)) {
            result.append("@");
            result.append(extension);
        }

        return result.toString();
    }
}
