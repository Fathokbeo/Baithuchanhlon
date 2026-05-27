package main.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import main.client.AppContext;
import main.shared.dto.SessionUserDto;
import main.shared.util.MoneyUtils;

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
        balanceField.setText(MoneyUtils.display(user.balance()));
    }

    @FXML
    private void handleSave() {
        if (displayNameField.getText() == null || displayNameField.getText().isBlank()) {
            AlertHelper.error("Tên hiển thị không được để trống");
            return;
        }
        AlertHelper.info("Đã nhập thông tin user trên form");
    }

    @FXML
    private void handleBack() {
        AppContext.showDashboardView();
    }
}
