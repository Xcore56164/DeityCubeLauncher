package fr.deitycube.bootstrap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

final class SwingUi implements BootstrapUi {

    private static final String TITLE = "DeityCube Launcher";

    private final Image icon;

    SwingUi() {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Look & feel par défaut : moins joli, tout aussi fonctionnel.
        }

        URL resource = SwingUi.class.getResource("/logo.png");
        icon = resource != null ? new ImageIcon(resource).getImage() : null;
    }

    @Override
    public boolean confirmUpdate(String version, String notes, boolean firstInstall) {

        String intro = firstInstall
                ? "Le launcher DeityCube (version " + version + ") va être téléchargé puis installé dans votre "
                + "profil.\n\nVotre antivirus peut analyser les fichiers pendant cette étape : si une alerte "
                + "apparaît, autorisez-les."
                : "La version " + version + " du launcher est disponible.\n\nElle est vérifiée (signature) puis "
                + "installée à côté de la version actuelle, qui n'est supprimée qu'ensuite.\nVotre antivirus peut "
                + "analyser les nouveaux fichiers pendant cette étape : si une alerte apparaît, autorisez-les.";

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextArea introArea = text(intro, firstInstall ? 6 : 8, 44);
        introArea.setOpaque(false);
        introArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(introArea);

        if (!notes.isBlank()) {

            panel.add(Box.createVerticalStrut(10));

            JLabel notesTitle = new JLabel("Nouveautés :");
            notesTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(notesTitle);

            JScrollPane notesPane = new JScrollPane(text(notes, 6, 44));
            notesPane.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(notesPane);
        }

        String[] options = firstInstall
                ? new String[]{"Installer", "Quitter"}
                : new String[]{"Mettre à jour", "Plus tard"};

        Integer choice = onEdt(() -> JOptionPane.showOptionDialog(
                null,
                panel,
                firstInstall ? TITLE + " — Installation" : TITLE + " — Mise à jour",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        ));

        return Integer.valueOf(0).equals(choice);
    }

    @Override
    public Progress progress(String title) {

        AtomicReference<JDialog> dialog = new AtomicReference<>();
        AtomicReference<JLabel> label = new AtomicReference<>();
        AtomicReference<JProgressBar> bar = new AtomicReference<>();

        onEdt(() -> {
            JLabel text = new JLabel(title);
            JProgressBar progress = new JProgressBar();
            progress.setIndeterminate(true);

            JPanel content = new JPanel(new BorderLayout(0, 10));
            content.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
            content.add(text, BorderLayout.NORTH);
            content.add(progress, BorderLayout.CENTER);
            content.setPreferredSize(new Dimension(420, 90));

            JDialog window = newDialog(content);
            window.setVisible(true);

            dialog.set(window);
            label.set(text);
            bar.set(progress);

            return null;
        });

        return new Progress() {

            @Override
            public void update(String message, long done, long total) {

                SwingUtilities.invokeLater(() -> {

                    label.get().setText(message);

                    if (total > 0) {
                        bar.get().setIndeterminate(false);
                        bar.get().setMaximum(1000);
                        bar.get().setValue((int) (done * 1000 / total));
                    } else {
                        bar.get().setIndeterminate(true);
                    }
                });
            }

            @Override
            public void close() {
                SwingUtilities.invokeLater(() -> dialog.get().dispose());
            }
        };
    }

    @Override
    public void antivirusWait(int seconds, Path folder) throws InterruptedException {

        AtomicReference<JLabel> label = new AtomicReference<>();
        AtomicReference<JDialog> dialog = new AtomicReference<>();

        onEdt(() -> {
            JLabel text = new JLabel();
            JTextArea hint = text(
                    "Si votre antivirus affiche une alerte, autorisez les fichiers, ou ajoutez ce dossier à ses "
                            + "exclusions :\n" + folder,
                    4,
                    44
            );
            hint.setOpaque(false);

            JProgressBar progress = new JProgressBar();
            progress.setIndeterminate(true);

            JPanel content = new JPanel(new BorderLayout(0, 10));
            content.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
            content.add(text, BorderLayout.NORTH);
            content.add(progress, BorderLayout.CENTER);
            content.add(hint, BorderLayout.SOUTH);
            content.setPreferredSize(new Dimension(460, 150));

            JDialog window = newDialog(content);
            window.setVisible(true);

            label.set(text);
            dialog.set(window);

            return null;
        });

        try {
            for (int remaining = seconds; remaining > 0; remaining--) {

                int shown = remaining;

                SwingUtilities.invokeLater(() -> label.get().setText(
                        "Analyse antivirus en cours… vérification dans " + shown + " s"
                ));

                Thread.sleep(1000);
            }
        } finally {
            SwingUtilities.invokeLater(() -> dialog.get().dispose());
        }
    }

    @Override
    public boolean askRetry(List<String> problems, Path folder) {

        String message = "Des fichiers ont disparu ou été modifiés juste après leur extraction — très "
                + "probablement par votre antivirus :\n\n  • " + String.join("\n  • ", problems)
                + "\n\nAjoutez ce dossier aux exclusions de votre antivirus, puis cliquez sur Réessayer :\n"
                + folder;

        JTextArea area = text(message, 12, 52);

        String[] options = {"Réessayer", "Annuler"};

        Integer choice = onEdt(() -> JOptionPane.showOptionDialog(
                null,
                new JScrollPane(area),
                TITLE + " — Fichiers bloqués",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]
        ));

        return Integer.valueOf(0).equals(choice);
    }

    @Override
    public void warn(String title, String message) {
        onEdt(() -> {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
            return null;
        });
    }

    @Override
    public void error(String title, String message) {
        onEdt(() -> {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            return null;
        });
    }

    /** Zone de texte en lecture seule avec la police du système (celle d'un JTextArea est monospace). */
    private static JTextArea text(String content, int rows, int columns) {

        JTextArea area = new JTextArea(content, rows, columns);

        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(UIManager.getFont("Label.font"));

        return area;
    }

    private JDialog newDialog(JPanel content) {

        JDialog dialog = new JDialog((java.awt.Frame) null, TITLE, false);

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setResizable(false);
        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setAlwaysOnTop(true);

        if (icon != null) {
            dialog.setIconImage(icon);
        }

        return dialog;
    }

    private static <T> T onEdt(Supplier<T> action) {

        if (SwingUtilities.isEventDispatchThread()) {
            return action.get();
        }

        AtomicReference<T> result = new AtomicReference<>();

        try {
            SwingUtilities.invokeAndWait(() -> result.set(action.get()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        }

        return result.get();
    }
}
