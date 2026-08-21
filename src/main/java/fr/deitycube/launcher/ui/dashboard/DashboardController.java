package fr.deitycube.launcher.ui.dashboard;

import com.sun.management.OperatingSystemMXBean;
import fr.deitycube.launcher.deitycube.DeityCubeManifest;
import fr.deitycube.launcher.filesystem.GameDirectory;
import fr.deitycube.launcher.minecraft.MinecraftResolvedVersion;
import fr.deitycube.launcher.minecraft.launch.MinecraftLauncher;
import fr.deitycube.launcher.progress.InstallationPipeline;
import fr.deitycube.launcher.progress.ProgressListener;
import fr.deitycube.launcher.settings.LauncherSettings;
import fr.deitycube.launcher.ui.LauncherApp;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;

import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;

public final class DashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label versionInfoLabel;

    @FXML
    private ComboBox<String> profileComboBox;

    @FXML
    private Slider ramSlider;

    @FXML
    private Label ramValueLabel;

    @FXML
    private CheckBox closeLauncherCheckbox;

    @FXML
    private Button playButton;

    @FXML
    private Button reinstallButton;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label phaseLabel;

    @FXML
    private Label detailLabel;

    private LauncherApp app;
    private final InstallationPipeline pipeline = new InstallationPipeline();

    public void init(LauncherApp app) {

        this.app = app;

        LauncherSettings settings = app.getSettings();

        welcomeLabel.setText(
                "Bonjour, "
                        + app.getAuthentication().getUsername()
        );

        configureRamSlider(settings);

        closeLauncherCheckbox.setSelected(
                settings.isCloseLauncherOnPlay()
        );

        playButton.setDisable(true);
        reinstallButton.setDisable(true);
        phaseLabel.setText("Chargement du manifest DeityCube...");
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        loadManifest();
    }

    private void configureRamSlider(LauncherSettings settings) {

        int totalRamMb = getSystemRamMb();
        int maxRamMb = Math.min(totalRamMb, 16384);
        int defaultRamMb = roundTo512(totalRamMb / 2);

        ramSlider.setMin(1024);
        ramSlider.setMax(Math.max(maxRamMb, 2048));

        int savedRamMb = settings.getAllocatedRamMb();

        ramSlider.setValue(
                savedRamMb > 0 ? savedRamMb : defaultRamMb
        );

        ramValueLabel.setText(formatRam((int) ramSlider.getValue()));

        ramSlider.valueProperty().addListener((obs, oldValue, newValue) ->
                ramValueLabel.setText(formatRam(newValue.intValue()))
        );
    }

    private int getSystemRamMb() {

        OperatingSystemMXBean bean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        return (int) (bean.getTotalMemorySize() / (1024 * 1024));
    }

    private int roundTo512(int value) {
        return Math.max(1024, (value / 512) * 512);
    }

    private String formatRam(int ramMb) {

        if (ramMb >= 1024) {
            return String.format("%.1f Go", ramMb / 1024.0);
        }

        return ramMb + " Mo";
    }

    private void loadManifest() {

        Task<DeityCubeManifest> task = new Task<>() {
            @Override
            protected DeityCubeManifest call() throws Exception {
                return InstallationPipeline.fetchManifest();
            }
        };

        task.setOnSucceeded(event -> {

            DeityCubeManifest manifest = task.getValue();

            app.setManifest(manifest);

            profileComboBox.getItems().setAll(
                    manifest.getPackages().keySet()
            );

            String savedProfile = app.getSettings().getSelectedProfile();

            if (savedProfile != null
                    && profileComboBox.getItems().contains(savedProfile)) {

                profileComboBox.getSelectionModel().select(savedProfile);

            } else if (!profileComboBox.getItems().isEmpty()) {

                profileComboBox.getSelectionModel().selectFirst();
            }

            versionInfoLabel.setText(
                    "Minecraft "
                            + manifest.getMinecraftVersion()
                            + "  •  NeoForge "
                            + manifest.getNeoforgeVersion()
                            + "  •  Modpack "
                            + manifest.getModpackVersion()
            );

            phaseLabel.setText("Prêt.");
            progressBar.setProgress(0);

            playButton.setDisable(false);
            reinstallButton.setDisable(false);
        });

        task.setOnFailed(event ->
                showErrorDialog(
                        "Impossible de récupérer le manifest DeityCube",
                        task.getException()
                )
        );

        runInBackground(task, "manifest-load");
    }

    @FXML
    private void onPlay() {

        persistSelection();

        runGameTask(false);
    }

    @FXML
    private void onReinstall() {

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Réinstaller le jeu");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Cela va supprimer et retélécharger Minecraft, NeoForge "
                        + "et le modpack. Continuer ?"
        );

        confirm.showAndWait().filter(button -> button == ButtonType.OK)
                .ifPresent(button -> {
                    persistSelection();
                    runGameTask(true);
                });
    }

    @FXML
    private void onOpenGameFolder() {
        openFolder(GameDirectory.getGameDirectory());
    }

    @FXML
    private void onOpenLogsFolder() {
        openFolder(GameDirectory.getLogsDirectory());
    }

    @FXML
    private void onLogout() {

        app.setAuthentication(null);
        app.showLogin();
    }

    private void openFolder(java.nio.file.Path folder) {

        try {

            Files.createDirectories(folder);

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(folder.toFile());
            }

        } catch (IOException e) {
            showErrorDialog("Impossible d'ouvrir le dossier", e);
        }
    }

    private void persistSelection() {

        LauncherSettings settings = app.getSettings();

        settings.setSelectedProfile(
                profileComboBox.getSelectionModel().getSelectedItem()
        );

        settings.setAllocatedRamMb((int) ramSlider.getValue());
        settings.setCloseLauncherOnPlay(closeLauncherCheckbox.isSelected());

        app.saveSettings();
    }

    private void runGameTask(boolean reinstall) {

        setControlsDisabled(true);

        String profile = profileComboBox.getSelectionModel().getSelectedItem();
        int ramMb = (int) ramSlider.getValue();
        boolean closeOnPlay = closeLauncherCheckbox.isSelected() && !reinstall;

        GameTask task = new GameTask(reinstall, profile, ramMb, closeOnPlay);

        progressBar.progressProperty().bind(task.progressProperty());
        phaseLabel.textProperty().bind(task.titleProperty());
        detailLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(event -> {
            unbindProgress();
            phaseLabel.setText("Prêt.");
            detailLabel.setText("");
            progressBar.setProgress(0);
            setControlsDisabled(false);
        });

        task.setOnFailed(event -> {
            unbindProgress();
            phaseLabel.setText("Échec.");
            progressBar.setProgress(0);
            setControlsDisabled(false);
            showErrorDialog(
                    reinstall
                            ? "La réinstallation a échoué"
                            : "Le lancement du jeu a échoué",
                    task.getException()
            );
        });

        runInBackground(task, reinstall ? "reinstall" : "play");
    }

    private void unbindProgress() {
        progressBar.progressProperty().unbind();
        phaseLabel.textProperty().unbind();
        detailLabel.textProperty().unbind();
    }

    private void setControlsDisabled(boolean disabled) {
        playButton.setDisable(disabled);
        reinstallButton.setDisable(disabled);
        profileComboBox.setDisable(disabled);
        ramSlider.setDisable(disabled);
    }

    private void runInBackground(Task<?> task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }

    private void showErrorDialog(String header, Throwable error) {

        Platform.runLater(() -> {

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(header);
            alert.setContentText(
                    error != null ? error.getMessage() : "Erreur inconnue."
            );

            if (error != null) {

                StringWriter stringWriter = new StringWriter();
                error.printStackTrace(new PrintWriter(stringWriter));

                TextArea textArea = new TextArea(stringWriter.toString());
                textArea.setEditable(false);
                textArea.setWrapText(true);

                alert.getDialogPane().setExpandableContent(textArea);
            }

            ButtonType openLogs = new ButtonType("Ouvrir les logs");
            alert.getButtonTypes().add(openLogs);

            alert.showAndWait().ifPresent(button -> {
                if (button == openLogs) {
                    onOpenLogsFolder();
                }
            });
        });
    }

    private final class GameTask extends Task<Void> implements ProgressListener {

        private final boolean reinstall;
        private final String profile;
        private final int ramMb;
        private final boolean closeOnPlay;

        private GameTask(
                boolean reinstall,
                String profile,
                int ramMb,
                boolean closeOnPlay
        ) {
            this.reinstall = reinstall;
            this.profile = profile;
            this.ramMb = ramMb;
            this.closeOnPlay = closeOnPlay;
        }

        @Override
        protected Void call() throws Exception {

            DeityCubeManifest manifest = app.getManifest();

            MinecraftResolvedVersion resolved =
                    reinstall
                            ? pipeline.reinstall(manifest, profile, this)
                            : pipeline.install(manifest, profile, this);

            if (reinstall) {
                return null;
            }

            updateTitle("Lancement du jeu");
            updateMessage("Démarrage de Minecraft...");
            updateProgress(-1, 1);

            Process process = new MinecraftLauncher().launch(
                    resolved,
                    app.getAuthentication(),
                    ramMb
            );

            if (closeOnPlay) {
                Platform.runLater(() -> app.getStage().hide());
            }

            int exitCode = process.waitFor();

            if (closeOnPlay) {
                Platform.runLater(() -> app.getStage().show());
            }

            if (exitCode != 0) {
                throw new IOException(
                        "Minecraft s'est terminé avec le code "
                                + exitCode
                );
            }

            return null;
        }

        @Override
        public void update(
                String phase,
                String detail,
                long current,
                long total
        ) {
            updateTitle(phase);
            updateMessage(detail);

            if (total <= 0) {
                updateProgress(-1, 1);
            } else {
                updateProgress(current, total);
            }
        }
    }
}
