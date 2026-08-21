package fr.deitycube.launcher.progress;

public interface ProgressListener {

    ProgressListener NONE = (phase, detail, current, total) -> {
    };

    void update(
            String phase,
            String detail,
            long current,
            long total
    );

    default void phase(String phase) {
        update(phase, "", 0, -1);
    }

    default void indeterminate(String phase, String detail) {
        update(phase, detail, 0, -1);
    }
}
