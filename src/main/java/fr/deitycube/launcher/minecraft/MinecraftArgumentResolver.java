package fr.deitycube.launcher.minecraft;

import java.util.ArrayList;
import java.util.List;

public class MinecraftArgumentResolver {

    public List<String> resolve(
            List<Object> arguments
    ) {

        List<String> resolved =
                new ArrayList<>();

        if (arguments == null) {
            return resolved;
        }

        for (Object argument : arguments) {

            if (argument instanceof String value) {

                resolved.add(value);
                continue;
            }

            if (!(argument instanceof MinecraftArgument
                    minecraftArgument)) {

                continue;
            }

            if (!isAllowed(minecraftArgument)) {
                continue;
            }

            Object value =
                    minecraftArgument.getValue();

            if (value instanceof String string) {

                resolved.add(string);

            } else if (value instanceof List<?> values) {

                for (Object item : values) {

                    if (item instanceof String string) {
                        resolved.add(string);
                    }
                }
            }
        }

        return resolved;
    }

    private boolean isAllowed(
            MinecraftArgument argument
    ) {

        List<MinecraftArgumentRule> rules =
                argument.getRules();

        if (rules == null || rules.isEmpty()) {
            return true;
        }

        for (MinecraftArgumentRule rule : rules) {

            if (!matchesOs(rule)) {
                continue;
            }

            String action =
                    rule.getAction();

            if ("allow".equals(action)) {
                return true;
            }

            if ("disallow".equals(action)) {
                return false;
            }
        }

        return false;
    }

    private boolean matchesOs(
            MinecraftArgumentRule rule
    ) {

        MinecraftArgumentOs os =
                rule.getOs();

        if (os == null) {
            return true;
        }

        String currentOs =
                getCurrentOs();

        String requiredOs =
                os.getName();

        if (requiredOs != null
                && !requiredOs.equalsIgnoreCase(
                currentOs
        )) {
            return false;
        }

        String requiredArch =
                os.getArch();

        if (requiredArch != null
                && !requiredArch.equalsIgnoreCase(
                System.getProperty("os.arch")
        )) {
            return false;
        }

        return true;
    }

    private String getCurrentOs() {

        String os =
                System.getProperty("os.name")
                        .toLowerCase();

        if (os.contains("win")) {
            return "windows";
        }

        if (os.contains("mac")) {
            return "osx";
        }

        if (os.contains("linux")) {
            return "linux";
        }

        return os;
    }
}
