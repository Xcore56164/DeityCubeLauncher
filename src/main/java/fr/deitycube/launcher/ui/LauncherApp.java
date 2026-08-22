package fr.deitycube.launcher.ui;

import fr.deitycube.launcher.auth.AuthenticationResult;
import fr.deitycube.launcher.deitycube.DeityCubeManifest;
import fr.deitycube.launcher.logging.LauncherLogger;
import fr.deitycube.launcher.progress.InstallationPipeline;
import fr.deitycube.launcher.settings.LauncherSettings;
import fr.deitycube.launcher.settings.LauncherSettingsStore;
import fr.deitycube.launcher.ui.dashboard.DashboardController;
import fr.deitycube.launcher.ui.login.LoginController;
import fr.deitycube.launcher.update.LauncherUpdateManifest;
import fr.deitycube.launcher.update.LauncherUpdater;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

public final class LauncherApp extends Application {

    private static final String ASSETS_PATH = "assets/";
    private static final String WEBSITE_URL = "https://deitycube.fr";

    private Stage stage;
    private StackPane contentArea;
    private Label deityCubeVersionLabel;
    private Label gameVersionsLabel;

    private LauncherSettings settings;
    private DeityCubeManifest manifest;
    private AuthenticationResult authentication;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        LauncherLogger.initialize();
    }

    @Override
    public void start(Stage primaryStage) {

        this.stage = primaryStage;
        this.settings = LauncherSettingsStore.load();

        StackPane shell = buildShell();

        Scene scene = new Scene(shell, 1180, 720);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("app.css")
                ).toExternalForm()
        );

        primaryStage.setTitle("DeityCube Launcher");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(620);
        primaryStage.setScene(scene);

        loadWindowIcon(primaryStage);

        showLogin();
        loadManifest();
        checkForLauncherUpdate();

        primaryStage.show();
    }

    private StackPane buildShell() {

        StackPane shell = new StackPane();
        shell.getStyleClass().add("app-shell");

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(shell.widthProperty());
        clip.heightProperty().bind(shell.heightProperty());
        shell.setClip(clip);

        MediaView background = createBackgroundVideo(shell);

        Region vignette = new Region();
        vignette.getStyleClass().add("vignette");
        vignette.setMouseTransparent(true);

        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        BorderPane chrome = new BorderPane();
        chrome.setPickOnBounds(false);
        chrome.setCenter(contentArea);
        chrome.setBottom(buildFooter());

        if (background != null) {
            shell.getChildren().add(background);
        }

        shell.getChildren().addAll(vignette, chrome);

        return shell;
    }

    private MediaView createBackgroundVideo(StackPane container) {

        URL videoUrl = getClass().getResource(ASSETS_PATH + "background.mp4");

        if (videoUrl == null) {
            return null;
        }

        try {

            Media media = new Media(videoUrl.toExternalForm());
            MediaPlayer player = new MediaPlayer(media);

            player.setMute(true);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setAutoPlay(true);

            MediaView view = new MediaView(player);
            view.setPreserveRatio(false);
            view.setMouseTransparent(true);

            player.setOnReady(() -> {

                double videoWidth = media.getWidth();
                double videoHeight = media.getHeight();

                if (videoWidth <= 0 || videoHeight <= 0) {
                    return;
                }

                DoubleBinding scale = Bindings.createDoubleBinding(
                        () -> Math.max(
                                container.getWidth() / videoWidth,
                                container.getHeight() / videoHeight
                        ),
                        container.widthProperty(),
                        container.heightProperty()
                );

                view.fitWidthProperty().bind(scale.multiply(videoWidth));
                view.fitHeightProperty().bind(scale.multiply(videoHeight));
            });

            return view;

        } catch (Exception e) {

            System.err.println(
                    "Fond vidéo indisponible : " + e.getMessage()
            );

            return null;
        }
    }

    private BorderPane buildFooter() {

        BorderPane footer = new BorderPane();
        footer.getStyleClass().add("footer-bar");

        deityCubeVersionLabel = new Label("DeityCube —");
        deityCubeVersionLabel.getStyleClass().add("footer-label");

        gameVersionsLabel = new Label("—");
        gameVersionsLabel.getStyleClass().add("footer-label");

        Label siteLink = new Label("deitycube.fr");
        siteLink.getStyleClass().add("footer-link");
        siteLink.setOnMouseClicked(event -> openWebsite());

        HBox center = new HBox(siteLink);
        center.setAlignment(Pos.CENTER);

        BorderPane.setAlignment(deityCubeVersionLabel, Pos.CENTER_LEFT);
        BorderPane.setAlignment(gameVersionsLabel, Pos.CENTER_RIGHT);

        footer.setLeft(deityCubeVersionLabel);
        footer.setCenter(center);
        footer.setRight(gameVersionsLabel);
        footer.setPadding(new Insets(0));

        return footer;
    }

    private void openWebsite() {

        Thread thread = new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI.create(WEBSITE_URL));
                }
            } catch (Exception e) {
                System.err.println(
                        "Impossible d'ouvrir le site web : " + e.getMessage()
                );
            }
        }, "open-website");

        thread.setDaemon(true);
        thread.start();
    }

    private void loadWindowIcon(Stage stage) {

        try {
            stage.getIcons().add(
                    new Image(
                            Objects.requireNonNull(
                                    getClass().getResourceAsStream(
                                            ASSETS_PATH + "logo.png"
                                    )
                            )
                    )
            );
        } catch (Exception ignored) {
            // Icône optionnelle tant que l'asset n'est pas fourni.
        }
    }

    private void loadManifest() {

        Task<DeityCubeManifest> task = new Task<>() {
            @Override
            protected DeityCubeManifest call() throws Exception {
                return InstallationPipeline.fetchManifest();
            }
        };

        task.setOnSucceeded(event -> {

            manifest = task.getValue();

            deityCubeVersionLabel.setText(
                    "DeityCube " + manifest.getModpackVersion()
            );

            gameVersionsLabel.setText(
                    "Minecraft " + manifest.getMinecraftVersion()
                            + "   •   NeoForge " + manifest.getNeoforgeVersion()
            );
        });

        task.setOnFailed(event ->
                deityCubeVersionLabel.setText("DeityCube — hors ligne")
        );

        Thread thread = new Thread(task, "manifest-startup-fetch");
        thread.setDaemon(true);
        thread.start();
    }

    public void showLogin() {
        swapContent("login/login-content.fxml", (LoginController controller) -> controller.init(this));
    }

    public void showDashboard() {
        swapContent("dashboard/dashboard-content.fxml", (DashboardController controller) -> controller.init(this));
    }

    private <T> void swapContent(String fxmlPath, java.util.function.Consumer<T> initializer) {

        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            T controller = loader.getController();
            initializer.accept(controller);

            root.setOpacity(0);
            contentArea.getChildren().setAll(root);

            FadeTransition fade = new FadeTransition(Duration.millis(260), root);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Impossible de charger l'écran : " + fxmlPath,
                    e
            );
        }
    }

    public Stage getStage() {
        return stage;
    }

    public LauncherSettings getSettings() {
        return settings;
    }

    public void saveSettings() {
        try {
            LauncherSettingsStore.save(settings);
        } catch (IOException e) {
            System.err.println(
                    "Impossible d'enregistrer les réglages : " + e.getMessage()
            );
        }
    }

    public DeityCubeManifest getManifest() {
        return manifest;
    }

    public void ensureManifestLoaded(Runnable onReady, java.util.function.Consumer<Throwable> onError) {

        if (manifest != null) {
            onReady.run();
            return;
        }

        Task<DeityCubeManifest> task = new Task<>() {
            @Override
            protected DeityCubeManifest call() throws Exception {
                return InstallationPipeline.fetchManifest();
            }
        };

        task.setOnSucceeded(event -> {
            manifest = task.getValue();
            deityCubeVersionLabel.setText("DeityCube " + manifest.getModpackVersion());
            gameVersionsLabel.setText(
                    "Minecraft " + manifest.getMinecraftVersion()
                            + "   •   NeoForge " + manifest.getNeoforgeVersion()
            );
            onReady.run();
        });

        task.setOnFailed(event -> Platform.runLater(() -> onError.accept(task.getException())));

        Thread thread = new Thread(task, "manifest-fetch");
        thread.setDaemon(true);
        thread.start();
    }

    public AuthenticationResult getAuthentication() {
        return authentication;
    }

    public void setAuthentication(AuthenticationResult authentication) {
        this.authentication = authentication;
    }

    private void checkForLauncherUpdate() {

        Task<LauncherUpdateManifest> task = new Task<>() {
            @Override
            protected LauncherUpdateManifest call() throws Exception {
                return new LauncherUpdater().checkForUpdate();
            }
        };

        task.setOnSucceeded(event -> {

            LauncherUpdateManifest update = task.getValue();

            if (update != null) {
                promptLauncherUpdate(update);
            }
        });

        task.setOnFailed(event ->
                System.err.println(
                        "Vérification de mise à jour du launcher impossible : "
                                + task.getException()
                )
        );

        Thread thread = new Thread(task, "update-check");
        thread.setDaemon(true);
        thread.start();
    }

    private void promptLauncherUpdate(LauncherUpdateManifest update) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Mise à jour disponible");
        alert.setHeaderText("DeityCube Launcher " + update.getVersion() + " est disponible");
        alert.setContentText(
                (update.getNotes() != null && !update.getNotes().isBlank()
                        ? update.getNotes() + "\n\n"
                        : "")
                        + "Voulez-vous l'installer maintenant ? Le launcher redémarrera automatiquement."
        );

        alert.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .ifPresent(button -> runLauncherUpdate(update));
    }

    private void runLauncherUpdate(LauncherUpdateManifest update) {

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {

                new LauncherUpdater().downloadAndApply(update, (phase, detail, current, total) -> {
                    updateTitle(phase);
                    updateMessage(detail);
                    updateProgress(-1, 1);
                });

                return null;
            }
        };

        Stage progressDialog = showUpdateProgressDialog(task);

        task.setOnFailed(event -> {
            progressDialog.close();
            showUpdateErrorDialog(task.getException());
        });

        Thread thread = new Thread(task, "launcher-update");
        thread.setDaemon(true);
        thread.start();
    }

    private Stage showUpdateProgressDialog(Task<Void> task) {

        Label titleLabel = new Label("Mise à jour du launcher...");
        titleLabel.getStyleClass().add("welcome-label");

        ProgressBar progressBar = new ProgressBar();
        progressBar.progressProperty().bind(task.progressProperty());
        progressBar.setPrefWidth(320);

        Label statusLabel = new Label();
        statusLabel.textProperty().bind(task.messageProperty());

        VBox content = new VBox(14, titleLabel, progressBar, statusLabel);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(28));
        content.getStyleClass().add("modal-card");

        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initOwner(stage);
        dialog.initModality(Modality.APPLICATION_MODAL);

        Scene scene = new Scene(content, 380, 170);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("app.css")).toExternalForm()
        );

        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.show();

        return dialog;
    }

    private void showUpdateErrorDialog(Throwable error) {

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("La mise à jour du launcher a échoué");
        alert.setContentText(error != null ? error.getMessage() : "Erreur inconnue.");
        alert.showAndWait();
    }
}
