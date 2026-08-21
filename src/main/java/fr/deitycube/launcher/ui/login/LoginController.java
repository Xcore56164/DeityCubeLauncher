package fr.deitycube.launcher.ui.login;

import fr.deitycube.launcher.auth.AuthenticationMode;
import fr.deitycube.launcher.auth.MicrosoftLoginFlow;
import fr.deitycube.launcher.auth.MicrosoftLoginResult;
import fr.deitycube.launcher.auth.OfflineAuthenticator;
import fr.deitycube.launcher.settings.LauncherSettings;
import fr.deitycube.launcher.ui.LauncherApp;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

public final class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private CheckBox rememberMeCheckbox;

    @FXML
    private Button offlineLoginButton;

    @FXML
    private Button microsoftLoginButton;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressIndicator loadingIndicator;

    private LauncherApp app;

    public void init(LauncherApp app) {

        this.app = app;

        LauncherSettings settings = app.getSettings();

        usernameField.setText(settings.getOfflineUsername());
        rememberMeCheckbox.setSelected(settings.isRememberMe());

        if (settings.isRememberMe()
                && "MICROSOFT".equals(settings.getAuthMode())
                && settings.getMicrosoftRefreshToken() != null
                && !settings.getMicrosoftRefreshToken().isBlank()) {

            attemptSilentMicrosoftLogin(settings.getMicrosoftRefreshToken());
        }
    }

    @FXML
    private void onOfflineLogin() {

        String username = usernameField.getText();

        try {

            var authentication =
                    OfflineAuthenticator.authenticate(username);

            persistOfflineSession(username);

            app.setAuthentication(authentication);
            app.showDashboard();

        } catch (IllegalArgumentException e) {

            showError(e.getMessage());
        }
    }

    @FXML
    private void onMicrosoftLogin() {

        setLoading(true, "Connexion à Microsoft...");

        Task<MicrosoftLoginResult> task = new Task<>() {
            @Override
            protected MicrosoftLoginResult call() throws Exception {
                return MicrosoftLoginFlow.login();
            }
        };

        task.setOnSucceeded(event -> {

            MicrosoftLoginResult result = task.getValue();

            persistMicrosoftSession(result.getRefreshToken());

            app.setAuthentication(result.getAuthentication());

            setLoading(false, "");

            app.showDashboard();
        });

        task.setOnFailed(event -> {

            setLoading(false, "");

            showError(
                    "Connexion Microsoft impossible : "
                            + task.getException().getMessage()
            );
        });

        Thread thread = new Thread(task, "microsoft-login");
        thread.setDaemon(true);
        thread.start();
    }

    private void attemptSilentMicrosoftLogin(String refreshToken) {

        setLoading(true, "Reconnexion automatique...");

        Task<MicrosoftLoginResult> task = new Task<>() {
            @Override
            protected MicrosoftLoginResult call() throws Exception {
                return MicrosoftLoginFlow.loginWithRefreshToken(refreshToken);
            }
        };

        task.setOnSucceeded(event -> {

            MicrosoftLoginResult result = task.getValue();

            persistMicrosoftSession(result.getRefreshToken());

            app.setAuthentication(result.getAuthentication());

            setLoading(false, "");

            app.showDashboard();
        });

        task.setOnFailed(event -> setLoading(false, ""));

        Thread thread = new Thread(task, "microsoft-silent-login");
        thread.setDaemon(true);
        thread.start();
    }

    private void persistOfflineSession(String username) {

        LauncherSettings settings = app.getSettings();

        settings.setAuthMode(AuthenticationMode.OFFLINE.name());
        settings.setRememberMe(rememberMeCheckbox.isSelected());
        settings.setMicrosoftRefreshToken(null);

        settings.setOfflineUsername(
                rememberMeCheckbox.isSelected() ? username.trim() : ""
        );

        app.saveSettings();
    }

    private void persistMicrosoftSession(String refreshToken) {

        LauncherSettings settings = app.getSettings();

        settings.setAuthMode(AuthenticationMode.MICROSOFT.name());
        settings.setRememberMe(rememberMeCheckbox.isSelected());

        settings.setMicrosoftRefreshToken(
                rememberMeCheckbox.isSelected() ? refreshToken : null
        );

        app.saveSettings();
    }

    private void setLoading(boolean loading, String message) {

        Platform.runLater(() -> {
            loadingIndicator.setVisible(loading);
            offlineLoginButton.setDisable(loading);
            microsoftLoginButton.setDisable(loading);
            statusLabel.setText(message);
        });
    }

    private void showError(String message) {

        Platform.runLater(() -> {

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connexion impossible");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
