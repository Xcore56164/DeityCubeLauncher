package fr.deitycube.launcher.neoforge;

public final class NeoForgeLibrary {

    private final MavenArtifact artifact;
    private final String url;
    private final String sha1;

    public NeoForgeLibrary(
            MavenArtifact artifact,
            String url,
            String sha1
    ) {
        this.artifact = artifact;
        this.url = url;
        this.sha1 = sha1;
    }

    public MavenArtifact getArtifact() {
        return artifact;
    }

    public String getCoordinate() {
        return artifact.toString();
    }

    public String getUrl() {
        return url;
    }

    public String getSha1() {
        return sha1;
    }
}
