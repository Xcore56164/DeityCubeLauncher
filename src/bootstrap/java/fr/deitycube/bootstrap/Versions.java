package fr.deitycube.bootstrap;

import java.util.regex.Pattern;

/**
 * Les versions deviennent des noms de dossier : on n'accepte que des suites de nombres séparés
 * par des points, ce qui exclut tout ".." ou séparateur de chemin venant d'un manifeste forgé.
 */
final class Versions {

    private static final Pattern VALID = Pattern.compile("\\d{1,6}(\\.\\d{1,6}){1,3}");

    private Versions() {
    }

    static boolean isValid(String version) {
        return version != null && VALID.matcher(version).matches();
    }

    static boolean isNewer(String candidate, String current) {
        return compare(candidate, current) > 0;
    }

    /** Les deux versions doivent avoir été validées par {@link #isValid(String)}. */
    static int compare(String a, String b) {

        String[] left = a.split("\\.");
        String[] right = b.split("\\.");

        int length = Math.max(left.length, right.length);

        for (int i = 0; i < length; i++) {

            int x = i < left.length ? Integer.parseInt(left[i]) : 0;
            int y = i < right.length ? Integer.parseInt(right[i]) : 0;

            if (x != y) {
                return Integer.compare(x, y);
            }
        }

        return 0;
    }
}
