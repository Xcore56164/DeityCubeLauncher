package fr.deitycube.launcher.minecraft.library;

import fr.deitycube.launcher.minecraft.platform.OperatingSystem;

public final class MinecraftLibraryRuleEvaluator {

    private static final String CURRENT_OS =
            OperatingSystem.current().minecraftName();

    private MinecraftLibraryRuleEvaluator() {
    }

    public static boolean isAllowed(MinecraftLibrary library) {

        if (library.getRules() == null
                || library.getRules().isEmpty()) {

            return true;
        }

        boolean allowed = false;

        for (MinecraftLibrary.Rule rule : library.getRules()) {

            if (rule.getOs() == null
                    || rule.getOs().getName() == null) {

                allowed = "allow".equalsIgnoreCase(
                        rule.getAction()
                );

                continue;
            }

            if (!CURRENT_OS.equalsIgnoreCase(
                    rule.getOs().getName()
            )) {
                continue;
            }

            allowed = "allow".equalsIgnoreCase(
                    rule.getAction()
            );
        }

        return allowed;
    }
}
