package fr.deitycube.launcher.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class LinuxCodecAdvisor {

    private static final Path OS_RELEASE_FILE = Path.of("/etc/os-release");

    private LinuxCodecAdvisor() {
    }

    static String suggestedInstallCommand() {

        String osRelease = readOsRelease();

        String id = extractField(osRelease, "ID");
        String idLike = extractField(osRelease, "ID_LIKE");

        String combined = (id + " " + idLike).toLowerCase();

        if (combined.contains("fedora") || combined.contains("rhel") || combined.contains("centos")) {

            return "sudo dnf install -y "
                    + "https://mirrors.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm "
                    + "https://mirrors.rpmfusion.org/nonfree/fedora/rpmfusion-nonfree-release-$(rpm -E %fedora).noarch.rpm\n"
                    + "sudo dnf install -y gstreamer1-plugins-good gstreamer1-plugins-bad-freeworld gstreamer1-libav";
        }

        if (combined.contains("suse")) {

            return "sudo zypper install -y gstreamer-plugins-good gstreamer-plugins-bad "
                    + "gstreamer-plugins-ugly gstreamer-plugins-libav";
        }

        return "sudo apt install -y gstreamer1.0-plugins-good gstreamer1.0-plugins-bad gstreamer1.0-libav";
    }

    private static String readOsRelease() {

        try {
            return Files.readString(OS_RELEASE_FILE);
        } catch (IOException e) {
            return "";
        }
    }

    private static String extractField(String osRelease, String key) {

        for (String line : osRelease.split("\n")) {

            if (line.startsWith(key + "=")) {

                return line.substring(key.length() + 1)
                        .replace("\"", "")
                        .trim();
            }
        }

        return "";
    }
}
