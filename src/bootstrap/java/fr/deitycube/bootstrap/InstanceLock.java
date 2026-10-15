package fr.deitycube.bootstrap;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Empêche deux bootstraps de télécharger/installer en même temps dans les mêmes dossiers. */
final class InstanceLock implements AutoCloseable {

    private final FileChannel channel;
    private final FileLock lock;

    private InstanceLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /** @return le verrou, ou {@code null} si un autre bootstrap le détient déjà */
    static InstanceLock tryAcquire(Path file) throws IOException {

        Files.createDirectories(file.getParent());

        FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        FileLock lock;

        try {
            lock = channel.tryLock();
        } catch (OverlappingFileLockException e) {
            lock = null;
        }

        if (lock == null) {
            channel.close();
            return null;
        }

        return new InstanceLock(channel, lock);
    }

    @Override
    public void close() throws IOException {

        try {
            lock.release();
        } finally {
            channel.close();
        }
    }
}
