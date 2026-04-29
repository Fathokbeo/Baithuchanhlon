package main.client.controller;

import main.client.AppContext;
import main.shared.dto.RegisterRequest;
import main.shared.model.Role;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.concurrent.CompletionException;

public final class LoginController {
    @FXML
    private TextField loginUsernameField;
    @FXML
    private PasswordField loginPasswordField;
    @FXML
    private TextField registerDisplayNameField;
    @FXML
    private TextField registerUsernameField;
    @FXML
    private PasswordField registerPasswordField;
    @FXML
    private ComboBox<Role> roleComboBox;
    @FXML
    private Label statusLabel;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;

    @FXML
    private void initialize() {
        roleComboBox.setItems(FXCollections.observableArrayList(Role.BIDDER, Role.SELLER));
        roleComboBox.setValue(Role.BIDDER);
    }

    @FXML
    private void handleLogin() {
        if (loginButton.isDisabled()) {
            return;
        }
        setBusy(true, "Dang dang nhap...");
        AppContext.service().login(loginUsernameField.getText().trim(), loginPasswordField.getText())
                .thenAccept(response -> Platform.runLater(() -> {
                    AppContext.state().setCurrentUser(response.user());
                    openDashboard();
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> setBusy(false, extractMessage(throwable)));
                    return null;
                });
    }

    @FXML
    private void handleRegister() {
        if (registerButton.isDisabled()) {
            return;
        }
        setBusy(true, "Dang tao tai khoan...");
        RegisterRequest request = new RegisterRequest(
                registerUsernameField.getText().trim(),
                registerPasswordField.getText(),
                registerDisplayNameField.getText().trim(),
                roleComboBox.getValue()
        );
        AppContext.service().register(request)
                .thenAccept(response -> Platform.runLater(() -> {
                    AppContext.state().setCurrentUser(response.user());
                    openDashboard();
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> setBusy(false, extractMessage(throwable)));
                    return null;
                });
    }

    private void setBusy(boolean busy, String message) {
        loginButton.setDisable(busy);
        registerButton.setDisable(busy);
        statusLabel.setText(message);
    }

    private void openDashboard() {
        try {
            setBusy(true, "Dang mo dashboard...");
            AppContext.showDashboardView();
        } catch (RuntimeException exception) {
            AppContext.state().setCurrentUser(null);
            setBusy(false, "Khong the mo man hinh chinh");
            AlertHelper.error(extractMessage(exception));
        }
    }

    private String extractMessage(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        return cause.getMessage() == null ? "Co loi xay ra" : cause.getMessage();
    }
}
