package main.client.controller;

import main.client.AppContext;
import main.shared.dto.AuctionDetailDto;
import main.shared.dto.AuctionDetailResponse;
import main.shared.dto.AuctionEventDto;
import main.shared.dto.AuctionSummaryDto;
import main.shared.dto.AutoBidDto;
import main.shared.dto.BidTransactionDto;
import main.shared.dto.ConfigureAutoBidRequest;
import main.shared.dto.PlaceBidRequest;
import main.shared.dto.SessionUserDto;
import main.shared.dto.UpdateAuctionStatusRequest;
import main.shared.dto.UpsertAuctionRequest;
import main.shared.dto.UserRowDto;
import main.shared.model.AuctionStatus;
import main.shared.model.ItemType;
import main.shared.model.Role;
import main.shared.util.MoneyUtils;
import main.shared.util.TimeUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

public final class DashboardController {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    private TabPane workspaceTabs;
    @FXML
    private Tab sellerTab;
    @FXML
    private Tab adminTab;
    @FXML
    private Label userLabel;
    @FXML
    private TableView<AuctionSummaryDto> auctionTable;
    @FXML
    private TableColumn<AuctionSummaryDto, String> auctionNameColumn;
    @FXML
    private TableColumn<AuctionSummaryDto, String> auctionStatusColumn;
    @FXML
    private TableColumn<AuctionSummaryDto, String> auctionPriceColumn;
    @FXML
    private TableColumn<AuctionSummaryDto, String> auctionEndColumn;
    @FXML
    private TableColumn<AuctionSummaryDto, String> auctionSellerColumn;
    @FXML
    private Label detailTitleLabel;
    @FXML
    private Label detailStatusLabel;
    @FXML
    private Label detailSellerLabel;
    @FXML
    private Label detailTimeLabel;
    @FXML
    private Label detailLeadingLabel;
    @FXML
    private Label detailCurrentPriceLabel;
    @FXML
    private Label detailItemInfoLabel;
    @FXML
    private Label detailExtensionLabel;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TableView<BidTransactionDto> bidHistoryTable;
    @FXML
    private TableColumn<BidTransactionDto, String> bidBidderColumn;
    @FXML
    private TableColumn<BidTransactionDto, String> bidAmountColumn;
    @FXML
    private TableColumn<BidTransactionDto, String> bidTimeColumn;
    @FXML
    private TableColumn<BidTransactionDto, String> bidSourceColumn;
    @FXML
    private TableView<AutoBidDto> autoBidTable;
    @FXML
    private TableColumn<AutoBidDto, String> autoBidderColumn;
    @FXML
    private TableColumn<AutoBidDto, String> autoMaxColumn;
    @FXML
    private TableColumn<AutoBidDto, String> autoIncrementColumn;
    @FXML
    private LineChart<String, Number> priceChart;
    @FXML
    private TextField bidAmountField;
    @FXML
    private TextField autoMaxBidField;
    @FXML
    private TextField autoIncrementField;
    @FXML
    private Button placeBidButton;
    @FXML
    private Button configureAutoBidButton;
    @FXML
    private TableView<AuctionSummaryDto> sellerAuctionTable;
    @FXML
    private TableColumn<AuctionSummaryDto, String> sellerNameColumn;
    @FXML
    private TableColumn<AuctionSummaryDto, String> sellerStatusColumn;
    @FXML
    private TableColumn<AuctionSummaryDto, String> sellerPriceColumn;
    @FXML
    private TableColumn<AuctionSummaryDto, String> sellerStartColumn;
    @FXML
    private Button addAuctionButton;
    @FXML
    private Button editAuctionButton;
    @FXML
    private Button deleteAuctionButton;
    @FXML
    private Button sellerCancelButton;
    @FXML
    private Button sellerMarkPaidButton;
    @FXML
    private TableView<UserRowDto> userTable;
    @FXML
    private TableColumn<UserRowDto, String> adminUsernameColumn;
    @FXML
    private TableColumn<UserRowDto, String> adminDisplayNameColumn;
    @FXML
    private TableColumn<UserRowDto, String> adminRoleColumn;
    @FXML
    private TableColumn<UserRowDto, String> adminCreatedColumn;
    @FXML
    private TableView<AuctionSummaryDto> adminAuctionTable;
    @FXML
    private TableColumn<AuctionSummaryDto, String> adminAuctionNameColumn;
    @FXML
    private TableColumn<AuctionSummaryDto, String> adminAuctionStatusColumn;
    @FXML
    private TableColumn<AuctionSummaryDto, String> adminAuctionPriceColumn;
    @FXML
    private TableColumn<AuctionSummaryDto, String> adminAuctionEndColumn;
    @FXML
    private Button adminCancelButton;
    @FXML
    private Button adminMarkPaidButton;

    private UUID selectedAuctionId;
    private Consumer<AuctionEventDto> eventListener;
    private boolean sellerDataLoaded;
    private boolean adminDataLoaded;

    @FXML
    private void initialize() {
        SessionUserDto user = AppContext.state().getCurrentUser();
        userLabel.setText(user.displayName() + " (" + user.role() + ")");
        setupAuctionTable();
        setupDetailTables();
        setupSellerTable();
        setupAdminTables();
        applyRoleVisibility(user.role());
        auctionTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadAuctionDetail(newValue.auctionId());
            }
        });
        eventListener = event -> Platform.runLater(() -> handleAuctionEvent(event));
        AppContext.connection().addAuctionListener(eventListener);
        workspaceTabs.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (newTab == sellerTab) {
                ensureSellerDataLoaded();
            } else if (newTab == adminTab) {
                ensureAdminDataLoaded();
            }
        });
        loadAuctionLists();
    }

    @FXML
    private void handleRefresh() {
        loadAuctionLists();
        if (AppContext.state().getCurrentUser().role() == Role.ADMIN && adminDataLoaded) {
            loadUsers();
        }
        if (AppContext.state().getCurrentUser().role() == Role.SELLER && sellerDataLoaded) {
            loadMyAuctions();
        }
        if (selectedAuctionId != null) {
            loadAuctionDetail(selectedAuctionId);
        }
    }

    @FXML
    private void handleLogout() {
        AppContext.connection().removeAuctionListener(eventListener);
        AppContext.state().setCurrentUser(null);
        AppContext.showLoginView();
    }

    @FXML
    private void handlePlaceBid() {
        if (selectedAuctionId == null) {
            AlertHelper.error("Hay chon mot phien dau gia");
            return;
        }
        runAction(AppContext.service().placeBid(new PlaceBidRequest(
                        selectedAuctionId,
                        parseAmount(bidAmountField.getText())
                )),
                response -> {
                    renderAuctionDetail(response.auction());
                    bidAmountField.clear();
                    AlertHelper.info("Dat gia thanh cong");
                });
    }

    @FXML
    private void handleConfigureAutoBid() {
        if (selectedAuctionId == null) {
            AlertHelper.error("Hay chon mot phien dau gia");
            return;
        }
        runAction(AppContext.service().configureAutoBid(new ConfigureAutoBidRequest(
                        selectedAuctionId,
                        parseAmount(autoMaxBidField.getText()),
                        parseAmount(autoIncrementField.getText())
                )),
                response -> {
                    renderAuctionDetail(response.auction());
                    autoMaxBidField.clear();
                    autoIncrementField.clear();
                    AlertHelper.info("Cap nhat auto-bid thanh cong");
                });
    }

    @FXML
    private void handleAddAuction() {
        showAuctionForm(null).ifPresent(request -> runAction(AppContext.service().saveAuction(request),
                response -> {
                    loadAuctionLists();
                    renderAuctionDetail(response.auction());
                    AlertHelper.info("Da tao phien dau gia");
                }));
    }

    @FXML
    private void handleEditAuction() {
        AuctionSummaryDto selected = sellerAuctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.error("Hay chon mot phien dau gia cua ban");
            return;
        }
        runAction(AppContext.service().getAuctionDetail(selected.auctionId()),
                response -> showAuctionForm(response.auction()).ifPresent(request -> runAction(
                        AppContext.service().saveAuction(request),
                        saved -> {
                            loadAuctionLists();
                            renderAuctionDetail(saved.auction());
                            AlertHelper.info("Da cap nhat phien dau gia");
                        })));
    }

    @FXML
    private void handleDeleteAuction() {
        AuctionSummaryDto selected = sellerAuctionTable.getSelectionModel().getSelectedItem();
        if (selected == null || !AlertHelper.confirm("Xoa phien dau gia da chon?")) {
            return;
        }
        runAction(AppContext.service().deleteAuction(selected.auctionId()), response -> {
            loadAuctionLists();
            clearDetail();
            AlertHelper.info(response.message());
        });
    }

    @FXML
    private void handleSellerCancel() {
        updateSelectedSellerAuction(AuctionStatus.CANCELED);
    }

    @FXML
    private void handleSellerMarkPaid() {
        updateSelectedSellerAuction(AuctionStatus.PAID);
    }

    @FXML
    private void handleAdminCancel() {
        updateSelectedAdminAuction(AuctionStatus.CANCELED);
    }

    @FXML
    private void handleAdminMarkPaid() {
        updateSelectedAdminAuction(AuctionStatus.PAID);
    }

    private void applyRoleVisibility(Role role) {
        if (role != Role.SELLER) {
            workspaceTabs.getTabs().remove(sellerTab);
        }
        if (role != Role.ADMIN) {
            workspaceTabs.getTabs().remove(adminTab);
        }
        boolean bidderControlsVisible = role == Role.BIDDER;
        placeBidButton.setDisable(!bidderControlsVisible);
        configureAutoBidButton.setDisable(!bidderControlsVisible);
        bidAmountField.setDisable(!bidderControlsVisible);
        autoMaxBidField.setDisable(!bidderControlsVisible);
        autoIncrementField.setDisable(!bidderControlsVisible);
    }

    private void setupAuctionTable() {
        auctionNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().itemName()));
        auctionStatusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status().name()));
        auctionPriceColumn.setCellValueFactory(data -> new SimpleStringProperty(MoneyUtils.display(data.getValue().currentPrice())));
        auctionEndColumn.setCellValueFactory(data -> new SimpleStringProperty(TimeUtils.display(data.getValue().endTime())));
        auctionSellerColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().sellerName()));
    }

    private void setupDetailTables() {
        descriptionArea.setEditable(false);
        bidBidderColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().bidderName()));
        bidAmountColumn.setCellValueFactory(data -> new SimpleStringProperty(MoneyUtils.display(data.getValue().amount())));
        bidTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(TimeUtils.display(data.getValue().timestamp())));
        bidSourceColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().source().name()));

        autoBidderColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().bidderName()));
        autoMaxColumn.setCellValueFactory(data -> new SimpleStringProperty(MoneyUtils.display(data.getValue().maxBid())));
        autoIncrementColumn.setCellValueFactory(data -> new SimpleStringProperty(MoneyUtils.display(data.getValue().increment())));
    }

    private void setupSellerTable() {
        sellerNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().itemName()));
        sellerStatusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status().name()));
        sellerPriceColumn.setCellValueFactory(data -> new SimpleStringProperty(MoneyUtils.display(data.getValue().currentPrice())));
        sellerStartColumn.setCellValueFactory(data -> new SimpleStringProperty(TimeUtils.display(data.getValue().startTime())));
    }

    private void setupAdminTables() {
        adminUsernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username()));
        adminDisplayNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().displayName()));
        adminRoleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().role().name()));
        adminCreatedColumn.setCellValueFactory(data -> new SimpleStringProperty(TimeUtils.display(data.getValue().createdAt())));

        adminAuctionNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().itemName()));
        adminAuctionStatusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status().name()));
        adminAuctionPriceColumn.setCellValueFactory(data -> new SimpleStringProperty(MoneyUtils.display(data.getValue().currentPrice())));
        adminAuctionEndColumn.setCellValueFactory(data -> new SimpleStringProperty(TimeUtils.display(data.getValue().endTime())));
    }

    private void loadAuctionLists() {
        runAction(AppContext.service().listAuctions(), response -> {
            auctionTable.setItems(FXCollections.observableArrayList(response.auctions()));
            if (AppContext.state().getCurrentUser().role() == Role.SELLER && sellerDataLoaded) {
                loadMyAuctions();
            }
            if (AppContext.state().getCurrentUser().role() == Role.ADMIN && adminDataLoaded) {
                adminAuctionTable.setItems(FXCollections.observableArrayList(response.auctions()));
            }
        });
    }

    private void loadMyAuctions() {
        runAction(AppContext.service().listMyAuctions(),
                response -> sellerAuctionTable.setItems(FXCollections.observableArrayList(response.auctions())));
    }

    private void loadUsers() {
        runAction(AppContext.service().listUsers(),
                response -> userTable.setItems(FXCollections.observableArrayList(response.users())));
    }

    private void ensureSellerDataLoaded() {
        if (!sellerDataLoaded) {
            sellerDataLoaded = true;
            loadMyAuctions();
        }
    }

    private void ensureAdminDataLoaded() {
        if (!adminDataLoaded) {
            adminDataLoaded = true;
            loadUsers();
            runAction(AppContext.service().listAuctions(),
                    response -> adminAuctionTable.setItems(FXCollections.observableArrayList(response.auctions())));
        }
    }

    private void loadAuctionDetail(UUID auctionId) {
        selectedAuctionId = auctionId;
        runAction(AppContext.service().getAuctionDetail(auctionId), response -> renderAuctionDetail(response.auction()));
    }

    private void renderAuctionDetail(AuctionDetailDto auction) {
        selectedAuctionId = auction.auctionId();
        detailTitleLabel.setText(auction.itemName());
        detailStatusLabel.setText(auction.status().name());
        detailSellerLabel.setText(auction.sellerName());
        detailTimeLabel.setText(TimeUtils.display(auction.startTime()) + " -> " + TimeUtils.display(auction.endTime()));
        detailLeadingLabel.setText(auction.leadingBidderName() == null ? "-" : auction.leadingBidderName());
        detailCurrentPriceLabel.setText(MoneyUtils.display(auction.currentPrice()));
        detailItemInfoLabel.setText(auction.itemInfo());
        detailExtensionLabel.setText("Gia han: " + auction.extensionCount() + " lan");
        descriptionArea.setText(auction.description() == null ? "" : auction.description());
        bidHistoryTable.setItems(FXCollections.observableArrayList(
                auction.bidHistory() == null ? java.util.List.of() : auction.bidHistory()
        ));
        autoBidTable.setItems(FXCollections.observableArrayList(
                auction.autoBids() == null ? java.util.List.of() : auction.autoBids()
        ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (auction.priceHistory() != null) {
            auction.priceHistory().forEach(point -> series.getData().add(
                    new XYChart.Data<>(TimeUtils.chartLabel(point.timestamp()), point.amount())
            ));
        }
        priceChart.getData().clear();
        priceChart.getData().add(series);
    }

    private void clearDetail() {
        selectedAuctionId = null;
        detailTitleLabel.setText("Chua chon phien dau gia");
        detailStatusLabel.setText("-");
        detailSellerLabel.setText("-");
        detailTimeLabel.setText("-");
        detailLeadingLabel.setText("-");
        detailCurrentPriceLabel.setText("-");
        detailItemInfoLabel.setText("-");
        detailExtensionLabel.setText("Gia han: 0 lan");
        descriptionArea.clear();
        bidHistoryTable.setItems(FXCollections.emptyObservableList());
        autoBidTable.setItems(FXCollections.emptyObservableList());
        priceChart.getData().clear();
    }

    private void handleAuctionEvent(AuctionEventDto event) {
        loadAuctionLists();
        if (event.detail() != null && event.detail().auctionId().equals(selectedAuctionId)) {
            renderAuctionDetail(event.detail());
        }
        if ("DELETED".equals(event.eventName())
                && event.summary() != null
                && event.summary().auctionId().equals(selectedAuctionId)) {
            clearDetail();
        }
    }

    private void updateSelectedSellerAuction(AuctionStatus status) {
        AuctionSummaryDto selected = sellerAuctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.error("Hay chon mot phien dau gia cua ban");
            return;
        }
        runAction(AppContext.service().updateAuctionStatus(new UpdateAuctionStatusRequest(selected.auctionId(), status)),
                response -> {
                    renderAuctionDetail(response.auction());
                    loadAuctionLists();
                    AlertHelper.info("Cap nhat trang thai thanh cong");
                });
    }

    private void updateSelectedAdminAuction(AuctionStatus status) {
        AuctionSummaryDto selected = adminAuctionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.error("Hay chon mot phien dau gia");
            return;
        }
        runAction(AppContext.service().updateAuctionStatus(new UpdateAuctionStatusRequest(selected.auctionId(), status)),
                response -> {
                    renderAuctionDetail(response.auction());
                    loadAuctionLists();
                    AlertHelper.info("Cap nhat trang thai thanh cong");
                });
    }

    private Optional<UpsertAuctionRequest> showAuctionForm(AuctionDetailDto auction) {
        Dialog<UpsertAuctionRequest> dialog = new Dialog<>();
        dialog.setTitle(auction == null ? "Tao phien dau gia" : "Sua phien dau gia");
        ButtonType saveButtonType = new ButtonType("Luu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        ComboBox<ItemType> typeComboBox = new ComboBox<>(FXCollections.observableArrayList(ItemType.values()));
        TextField nameField = new TextField();
        TextField priceField = new TextField();
        TextArea descriptionField = new TextArea();
        TextField specialField = new TextField();
        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        TextField startTimeField = new TextField("20:00:00");
        DatePicker endDatePicker = new DatePicker(LocalDate.now().plusDays(1));
        TextField endTimeField = new TextField("20:30:00");

        typeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ItemType object) {
                return object == null ? "" : object.name();
            }

            @Override
            public ItemType fromString(String string) {
                return ItemType.valueOf(string);
            }
        });

        if (auction != null) {
            typeComboBox.setValue(auction.itemType());
            typeComboBox.setDisable(true);
            nameField.setText(auction.itemName());
            priceField.setText(auction.startingPrice().stripTrailingZeros().toPlainString());
            descriptionField.setText(auction.description());
            specialField.setText(auction.itemInfo().replaceFirst("^[^|]+\\s\\|\\s", ""));
            startDatePicker.setValue(auction.startTime().toLocalDate());
            startTimeField.setText(auction.startTime().toLocalTime().format(TIME_FORMAT));
            endDatePicker.setValue(auction.endTime().toLocalDate());
            endTimeField.setText(auction.endTime().toLocalTime().format(TIME_FORMAT));
        } else {
            typeComboBox.setValue(ItemType.ELECTRONICS);
        }

        GridPane gridPane = new GridPane();
        gridPane.setHgap(12);
        gridPane.setVgap(12);
        gridPane.add(new Label("Loai"), 0, 0);
        gridPane.add(typeComboBox, 1, 0);
        gridPane.add(new Label("Ten"), 0, 1);
        gridPane.add(nameField, 1, 1);
        gridPane.add(new Label("Gia khoi diem"), 0, 2);
        gridPane.add(priceField, 1, 2);
        gridPane.add(new Label("Bat dau"), 0, 3);
        gridPane.add(startDatePicker, 1, 3);
        gridPane.add(new Label("Gio bat dau"), 0, 4);
        gridPane.add(startTimeField, 1, 4);
        gridPane.add(new Label("Ket thuc"), 0, 5);
        gridPane.add(endDatePicker, 1, 5);
        gridPane.add(new Label("Gio ket thuc"), 0, 6);
        gridPane.add(endTimeField, 1, 6);
        gridPane.add(new Label("Thong so them"), 0, 7);
        gridPane.add(specialField, 1, 7);
        gridPane.add(new Label("Mo ta"), 0, 8);
        gridPane.add(descriptionField, 1, 8);
        dialog.getDialogPane().setContent(gridPane);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }
            LocalDateTime startTime = LocalDateTime.of(startDatePicker.getValue(), LocalTime.parse(startTimeField.getText().trim()));
            LocalDateTime endTime = LocalDateTime.of(endDatePicker.getValue(), LocalTime.parse(endTimeField.getText().trim()));
            return new UpsertAuctionRequest(
                    auction == null ? null : auction.auctionId(),
                    typeComboBox.getValue(),
                    nameField.getText().trim(),
                    descriptionField.getText().trim(),
                    parseAmount(priceField.getText()),
                    startTime,
                    endTime,
                    specialField.getText().trim()
            );
        });
        return dialog.showAndWait();
    }

    private <T> void runAction(java.util.concurrent.CompletableFuture<T> future, Consumer<T> onSuccess) {
        future.thenAccept(result -> Platform.runLater(() -> onSuccess.accept(result)))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> AlertHelper.error(extractMessage(throwable)));
                    return null;
                });
    }

    private String extractMessage(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        return cause.getMessage() == null ? "Co loi xay ra" : cause.getMessage();
    }

    private BigDecimal parseAmount(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().replace(".", "").replace(",", "");
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Vui long nhap so tien hop le");
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Vui long nhap so tien hop le");
        }
    }
}
