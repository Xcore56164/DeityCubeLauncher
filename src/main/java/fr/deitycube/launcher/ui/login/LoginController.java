package fr.deitycube.launcher.ui.login;

import fr.deitycube.launcher.config.LauncherConfig;
import fr.deitycube.launcher.deitycompte.DeityCompteAccount;
import fr.deitycube.launcher.deitycompte.DeityCompteApi;
import fr.deitycube.launcher.deitycompte.DeityCompteException;
import fr.deitycube.launcher.deitycompte.DeityCompteSession;
import fr.deitycube.launcher.settings.LauncherSettings;
import fr.deitycube.launcher.ui.LauncherApp;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public final class LoginController {

    @FXML
    private ImageView titleLogoView;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberMeCheckbox;

    @FXML
    private Button deityCompteLoginButton;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressIndicator loadingIndicator;

    private LauncherApp app;

    public void init(LauncherApp app) {

        this.app = app;

        playFloatAnimation();

        LauncherSettings settings = app.getSettings();

        emailField.setText(settings.getDeityCompteEmail());
        rememberMeCheckbox.setSelected(settings.isRememberMe());

        String savedSession = settings.getDeityCompteSession();

        if (savedSession != null && !savedSession.isBlank()) {
            attemptSilentDeityCompteLogin(savedSession);
        }
    }

    private void playFloatAnimation() {

        TranslateTransition floatUp = new TranslateTransition(Duration.seconds(2.75), titleLogoView);
        floatUp.setByY(-9);
        floatUp.setCycleCount(TranslateTransition.INDEFINITE);
        floatUp.setAutoReverse(true);
        floatUp.setInterpolator(Interpolator.EASE_BOTH);
        floatUp.play();
    }


    @FXML
    private void onDeityCompteLogin() {

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Indique l'email et le mot de passe de ton DeityCompte.", null);
            return;
        }

        setLoading(true, "Connexion à ton DeityCompte...");

        Task<DeityCompteSession> task = new Task<>() {
            @Override
            protected DeityCompteSession call() throws Exception {
                return DeityCompteApi.login(email, password);
            }
        };

        task.setOnSucceeded(event -> {

            DeityCompteSession session = task.getValue();

            passwordField.clear();
            persistDeityCompteSession(email, session.getSessionToken());

            app.setDeityCompteSession(session);

            setLoading(false, "");

            app.showDashboard();
        });

        task.setOnFailed(event -> {

            setLoading(false, "");
            passwordField.clear();

            showFailure(task.getException());
        });

        runInBackground(task, "deitycompte-login");
    }

    /** Session mémorisée : vérifiée auprès du site (compte suspendu, CGU à accepter...). */
    private void attemptSilentDeityCompteLogin(String sessionToken) {

        setLoading(true, "Reconnexion automatique...");

        Task<DeityCompteAccount> task = new Task<>() {
            @Override
            protected DeityCompteAccount call() throws Exception {
                return DeityCompteApi.fetchAccount(sessionToken);
            }
        };

        task.setOnSucceeded(event -> {

            app.setDeityCompteSession(
                    new DeityCompteSession(sessionToken, task.getValue())
            );

            setLoading(false, "");

            app.showDashboard();
        });

        task.setOnFailed(event -> {

            Throwable error = task.getException();

            if (error instanceof DeityCompteException accountError && accountError.isSessionInvalid()) {

                app.forgetDeityCompteSession(false);
                setLoading(false, accountError.getMessage());
                return;
            }

            // Site injoignable, compte à régulariser sur le site... : la session reste
            // mémorisée, le joueur pourra réessayer.
            setLoading(false, "");
            showFailure(error);
        });

        runInBackground(task, "deitycompte-silent-login");
    }

    @FXML
    private void onOpenSignup() {
        app.openUrl(LauncherConfig.SIGNUP_URL);
    }

    @FXML
    private void onOpenPasswordReset() {
        app.openUrl(LauncherConfig.PASSWORD_RESET_URL);
    }

    private void runInBackground(Task<?> task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }


    private void persistDeityCompteSession(String email, String sessionToken) {

        LauncherSettings settings = app.getSettings();

        settings.setRememberMe(rememberMeCheckbox.isSelected());
        settings.setDeityCompteEmail(email);

        // Sans "Se souvenir de moi", le jeton reste en mémoire et la session est fermée sur le
        // site à la fermeture du launcher.
        settings.setDeityCompteSession(
                rememberMeCheckbox.isSelected() ? sessionToken : null
        );

        app.saveSettings();
    }

    private void setLoading(boolean loading, String message) {

        Platform.runLater(() -> {
            loadingIndicator.setVisible(loading);
            loadingIndicator.setManaged(loading);
            deityCompteLoginButton.setDisable(loading);
            emailField.setDisable(loading);
            passwordField.setDisable(loading);
            statusLabel.setText(message);
        });
    }

    private void showFailure(Throwable error) {

        if (error instanceof DeityCompteException accountError) {
            showError(accountError.getMessage(), accountError.getUrl());
            return;
        }

        showError(
                "Connexion impossible : "
                        + (error != null ? error.getMessage() : "erreur inconnue."),
                null
        );
    }

    /** url : page du site où régler le problème (bouton « Ouvrir deitycube.fr »), ou null. */
    private void showError(String message, String url) {

        Platform.runLater(() -> {

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connexion impossible");
            alert.setHeaderText(null);
            alert.setContentText(message);

            ButtonType openSite = new ButtonType("Ouvrir deitycube.fr", ButtonBar.ButtonData.OTHER);

            if (url != null) {
                alert.getButtonTypes().add(openSite);
            }

            alert.showAndWait()
                    .filter(button -> button == openSite)
                    .ifPresent(button -> app.openUrl(url));
        });
    }
}
