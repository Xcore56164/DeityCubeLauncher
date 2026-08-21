package fr.deitycube.launcher.ui;

import fr.deitycube.launcher.auth.AuthenticationResult;
import fr.deitycube.launcher.deitycube.DeityCubeManifest;
import fr.deitycube.launcher.settings.LauncherSettings;
import fr.deitycube.launcher.settings.LauncherSettingsStore;
import fr.deitycube.launcher.ui.dashboard.DashboardController;
import fr.deitycube.launcher.ui.login.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public final class LauncherApp extends Application {

    private Stage stage;
    private LauncherSettings settings;
    private DeityCubeManifest manifest;
    private AuthenticationResult authentication;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        this.stage = primaryStage;
        this.settings = LauncherSettingsStore.load();

        primaryStage.setTitle("DeityCube Launcher");
        primaryStage.setResizable(false);

        try {
            primaryStage.getIcons().add(
                    new Image(
                            Objects.requireNonNull(
                                    getClass().getResourceAsStream(
                                            "icon.png"
                                    )
                            )
                    )
            );
        } catch (Exception ignored) {
            // Icône optionnelle.
        }

        showLogin();

        primaryStage.show();
    }

    public void showLogin() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("login/login.fxml")
            );

            javafx.scene.Parent root = loader.load();

            LoginController controller = loader.getController();
            controller.init(this);

            stage.setScene(new Scene(root));

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Impossible de charger l'écran de connexion.",
                    e
            );
        }
    }

    public void showDashboard() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("dashboard/dashboard.fxml")
            );

            javafx.scene.Parent root = loader.load();

            DashboardController controller = loader.getController();
            controller.init(this);

            stage.setScene(new Scene(root));

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Impossible de charger le tableau de bord.",
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
                    "Impossible d'enregistrer les réglages : "
                            + e.getMessage()
            );
        }
    }

    public DeityCubeManifest getManifest() {
        return manifest;
    }

    public void setManifest(DeityCubeManifest manifest) {
        this.manifest = manifest;
    }

    public AuthenticationResult getAuthentication() {
        return authentication;
    }

    public void setAuthentication(AuthenticationResult authentication) {
        this.authentication = authentication;
    }
}
