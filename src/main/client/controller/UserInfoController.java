package main.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import main.client.AppContext;
import main.shared.dto.SessionUserDto;
import main.shared.dto.UpdateUserInfoRequest;

import java.math.BigDecimal;
import java.util.concurrent.CompletionException;

public final class UserInfoController {
    @FXML
    private TextField usernameField;
    @FXML
    private TextField displayNameField;
    @FXML
    private TextField roleField;
    @FXML
    private TextField balanceField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextArea addressField;

    @FXML
    private void initialize() {
        SessionUserDto user = AppContext.state().getCurrentUser();
        if (user == null) {
            AppContext.showLoginView();
            return;
        }
        usernameField.setText(user.username());
        displayNameField.setText(user.displayName());
        roleField.setText(user.role().name());
        balanceField.setText(user.balance().stripTrailingZeros().toPlainString());
        emailField.setText(user.email());
        phoneField.setText(user.phone());
        addressField.setText(user.address());
    }

    @FXML
    private void handleSave() {
        if (displayNameField.getText() == null || displayNameField.getText().isBlank()) {
            AlertHelper.error("Tên hiển thị không được để trống");
            return;
        }
        BigDecimal balance;
        try {
            balance = parseAmount(balanceField.getText());
        } catch (IllegalArgumentException exception) {
            AlertHelper.error(exception.getMessage());
            return;
        }
        AppContext.service().updateUserInfo(new UpdateUserInfoRequest(
                        displayNameField.getText().trim(),
                        balance,
                        emailField.getText(),
                        phoneField.getText(),
                        addressField.getText()
                ))
                .thenAccept(response -> Platform.runLater(() -> {
                    AppContext.state().setCurrentUser(response.user());
                    displayNameField.setText(response.user().displayName());
                    balanceField.setText(response.user().balance().stripTrailingZeros().toPlainString());
                    emailField.setText(response.user().email());
                    phoneField.setText(response.user().phone());
                    addressField.setText(response.user().address());
                    AlertHelper.info(response.message());
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> AlertHelper.error(extractMessage(throwable)));
                    return null;
                });
    }

    @FXML
    private void handleBack() {
        AppContext.showDashboardView();
    }

    private BigDecimal parseAmount(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().replace(".", "").replace(",", "");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Vui long nhap so du");
        }
        try {
            BigDecimal amount = new BigDecimal(normalized);
            if (amount.signum() < 0) {
                throw new IllegalArgumentException("So du khong duoc am");
            }
            return amount;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("So du khong hop le");
        }
    }

    private String extractMessage(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        return cause.getMessage() == null ? "Co loi xay ra" : cause.getMessage();
    }
}
