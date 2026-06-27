package com.lerakindletransfer.controller;

import com.lerakindletransfer.model.AppConfig;
import com.lerakindletransfer.model.Credentials;
import com.lerakindletransfer.model.SecureInitResult;
import com.lerakindletransfer.model.TransferResult;
import com.lerakindletransfer.service.ConfigService;
import com.lerakindletransfer.service.LogService;
import com.lerakindletransfer.service.ProgressCallback;
import com.lerakindletransfer.service.SecureConnectionService;
import com.lerakindletransfer.service.SftpTransferService;
import com.lerakindletransfer.util.FileTypeValidator;
import com.lerakindletransfer.util.MacPaths;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainController {
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ConfigService configService;
    private final SftpTransferService transferService;
    private final SecureConnectionService secureConnectionService;
    private final LogService logService;
    private final ObservableList<File> selectedFiles = FXCollections.observableArrayList();
    private final BooleanProperty working = new SimpleBooleanProperty(false);

    private AppConfig config;
    private boolean secureKeyAvailable;
    private VBox rootPane;
    private GridPane connectionGrid;
    private TextField hostField;
    private CheckBox advancedConnectionCheckBox;
    private Label secureModeLabel;
    private TextField portField;
    private TextField usernameField;
    private TextField remoteFolderField;
    private ComboBox<AppConfig.AuthMode> authModeCombo;
    private PasswordField passwordField;
    private TextField privateKeyField;
    private PasswordField passphraseField;
    private Label passwordLabel;
    private Label privateKeyLabel;
    private Label passphraseLabel;
    private HBox privateKeyBox;
    private Label noPasswordWarning;
    private Label selectedCountLabel;
    private Button testConnectionButton;
    private Button initSecureConnectionButton;
    private Button openLogsButton;
    private Button openConfigButton;
    private Button selectBooksButton;
    private Button removeSelectedButton;
    private Button clearListButton;
    private Button transferButton;
    private ProgressBar progressBar;
    private Label statusLabel;
    private TextArea logArea;
    private ListView<File> fileListView;

    public MainController(ConfigService configService, SftpTransferService transferService,
                          SecureConnectionService secureConnectionService, LogService logService) {
        this.configService = configService;
        this.transferService = transferService;
        this.secureConnectionService = secureConnectionService;
        this.logService = logService;
    }

    public Parent createView() {
        config = configService.load();
        applyDetectedSecureKeyToConfig();

        rootPane = new VBox(16);
        rootPane.getStyleClass().add("root-pane");
        rootPane.setPadding(new Insets(18));

        rootPane.getChildren().addAll(
                buildAppHeader(),
                buildConnectionSection(),
                buildFileSection(),
                buildTransferSection()
        );

        ScrollPane scrollPane = new ScrollPane(rootPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        configureDragAndDrop(rootPane);
        updateAdvancedConnectionVisibility();
        updateAuthModeView();
        appendUserLog(secureKeyAvailable ? "Secure key found. Simple connection view is enabled." : "Ready.");
        return scrollPane;
    }

    private HBox buildAppHeader() {
        Label badge = new Label("LT");
        badge.getStyleClass().add("app-badge");

        Label title = new Label("Lera Kindle Transfer");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("KOReader book transfer");
        subtitle.getStyleClass().add("app-subtitle");

        VBox text = new VBox(2, title, subtitle);
        HBox header = new HBox(12, badge, text);
        header.getStyleClass().add("app-header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private void applyDetectedSecureKeyToConfig() {
        Path detectedKeyPath = detectedPrivateKeyPath();
        if (detectedKeyPath == null) {
            secureKeyAvailable = false;
            return;
        }
        secureKeyAvailable = true;
        config.setUsername("root");
        config.setAuthMode(AppConfig.AuthMode.PRIVATE_KEY);
        config.setPrivateKeyPath(detectedKeyPath.toString());
    }

    private Path detectedPrivateKeyPath() {
        String configuredPath = config.getPrivateKeyPath();
        if (!configuredPath.isBlank()) {
            Path path = Path.of(configuredPath);
            if (Files.isRegularFile(path)) {
                return path;
            }
        }
        Path generatedKeyPath = MacPaths.privateKeyFile();
        if (Files.isRegularFile(generatedKeyPath)) {
            return generatedKeyPath;
        }
        return null;
    }

    private VBox buildConnectionSection() {
        advancedConnectionCheckBox = new CheckBox("Advanced connection");
        advancedConnectionCheckBox.setSelected(!secureKeyAvailable);
        advancedConnectionCheckBox.getStyleClass().add("advanced-toggle");
        advancedConnectionCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> updateAdvancedConnectionVisibility());
        setVisibleManaged(advancedConnectionCheckBox, secureKeyAvailable);
        secureModeLabel = new Label("Private key ready");
        secureModeLabel.getStyleClass().add("secure-pill");
        HBox headerTools = new HBox(10, secureModeLabel, advancedConnectionCheckBox);
        headerTools.setAlignment(Pos.CENTER_RIGHT);
        HBox sectionHeader = buildSectionHeader("1", "Kindle", headerTools);

        connectionGrid = new GridPane();
        connectionGrid.getStyleClass().add("form-grid");
        connectionGrid.setHgap(10);
        connectionGrid.setVgap(10);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(130);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        connectionGrid.getColumnConstraints().addAll(labels, fields);

        hostField = new TextField(config.getHost());
        hostField.setPromptText("Kindle IP address");
        hostField.getStyleClass().add("large-field");
        portField = new TextField(Integer.toString(config.getPort()));
        usernameField = new TextField(config.getUsername());
        remoteFolderField = new TextField(config.getRemoteBooksPath());

        authModeCombo = new ComboBox<>();
        authModeCombo.getItems().setAll(AppConfig.AuthMode.PASSWORD, AppConfig.AuthMode.PRIVATE_KEY, AppConfig.AuthMode.NO_PASSWORD);
        authModeCombo.setValue(config.getAuthMode());
        authModeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(AppConfig.AuthMode mode) {
                if (mode == null) {
                    return "";
                }
                return switch (mode) {
                    case PASSWORD -> "Password";
                    case PRIVATE_KEY -> "Private key";
                    case NO_PASSWORD -> "No password";
                };
            }

            @Override
            public AppConfig.AuthMode fromString(String string) {
                return AppConfig.AuthMode.fromString(string);
            }
        });
        authModeCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            resetTestConnectionButton();
            updateAuthModeView();
        });

        passwordField = new PasswordField();
        passwordField.setPromptText("Asked at runtime, never saved");
        passwordLabel = new Label("Password");

        privateKeyField = new TextField(config.getPrivateKeyPath());
        privateKeyField.setPromptText("Private key path");
        Button chooseKeyButton = new Button("Choose...");
        chooseKeyButton.setOnAction(event -> choosePrivateKey());
        privateKeyBox = new HBox(8, privateKeyField, chooseKeyButton);
        HBox.setHgrow(privateKeyField, Priority.ALWAYS);
        privateKeyLabel = new Label("Private key");

        passphraseField = new PasswordField();
        passphraseField.setPromptText("Optional, never saved");
        passphraseLabel = new Label("Passphrase");

        noPasswordWarning = new Label("Use this only on trusted home Wi-Fi and only while transferring books. KOReader accepts root/root in this mode.");
        noPasswordWarning.getStyleClass().add("warning-label");
        noPasswordWarning.setWrapText(true);

        testConnectionButton = new Button("Test Connection");
        testConnectionButton.getStyleClass().add("secondary-button");
        testConnectionButton.setOnAction(event -> testConnection());
        initSecureConnectionButton = new Button("Init secure connection");
        initSecureConnectionButton.getStyleClass().add("secondary-button");
        initSecureConnectionButton.setOnAction(event -> initSecureConnection());
        openLogsButton = new Button("Open logs folder");
        openLogsButton.getStyleClass().add("subtle-button");
        openLogsButton.setOnAction(event -> openFolder(MacPaths.logsDirectory()));
        openConfigButton = new Button("Open config folder");
        openConfigButton.getStyleClass().add("subtle-button");
        openConfigButton.setOnAction(event -> openFolder(MacPaths.configDirectory()));
        HBox actions = new HBox(8, testConnectionButton, initSecureConnectionButton, openLogsButton, openConfigButton);
        actions.getStyleClass().addAll("button-row", "connection-actions");
        actions.setAlignment(Pos.CENTER_LEFT);

        bindDisabled(hostField, portField, usernameField, remoteFolderField, authModeCombo,
                passwordField, privateKeyField, chooseKeyButton, passphraseField, testConnectionButton,
                initSecureConnectionButton, advancedConnectionCheckBox);
        watchConnectionInputs();
        rebuildConnectionGrid();

        return sectionBox(sectionHeader, connectionGrid, actions);
    }

    private VBox buildFileSection() {
        selectedCountLabel = new Label();
        selectedCountLabel.getStyleClass().add("count-label");
        updateSelectedCount();
        selectedFiles.addListener((javafx.collections.ListChangeListener<File>) change -> updateSelectedCount());
        HBox sectionHeader = buildSectionHeader("2", "Books", selectedCountLabel);

        fileListView = new ListView<>(selectedFiles);
        fileListView.setPrefHeight(180);
        fileListView.setPlaceholder(new Label("Drop books here"));
        fileListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - " + item.getParent());
                }
            }
        });
        fileListView.getStyleClass().add("drop-pane");
        configureDragAndDrop(fileListView);

        selectBooksButton = new Button("Select books");
        selectBooksButton.getStyleClass().add("secondary-button");
        selectBooksButton.setOnAction(event -> selectBooks());
        removeSelectedButton = new Button("Remove selected");
        removeSelectedButton.getStyleClass().add("subtle-button");
        removeSelectedButton.setOnAction(event -> removeSelectedFile());
        clearListButton = new Button("Clear list");
        clearListButton.getStyleClass().add("subtle-button");
        clearListButton.setOnAction(event -> selectedFiles.clear());

        removeSelectedButton.disableProperty().bind(working.or(fileListView.getSelectionModel().selectedItemProperty().isNull()));
        clearListButton.disableProperty().bind(working.or(Bindings.isEmpty(selectedFiles)));
        bindDisabled(selectBooksButton);

        HBox actions = new HBox(8, selectBooksButton, removeSelectedButton, clearListButton);
        actions.getStyleClass().add("button-row");
        actions.setAlignment(Pos.CENTER_LEFT);
        return sectionBox(sectionHeader, actions, fileListView);
    }

    private VBox buildTransferSection() {
        HBox sectionHeader = buildSectionHeader("3", "Transfer", null);

        transferButton = new Button("Transfer to Kindle");
        transferButton.getStyleClass().add("primary-button");
        transferButton.setDefaultButton(true);
        transferButton.setOnAction(event -> transferToKindle());
        transferButton.disableProperty().bind(working.or(Bindings.isEmpty(selectedFiles)));

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        HBox transferRow = new HBox(10, transferButton, progressBar);
        transferRow.getStyleClass().add("transfer-row");
        transferRow.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Select books to begin.");
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setWrapText(true);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefRowCount(9);
        logArea.getStyleClass().add("log-area");

        return sectionBox(sectionHeader, transferRow, statusLabel, logArea);
    }

    private HBox buildSectionHeader(String stepNumber, String titleText, Node trailing) {
        Label step = new Label(stepNumber);
        step.getStyleClass().add("step-badge");
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(9, step, title, spacer);
        if (trailing != null) {
            header.getChildren().add(trailing);
        }
        header.getStyleClass().add("section-header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private VBox sectionBox(Node... children) {
        VBox box = new VBox(8);
        box.getStyleClass().add("section-box");
        box.getChildren().addAll(children);
        return box;
    }

    private void updateSelectedCount() {
        if (selectedCountLabel == null) {
            return;
        }
        int count = selectedFiles.size();
        selectedCountLabel.setText(count == 1 ? "1 selected" : count + " selected");
    }

    private Label addRow(GridPane grid, int row, String labelText, Node field) {
        Label label = new Label(labelText);
        addStyleClass(label, "form-label");
        setVisibleManaged(field, true);
        grid.add(label, 0, row);
        grid.add(field, 1, row);
        if (field instanceof TextField textField) {
            textField.setMaxWidth(Double.MAX_VALUE);
        } else if (field instanceof ComboBox<?> comboBox) {
            comboBox.setMaxWidth(Double.MAX_VALUE);
        }
        return label;
    }

    private void addExistingRow(GridPane grid, int row, Label label, Node field) {
        addStyleClass(label, "form-label");
        setVisibleManaged(label, true);
        setVisibleManaged(field, true);
        grid.add(label, 0, row);
        grid.add(field, 1, row);
    }

    private void addStyleClass(Node node, String styleClass) {
        if (!node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }

    private void bindDisabled(Node... nodes) {
        for (Node node : nodes) {
            node.disableProperty().bind(working);
        }
    }

    private void updateAuthModeView() {
        if (authModeCombo == null || connectionGrid == null) {
            return;
        }
        rebuildConnectionGrid();
    }

    private void updateAdvancedConnectionVisibility() {
        if (portField == null) {
            return;
        }
        boolean advanced = isAdvancedConnectionVisible();
        setVisibleManaged(secureModeLabel, secureKeyAvailable && !advanced);
        setVisibleManaged(initSecureConnectionButton, advanced);
        setVisibleManaged(openLogsButton, advanced);
        setVisibleManaged(openConfigButton, advanced);
        rebuildConnectionGrid();
        updateAuthModeView();
    }

    private boolean isAdvancedConnectionVisible() {
        return !secureKeyAvailable || advancedConnectionCheckBox == null || advancedConnectionCheckBox.isSelected();
    }

    private void rebuildConnectionGrid() {
        if (connectionGrid == null || hostField == null) {
            return;
        }
        connectionGrid.getChildren().clear();
        boolean advanced = isAdvancedConnectionVisible();
        int row = 0;
        addRow(connectionGrid, row++, "Host/IP", hostField);
        if (advanced) {
            addRow(connectionGrid, row++, "Port", portField);
            addRow(connectionGrid, row++, "Username", usernameField);
        }
        addRow(connectionGrid, row++, "Remote folder", remoteFolderField);
        if (!advanced) {
            return;
        }

        addRow(connectionGrid, row++, "Authentication", authModeCombo);
        AppConfig.AuthMode mode = authModeCombo.getValue();
        if (mode == AppConfig.AuthMode.PASSWORD) {
            addExistingRow(connectionGrid, row++, passwordLabel, passwordField);
        } else if (mode == AppConfig.AuthMode.PRIVATE_KEY) {
            addExistingRow(connectionGrid, row++, privateKeyLabel, privateKeyBox);
            addExistingRow(connectionGrid, row++, passphraseLabel, passphraseField);
        } else if (mode == AppConfig.AuthMode.NO_PASSWORD) {
            setVisibleManaged(noPasswordWarning, true);
            connectionGrid.add(new Region(), 0, row);
            connectionGrid.add(noPasswordWarning, 1, row);
        }
    }

    private void watchConnectionInputs() {
        hostField.textProperty().addListener((observable, oldValue, newValue) -> resetTestConnectionButton());
        portField.textProperty().addListener((observable, oldValue, newValue) -> resetTestConnectionButton());
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> resetTestConnectionButton());
        remoteFolderField.textProperty().addListener((observable, oldValue, newValue) -> resetTestConnectionButton());
        privateKeyField.textProperty().addListener((observable, oldValue, newValue) -> resetTestConnectionButton());
        passphraseField.textProperty().addListener((observable, oldValue, newValue) -> resetTestConnectionButton());
    }

    private void resetTestConnectionButton() {
        if (testConnectionButton == null) {
            return;
        }
        testConnectionButton.getStyleClass().removeAll(List.of("connection-success-button", "connection-failed-button", "connection-testing-button"));
        testConnectionButton.setText("Test Connection");
    }

    private void markTestConnectionTesting() {
        testConnectionButton.getStyleClass().removeAll(List.of("connection-success-button", "connection-failed-button"));
        if (!testConnectionButton.getStyleClass().contains("connection-testing-button")) {
            testConnectionButton.getStyleClass().add("connection-testing-button");
        }
        testConnectionButton.setText("Testing...");
    }

    private void markTestConnectionSuccess() {
        testConnectionButton.getStyleClass().removeAll(List.of("connection-testing-button", "connection-failed-button"));
        if (!testConnectionButton.getStyleClass().contains("connection-success-button")) {
            testConnectionButton.getStyleClass().add("connection-success-button");
        }
        testConnectionButton.setText("Connected");
    }

    private void markTestConnectionFailed() {
        testConnectionButton.getStyleClass().removeAll(List.of("connection-testing-button", "connection-success-button"));
        if (!testConnectionButton.getStyleClass().contains("connection-failed-button")) {
            testConnectionButton.getStyleClass().add("connection-failed-button");
        }
        testConnectionButton.setText("Retry connection");
    }

    private void setVisibleManaged(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void choosePrivateKey() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose private key");
        File initial = initialDirectoryFromPath(privateKeyField.getText());
        if (initial != null) {
            chooser.setInitialDirectory(initial);
        }
        File file = chooser.showOpenDialog(window());
        if (file != null) {
            privateKeyField.setText(file.getAbsolutePath());
            saveCurrentConfigQuietly();
        }
    }

    private File initialDirectoryFromPath(String pathText) {
        if (pathText == null || pathText.isBlank()) {
            return null;
        }
        File file = new File(pathText);
        File directory = file.isDirectory() ? file : file.getParentFile();
        return directory != null && directory.isDirectory() ? directory : null;
    }

    private void selectBooks() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select books");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Ebook files", FileTypeValidator.acceptedGlobPatterns()));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));

        File lastFolder = config.getLastLocalFolder().isBlank() ? null : new File(config.getLastLocalFolder());
        if (lastFolder != null && lastFolder.isDirectory()) {
            chooser.setInitialDirectory(lastFolder);
        }

        List<File> files = chooser.showOpenMultipleDialog(window());
        if (files != null) {
            addFiles(files);
        }
    }

    private void configureDragAndDrop(Node node) {
        node.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        node.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean completed = false;
            if (dragboard.hasFiles()) {
                addFiles(dragboard.getFiles());
                completed = true;
            }
            event.setDropCompleted(completed);
            event.consume();
        });
    }

    private void addFiles(List<File> files) {
        List<String> rejected = new ArrayList<>();
        int added = 0;
        for (File file : files) {
            if (file == null) {
                continue;
            }
            if (!file.isFile()) {
                rejected.add(file.getName() + " is not a regular file.");
                continue;
            }
            if (!FileTypeValidator.isAccepted(file.toPath())) {
                rejected.add(file.getName() + " is not a supported ebook format.");
                continue;
            }
            boolean alreadySelected = selectedFiles.stream()
                    .anyMatch(existing -> existing.getAbsolutePath().equals(file.getAbsolutePath()));
            if (!alreadySelected) {
                selectedFiles.add(file);
                added++;
            }
        }

        if (added > 0) {
            File parent = files.stream()
                    .filter(File::isFile)
                    .findFirst()
                    .map(File::getParentFile)
                    .orElse(null);
            if (parent != null) {
                config.setLastLocalFolder(parent.getAbsolutePath());
                saveCurrentConfigQuietly();
            }
            statusLabel.setText(added == 1 ? "Added 1 book." : "Added " + added + " books.");
            appendUserLog(added == 1 ? "Added 1 book." : "Added " + added + " books.");
        }

        if (!rejected.isEmpty()) {
            String message = "Rejected " + rejected.size() + " file(s). Supported: " + FileTypeValidator.acceptedExtensionsDescription() + ".";
            statusLabel.setText(message);
            appendUserLog(message);
            rejected.forEach(reason -> appendUserLog("Rejected: " + reason));
            showAlert(Alert.AlertType.INFORMATION, "Unsupported file type", message);
        }
    }

    private void removeSelectedFile() {
        File selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedFiles.remove(selected);
            appendUserLog("Removed from selection: " + selected.getName());
        }
    }

    private void testConnection() {
        AppConfig currentConfig;
        Credentials credentials;
        try {
            currentConfig = buildConfigFromFields();
            credentials = buildCredentials();
        } catch (UserInputException ex) {
            statusLabel.setText(ex.getMessage());
            showAlert(Alert.AlertType.WARNING, "Check connection settings", ex.getMessage());
            return;
        }

        saveConfig(currentConfig);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setText("Testing connection...");
        markTestConnectionTesting();
        appendUserLog("Testing connection to " + currentConfig.getHost() + ":" + currentConfig.getPort() + ".");
        working.set(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                transferService.testConnection(currentConfig, credentials);
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            working.set(false);
            progressBar.setProgress(0);
            markTestConnectionSuccess();
            statusLabel.setText("Connection successful.");
            appendUserLog("Connection successful.");
        });
        task.setOnFailed(event -> {
            working.set(false);
            progressBar.setProgress(0);
            Throwable error = task.getException();
            logService.error("Connection test failed", error);
            String message = friendlyConnectionMessage(error);
            markTestConnectionFailed();
            statusLabel.setText(message);
            appendUserLog("Connection failed. " + firstLine(message));
            showAlert(Alert.AlertType.ERROR, "Connection failed", message);
        });
        new Thread(task, "connection-test").start();
    }

    private void initSecureConnection() {
        AppConfig currentConfig;
        try {
            currentConfig = buildConfigFromFields();
        } catch (UserInputException ex) {
            statusLabel.setText(ex.getMessage());
            showAlert(Alert.AlertType.WARNING, "Check Kindle IP", ex.getMessage());
            return;
        }

        currentConfig.setUsername("root");
        if (!confirmSecureInit(currentConfig)) {
            return;
        }

        saveConfig(currentConfig);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setText("Initializing secure connection...");
        appendUserLog("Initializing secure connection for " + currentConfig.getHost() + ":" + currentConfig.getPort() + ".");
        working.set(true);

        Task<SecureInitResult> task = new Task<>() {
            @Override
            protected SecureInitResult call() throws Exception {
                return secureConnectionService.installPublicKey(currentConfig, message -> Platform.runLater(() -> {
                    statusLabel.setText(message);
                    appendUserLog(message);
                }));
            }
        };
        task.setOnSucceeded(event -> {
            working.set(false);
            SecureInitResult result = task.getValue();
            progressBar.setProgress(0);
            privateKeyField.setText(result.privateKeyPath().toString());
            usernameField.setText("root");
            authModeCombo.setValue(AppConfig.AuthMode.PRIVATE_KEY);
            passwordField.clear();
            passphraseField.clear();
            saveConfigForSecureKey(currentConfig, result.privateKeyPath());

            String installedMessage = result.keyAlreadyPresent()
                    ? "This laptop key was already installed on KOReader."
                    : "This laptop key was installed on KOReader.";
            appendUserLog(installedMessage + " Path: " + result.authorizedKeysPath());

            if (confirmRestartThenTest()) {
                testSecureKeyConnection(currentConfig, result.privateKeyPath());
            } else {
                String message = "Key installed. Restart the KOReader SSH server with passwordless login disabled, then use Test Connection.";
                statusLabel.setText(message);
                appendUserLog(message);
            }
        });
        task.setOnFailed(event -> {
            working.set(false);
            progressBar.setProgress(0);
            Throwable error = task.getException();
            logService.error("Secure connection initialization failed", error);
            String message = friendlyConnectionMessage(error);
            statusLabel.setText(message);
            appendUserLog("Secure init failed. " + firstLine(message));
            showAlert(Alert.AlertType.ERROR, "Secure init failed", message);
        });
        new Thread(task, "secure-connection-init").start();
    }

    private boolean confirmSecureInit(AppConfig currentConfig) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Init secure connection");
        alert.setHeaderText("Prepare KOReader for key-based SSH");
        alert.setContentText("""
                On the Kindle:
                1. Open KOReader.
                2. Enable Wi-Fi.
                3. Open Network > SSH server.
                4. Enable temporary passwordless login.
                5. Start the SSH server.

                This app will connect to %s:%d as root/root, create a private key on this Mac, and append the public key to KOReader's authorized_keys file. It will not browse the Kindle filesystem.
                """.formatted(currentConfig.getHost(), currentConfig.getPort()));
        ButtonType start = new ButtonType("Start", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(start, cancel);
        return alert.showAndWait().orElse(cancel) == start;
    }

    private boolean confirmRestartThenTest() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Restart KOReader SSH");
        alert.setHeaderText("Now switch KOReader to key-only SSH");
        alert.setContentText("""
                On the Kindle:
                1. Stop the KOReader SSH server.
                2. Disable temporary passwordless login.
                3. Start the SSH server again.

                Then click "I restarted SSH" and this app will test the new key-based connection.
                """);
        ButtonType restarted = new ButtonType("I restarted SSH", ButtonBar.ButtonData.OK_DONE);
        ButtonType later = new ButtonType("Later", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(restarted, later);
        return alert.showAndWait().orElse(later) == restarted;
    }

    private void testSecureKeyConnection(AppConfig baseConfig, Path privateKeyPath) {
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        statusLabel.setText("Testing key-based connection...");
        appendUserLog("Testing key-based connection.");
        working.set(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                secureConnectionService.testPrivateKeyConnection(baseConfig, privateKeyPath);
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            working.set(false);
            progressBar.setProgress(0);
            saveConfigForSecureKey(baseConfig, privateKeyPath);
            usernameField.setText("root");
            authModeCombo.setValue(AppConfig.AuthMode.PRIVATE_KEY);
            privateKeyField.setText(privateKeyPath.toString());
            enableSimpleConnectionView();
            String message = "Secure connection confirmed. Future transfers will use this Mac's private key.";
            statusLabel.setText(message);
            appendUserLog(message);
            showAlert(Alert.AlertType.INFORMATION, "Secure connection ready",
                    message + "\n\nYou can now transfer books without temporary passwordless SSH.");
        });
        task.setOnFailed(event -> {
            working.set(false);
            progressBar.setProgress(0);
            Throwable error = task.getException();
            logService.error("Key-based connection test failed", error);
            if (isTimeout(error) && confirmRetryAfterRestartTimeout()) {
                appendUserLog("Key test timed out. Retrying after KOReader SSH restart.");
                testSecureKeyConnection(baseConfig, privateKeyPath);
                return;
            }
            String message = friendlyConnectionMessage(error);
            statusLabel.setText(message);
            appendUserLog("Key test failed. " + firstLine(message));
            showAlert(Alert.AlertType.ERROR, "Key test failed", message);
        });
        new Thread(task, "secure-key-test").start();
    }

    private boolean confirmRetryAfterRestartTimeout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Retry key connection");
        alert.setHeaderText("KOReader did not answer yet");
        alert.setContentText("""
                The key was installed, but the connection timed out while testing it.

                This often happens if the KOReader SSH server is still restarting or the Kindle Wi-Fi has not settled yet.

                On the Kindle, check that the SSH server is started again with temporary passwordless login disabled, then retry.
                """);
        ButtonType retry = new ButtonType("Retry connection", ButtonBar.ButtonData.OK_DONE);
        ButtonType later = new ButtonType("Later", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(retry, later);
        boolean shouldRetry = alert.showAndWait().orElse(later) == retry;
        if (!shouldRetry) {
            String message = "Key installed. Restart the KOReader SSH server, then use Test Connection when ready.";
            statusLabel.setText(message);
            appendUserLog(message);
        }
        return shouldRetry;
    }

    private void transferToKindle() {
        if (selectedFiles.isEmpty()) {
            String message = "Select at least one ebook file first.";
            statusLabel.setText(message);
            showAlert(Alert.AlertType.INFORMATION, "No books selected", message);
            return;
        }

        AppConfig currentConfig;
        Credentials credentials;
        try {
            currentConfig = buildConfigFromFields();
            credentials = buildCredentials();
        } catch (UserInputException ex) {
            statusLabel.setText(ex.getMessage());
            showAlert(Alert.AlertType.WARNING, "Check transfer settings", ex.getMessage());
            return;
        }

        saveConfig(currentConfig);
        List<File> files = List.copyOf(selectedFiles);
        progressBar.setProgress(0);
        statusLabel.setText("Starting transfer...");
        appendUserLog("Starting transfer of " + files.size() + " book(s).");
        working.set(true);

        Task<List<TransferResult>> task = new Task<>() {
            @Override
            protected List<TransferResult> call() throws Exception {
                return transferService.uploadBooks(files, currentConfig, credentials, new ProgressCallback() {
                    @Override
                    public void onMessage(String message) {
                        Platform.runLater(() -> {
                            statusLabel.setText(message);
                            appendUserLog(message);
                        });
                    }

                    @Override
                    public void onProgress(int completedFiles, int totalFiles) {
                        Platform.runLater(() -> progressBar.setProgress(totalFiles == 0 ? 0 : (double) completedFiles / totalFiles));
                    }

                    @Override
                    public void onFileResult(TransferResult result) {
                        Platform.runLater(() -> appendUserLog(result.message()));
                    }
                });
            }
        };
        task.setOnSucceeded(event -> {
            working.set(false);
            progressBar.setProgress(1);
            List<TransferResult> results = task.getValue();
            long failures = results.stream().filter(result -> !result.success()).count();
            if (failures == 0) {
                String message = "Transfer completed. You can now open the books in KOReader.";
                statusLabel.setText(message);
                appendUserLog(message);
            } else {
                String message = "Transfer finished with " + failures + " issue(s). Check the log below.";
                statusLabel.setText(message);
                appendUserLog(message);
                showAlert(Alert.AlertType.WARNING, "Transfer finished with issues", message);
            }
        });
        task.setOnFailed(event -> {
            working.set(false);
            progressBar.setProgress(0);
            Throwable error = task.getException();
            logService.error("Transfer failed", error);
            String message = friendlyTransferMessage(error);
            statusLabel.setText(message);
            appendUserLog("Transfer failed. " + firstLine(message));
            showAlert(Alert.AlertType.ERROR, "Transfer failed", message);
        });
        new Thread(task, "book-transfer").start();
    }

    private AppConfig buildConfigFromFields() throws UserInputException {
        String host = hostField.getText() == null ? "" : hostField.getText().trim();
        if (host.isBlank()) {
            throw new UserInputException("Enter the Kindle IP address shown by KOReader.");
        }

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            throw new UserInputException("Port must be a number. KOReader usually uses 2222.");
        }
        if (port < 1 || port > 65535) {
            throw new UserInputException("Port must be between 1 and 65535.");
        }

        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        if (username.isBlank()) {
            throw new UserInputException("Enter the SSH username. KOReader usually uses root.");
        }

        String remoteFolder = remoteFolderField.getText() == null ? "" : remoteFolderField.getText().trim();
        if (remoteFolder.isBlank()) {
            throw new UserInputException("Enter the remote books folder. KOReader usually uses /mnt/us/books.");
        }

        AppConfig current = new AppConfig();
        current.setHost(host);
        current.setPort(port);
        current.setUsername(username);
        current.setRemoteBooksPath(remoteFolder);
        current.setAuthMode(authModeCombo.getValue());
        current.setPrivateKeyPath(privateKeyField.getText() == null ? "" : privateKeyField.getText().trim());
        current.setLastLocalFolder(config.getLastLocalFolder());
        return current;
    }

    private Credentials buildCredentials() throws UserInputException {
        AppConfig.AuthMode mode = authModeCombo.getValue();
        if (mode == AppConfig.AuthMode.PASSWORD && passwordField.getText().isBlank()) {
            throw new UserInputException("Enter the KOReader SSH password.");
        }
        if (mode == AppConfig.AuthMode.PRIVATE_KEY) {
            String keyPath = privateKeyField.getText() == null ? "" : privateKeyField.getText().trim();
            if (keyPath.isBlank()) {
                throw new UserInputException("Choose a private key file.");
            }
            if (!Files.isRegularFile(Path.of(keyPath))) {
                throw new UserInputException("The private key file was not found.");
            }
        }
        return new Credentials(passwordField.getText(), passphraseField.getText());
    }

    private void saveCurrentConfigQuietly() {
        AppConfig current = new AppConfig();
        current.setHost(hostField.getText() == null ? "" : hostField.getText().trim());
        try {
            current.setPort(Integer.parseInt(portField.getText().trim()));
        } catch (RuntimeException ex) {
            current.setPort(config.getPort());
        }
        current.setUsername(usernameField.getText() == null ? "" : usernameField.getText().trim());
        current.setRemoteBooksPath(remoteFolderField.getText() == null ? "" : remoteFolderField.getText().trim());
        current.setAuthMode(authModeCombo.getValue());
        current.setPrivateKeyPath(privateKeyField.getText() == null ? "" : privateKeyField.getText().trim());
        current.setLastLocalFolder(config.getLastLocalFolder());
        saveConfig(current);
    }

    private void saveConfig(AppConfig currentConfig) {
        config = currentConfig;
        configService.save(currentConfig);
    }

    private void saveConfigForSecureKey(AppConfig baseConfig, Path privateKeyPath) {
        AppConfig keyConfig = new AppConfig();
        keyConfig.setHost(baseConfig.getHost());
        keyConfig.setPort(baseConfig.getPort());
        keyConfig.setUsername("root");
        keyConfig.setRemoteBooksPath(baseConfig.getRemoteBooksPath());
        keyConfig.setAuthMode(AppConfig.AuthMode.PRIVATE_KEY);
        keyConfig.setPrivateKeyPath(privateKeyPath.toString());
        keyConfig.setLastLocalFolder(config.getLastLocalFolder());
        saveConfig(keyConfig);
    }

    private void enableSimpleConnectionView() {
        secureKeyAvailable = true;
        if (advancedConnectionCheckBox != null) {
            setVisibleManaged(advancedConnectionCheckBox, true);
            advancedConnectionCheckBox.setSelected(false);
        }
        updateAdvancedConnectionVisibility();
    }

    private Window window() {
        return rootPane == null || rootPane.getScene() == null ? null : rootPane.getScene().getWindow();
    }

    private void openFolder(Path path) {
        try {
            Files.createDirectories(path);
            if (!Desktop.isDesktopSupported()) {
                throw new IOException("Desktop integration is not available.");
            }
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException ex) {
            logService.error("Could not open folder: " + path, ex);
            showAlert(Alert.AlertType.ERROR, "Could not open folder", "The folder is here:\n" + path);
        }
    }

    private void appendUserLog(String message) {
        String line = "[" + LocalTime.now().format(LOG_TIME_FORMAT) + "] " + message;
        if (logArea != null) {
            logArea.appendText(line + System.lineSeparator());
        }
        logService.info(message);
    }

    private String friendlyConnectionMessage(Throwable throwable) {
        Throwable root = rootCause(throwable);
        String base;
        if (root instanceof UnknownHostException || root instanceof NoRouteToHostException || root instanceof SocketTimeoutException) {
            base = "The Kindle was not reachable. Check the IP address and Wi-Fi.";
        } else if (root instanceof ConnectException) {
            base = "Connection refused. The KOReader SSH server may not be running.";
        } else if (containsIgnoreCase(root.getMessage(), "auth") || containsIgnoreCase(root.getClass().getName(), "auth")) {
            base = "Authentication failed. Check the selected authentication mode and credentials.";
        } else {
            base = "The app could not connect to the Kindle.";
        }
        return base + "\n\nMake sure KOReader is open, Wi-Fi is enabled, and the SSH server is started in KOReader under Network > SSH server. Check the IP address shown by KOReader.";
    }

    private String friendlyTransferMessage(Throwable throwable) {
        Throwable root = rootCause(throwable);
        if (containsIgnoreCase(root.getMessage(), "permission") || containsIgnoreCase(root.getMessage(), "denied")) {
            return "The remote books folder is not writable. Check the folder path and KOReader SSH settings.";
        }
        if (root instanceof UnknownHostException || root instanceof NoRouteToHostException || root instanceof SocketTimeoutException || root instanceof ConnectException) {
            return friendlyConnectionMessage(root);
        }
        return "Transfer could not finish. Check that the Kindle stayed awake, Wi-Fi stayed connected, and KOReader SSH server is still running.";
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable root = rootCause(throwable);
        return root instanceof SocketTimeoutException || containsIgnoreCase(root.getMessage(), "timed out");
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private String firstLine(String value) {
        int newline = value.indexOf('\n');
        return newline >= 0 ? value.substring(0, newline) : value;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(ok);
        alert.showAndWait();
    }

    private static final class UserInputException extends Exception {
        private UserInputException(String message) {
            super(message);
        }
    }
}
