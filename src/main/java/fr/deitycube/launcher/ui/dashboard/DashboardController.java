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
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.List;

public final class DashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private HBox profileToggle;

    @FXML
    private Button playButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Button logoutButton;

    @FXML
    private VBox idleBox;

    @FXML
    private VBox progressBox;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label phaseLabel;

    @FXML
    private Label detailLabel;

    @FXML
    private StackPane settingsOverlay;

    @FXML
    private VBox modalCard;

    @FXML
    private Label versionsInfoLabel;

    @FXML
    private VBox ramRow;

    @FXML
    private Slider ramSlider;

    @FXML
    private Label ramValueLabel;

    @FXML
    private CheckBox keepOpenCheckbox;

    @FXML
    private Button logsButton;

    @FXML
    private Button gameFolderButton;

    @FXML
    private Button reinstallButton;

    private LauncherApp app;
    private final InstallationPipeline pipeline = new InstallationPipeline();
    private String selectedProfile;

    public void init(LauncherApp app) {

        this.app = app;

        LauncherSettings settings = app.getSettings();

        welcomeLabel.setText(
                "Bonjour, " + app.getAuthentication().getUsername()
        );

        configureRamSlider(settings);
        clipToBounds(modalCard);

        keepOpenCheckbox.setSelected(settings.isKeepLauncherOpenWhilePlaying());

        playButton.setDisable(true);
        profileToggle.setDisable(true);

        app.ensureManifestLoaded(
                this::onManifestReady,
                error -> showErrorDialog("Impossible de récupérer le manifest DeityCube", error)
        );
    }

    private void onManifestReady() {

        DeityCubeManifest manifest = app.getManifest();

        List<String> profiles = List.copyOf(manifest.getProfiles().keySet());

        String savedProfile = app.getSettings().getSelectedProfile();

        String initialProfile =
                savedProfile != null && profiles.contains(savedProfile)
                        ? savedProfile
                        : profiles.isEmpty() ? null : profiles.get(0);

        buildProfileToggle(profiles, initialProfile);

        versionsInfoLabel.setText(
                "Minecraft " + manifest.getMinecraftVersion()
                        + "\nNeoForge " + manifest.getNeoforgeVersion()
                        + "\nModpack DeityCube " + manifest.getModpackVersion()
        );

        playButton.setDisable(false);
        profileToggle.setDisable(false);
    }

    private void buildProfileToggle(List<String> profiles, String initialProfile) {

        profileToggle.getChildren().clear();

        for (String profile : profiles) {

            Button segment = new Button(profile);
            segment.getStyleClass().add("segment-button");
            segment.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(segment, Priority.ALWAYS);
            segment.setOnAction(event -> selectProfile(profile));

            profileToggle.getChildren().add(segment);
        }

        selectProfile(initialProfile);
    }

    private void selectProfile(String profile) {

        selectedProfile = profile;

        for (var node : profileToggle.getChildren()) {

            Button segment = (Button) node;

            segment.getStyleClass().remove("selected-segment");

            if (segment.getText().equals(profile)) {
                segment.getStyleClass().add("selected-segment");
            }
        }
    }

    private void configureRamSlider(LauncherSettings settings) {

        int totalRamMb = getSystemRamMb();
        int maxRamMb = Math.max(Math.min(totalRamMb, 16384), 2048);
        int defaultRamMb = roundTo512(totalRamMb / 2);

        ramSlider.setMin(1024);
        ramSlider.setMax(maxRamMb);

        int savedRamMb = settings.getAllocatedRamMb();

        ramSlider.setValue(savedRamMb > 0 ? savedRamMb : defaultRamMb);

        ramValueLabel.setText(formatRam((int) ramSlider.getValue()));

        ramSlider.valueProperty().addListener((obs, oldValue, newValue) ->
                ramValueLabel.setText(formatRam(newValue.intValue()))
        );

        clipToBounds(ramRow);
    }

    private void clipToBounds(Region region) {

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());

        region.setClip(clip);
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
                    closeSettings();
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

    @FXML
    private void onOpenSettings() {

        settingsOverlay.setVisible(true);
        settingsOverlay.setManaged(true);
        settingsOverlay.setOpacity(0);

        FadeTransition fade = new FadeTransition(Duration.millis(180), settingsOverlay);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    @FXML
    private void onCloseSettings() {
        persistSelection();
        closeSettings();
    }

    @FXML
    private void onScrimClicked() {
        persistSelection();
        closeSettings();
    }

    @FXML
    private void onCardClicked(MouseEvent event) {
        event.consume();
    }

    private void closeSettings() {
        settingsOverlay.setVisible(false);
        settingsOverlay.setManaged(false);
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

        if (selectedProfile != null) {
            settings.setSelectedProfile(selectedProfile);
        }

        settings.setAllocatedRamMb((int) ramSlider.getValue());
        settings.setKeepLauncherOpenWhilePlaying(keepOpenCheckbox.isSelected());

        app.saveSettings();
    }

    private void runGameTask(boolean reinstall) {

        setControlsDisabled(true);

        idleBox.setVisible(false);
        idleBox.setManaged(false);
        progressBox.setVisible(true);
        progressBox.setManaged(true);

        String profile = selectedProfile;
        int ramMb = (int) ramSlider.getValue();
        boolean keepOpen = keepOpenCheckbox.isSelected() || reinstall;

        GameTask task = new GameTask(reinstall, profile, ramMb, keepOpen);

        progressBar.progressProperty().bind(task.progressProperty());
        phaseLabel.textProperty().bind(task.titleProperty());
        detailLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(event -> finishGameTask());

        task.setOnFailed(event -> {
            finishGameTask();
            showErrorDialog(
                    reinstall ? "La réinstallation a échoué" : "Le lancement du jeu a échoué",
                    task.getException()
            );
        });

        runInBackground(task, reinstall ? "reinstall" : "play");
    }

    private void finishGameTask() {

        progressBar.progressProperty().unbind();
        phaseLabel.textProperty().unbind();
        detailLabel.textProperty().unbind();

        progressBox.setVisible(false);
        progressBox.setManaged(false);
        idleBox.setVisible(true);
        idleBox.setManaged(true);

        setControlsDisabled(false);
    }

    private void setControlsDisabled(boolean disabled) {
        playButton.setDisable(disabled);
        profileToggle.setDisable(disabled);
        settingsButton.setDisable(disabled);
        logoutButton.setDisable(disabled);
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
        private final boolean keepOpen;

        private GameTask(boolean reinstall, String profile, int ramMb, boolean keepOpen) {
            this.reinstall = reinstall;
            this.profile = profile;
            this.ramMb = ramMb;
            this.keepOpen = keepOpen;
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

            if (!keepOpen) {
                Platform.runLater(() -> app.getStage().hide());
            }

            int exitCode = process.waitFor();

            if (!keepOpen) {
                Platform.runLater(() -> app.getStage().show());
            }

            if (exitCode != 0) {
                throw new IOException(
                        "Minecraft s'est terminé avec le code " + exitCode
                );
            }

            return null;
        }

        @Override
        public void update(String phase, String detail, long current, long total) {

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
