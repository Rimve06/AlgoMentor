package com.algomentor.controller;

import com.algomentor.model.PendingRegistration;
import com.algomentor.model.User;
import com.algomentor.util.AppExecutors;
import com.algomentor.util.PasswordUtil;
import com.algomentor.util.SessionContext;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

public class LoginController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    // --- login/register card ---
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private VBox loginCard;

    // --- verification card (shown right after registering) ---
    @FXML private VBox verificationCard;
    @FXML private Label verificationEmailLabel;
    @FXML private TextField verificationCodeField;
    @FXML private Label verificationStatusLabel;
    @FXML private Button verifyButton;
    @FXML private Button resendButton;
    @FXML private Button cancelVerificationButton;

    /** The username currently waiting on a verification code, if any (not a real account yet). */
    private String pendingUsername;
    private String pendingEmail;

    // -----------------------------------------------------------------
    // Login (only ever checks the `users` table - real, verified accounts)
    // -----------------------------------------------------------------

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Enter a username and password.");
            return;
        }
        setControlsDisabled(true);
        statusLabel.setText("Signing in...");

        AppExecutors.get().submit(() -> {
            try {
                User user = SessionContext.db().findUserByUsername(username);
                if (user == null || !PasswordUtil.verify(password, user.getSalt(), user.getPasswordHash())) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Invalid username or password.");
                        setControlsDisabled(false);
                    });
                    return;
                }
                SessionContext.setCurrentUser(user);
                Platform.runLater(this::goToMainScreen);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Login failed: " + e.getMessage());
                    setControlsDisabled(false);
                });
            }
        });
    }

    // -----------------------------------------------------------------
    // Register - creates a PENDING registration only. Nothing is written
    // to the `users` table (and no username/email is reserved) until the
    // emailed code is actually verified below.
    // -----------------------------------------------------------------

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (username.length() < 3) {
            statusLabel.setText("Username must be at least 3 characters.");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            statusLabel.setText("Enter a valid email address (e.g. you@gmail.com).");
            return;
        }
        if (password.length() < 4) {
            statusLabel.setText("Password must be at least 4 characters.");
            return;
        }
        setControlsDisabled(true);
        statusLabel.setText("Sending verification code...");

        AppExecutors.get().submit(() -> {
            try {
                // Only a REAL (already-verified) account blocks registration.
                if (SessionContext.db().findUserByUsername(username) != null) {
                    Platform.runLater(() -> {
                        statusLabel.setText("That username is already registered.");
                        setControlsDisabled(false);
                    });
                    return;
                }
                if (SessionContext.db().findUserByEmail(email) != null) {
                    Platform.runLater(() -> {
                        statusLabel.setText("That email is already registered.");
                        setControlsDisabled(false);
                    });
                    return;
                }
                String salt = PasswordUtil.generateSalt();
                String hash = PasswordUtil.hash(password, salt);
                String code = PasswordUtil.generateNumericCode();
                SessionContext.db().savePendingRegistration(username, email, hash, salt, code,
                        LocalDateTime.now().plusMinutes(10));

                Platform.runLater(() -> {
                    statusLabel.setText("");
                    setControlsDisabled(false);
                    showVerificationCard(username, email);
                });
                SessionContext.email().sendVerificationEmail(email, username, code)
                        .thenAccept(resultMessage -> Platform.runLater(() ->
                                verificationStatusLabel.setText(resultMessage)));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Registration failed: " + e.getMessage());
                    setControlsDisabled(false);
                });
            }
        });
    }

    // -----------------------------------------------------------------
    // Email verification - this is the moment the account becomes real.
    // -----------------------------------------------------------------

    private void showVerificationCard(String username, String email) {
        pendingUsername = username;
        pendingEmail = email;
        verificationEmailLabel.setText("We sent a 6-digit code to " + email);
        verificationCodeField.clear();
        verificationStatusLabel.setText("Sending email...");
        loginCard.setVisible(false);
        loginCard.setManaged(false);
        verificationCard.setVisible(true);
        verificationCard.setManaged(true);
    }

    private void hideVerificationCard() {
        verificationCard.setVisible(false);
        verificationCard.setManaged(false);
        loginCard.setVisible(true);
        loginCard.setManaged(true);
        pendingUsername = null;
        pendingEmail = null;
    }

    @FXML
    private void handleVerifyCode() {
        if (pendingUsername == null) return;
        String code = verificationCodeField.getText();
        if (code == null || code.isBlank()) {
            verificationStatusLabel.setText("Enter the code from your email.");
            return;
        }
        verifyButton.setDisable(true);
        verificationStatusLabel.setText("Checking code...");
        String username = pendingUsername;
        String email = pendingEmail;

        AppExecutors.get().submit(() -> {
            try {
                User user = SessionContext.db().completeRegistration(username, code);
                if (user == null) {
                    Platform.runLater(() -> {
                        verificationStatusLabel.setText("Incorrect or expired code. Try again or resend.");
                        verifyButton.setDisable(false);
                    });
                    return;
                }
                SessionContext.setCurrentUser(user);
                SessionContext.email().sendWelcomeEmail(email, username);
                Platform.runLater(this::goToMainScreen);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    verificationStatusLabel.setText("Verification failed: " + e.getMessage());
                    verifyButton.setDisable(false);
                });
            }
        });
    }

    @FXML
    private void handleResendCode() {
        if (pendingUsername == null) return;
        resendButton.setDisable(true);
        verificationStatusLabel.setText("Resending code...");
        String username = pendingUsername;
        String email = pendingEmail;
        String code = PasswordUtil.generateNumericCode();

        AppExecutors.get().submit(() -> {
            try {
                PendingRegistration existing = SessionContext.db().findPendingByUsername(username);
                if (existing == null) {
                    Platform.runLater(() -> {
                        verificationStatusLabel.setText("This registration expired - please register again.");
                        resendButton.setDisable(false);
                        hideVerificationCard();
                    });
                    return;
                }
                SessionContext.db().savePendingRegistration(existing.username(), existing.email(),
                        existing.passwordHash(), existing.salt(), code, LocalDateTime.now().plusMinutes(10));
                SessionContext.email().sendVerificationEmail(email, username, code)
                        .thenAccept(resultMessage -> Platform.runLater(() -> {
                            verificationStatusLabel.setText(resultMessage);
                            resendButton.setDisable(false);
                        }));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    verificationStatusLabel.setText("Could not resend: " + e.getMessage());
                    resendButton.setDisable(false);
                });
            }
        });
    }

    @FXML
    private void handleCancelVerification() {
        hideVerificationCard();
        statusLabel.setText("Registration cancelled - that username/email is free to use again.");
    }

    // -----------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------

    private void setControlsDisabled(boolean disabled) {
        loginButton.setDisable(disabled);
        registerButton.setDisable(disabled);
    }

    private void goToMainScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            statusLabel.setText("Could not load main screen: " + e.getMessage());
            setControlsDisabled(false);
        }
    }
}
