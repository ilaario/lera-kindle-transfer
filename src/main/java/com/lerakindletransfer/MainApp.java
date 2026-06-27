package com.lerakindletransfer;

import com.lerakindletransfer.controller.MainController;
import com.lerakindletransfer.service.ConfigService;
import com.lerakindletransfer.service.KeyPairService;
import com.lerakindletransfer.service.LogService;
import com.lerakindletransfer.service.SecureConnectionService;
import com.lerakindletransfer.service.SftpTransferService;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public final class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        LogService logService = new LogService();
        ConfigService configService = new ConfigService(logService);
        SftpTransferService transferService = new SftpTransferService(logService);
        SecureConnectionService secureConnectionService = new SecureConnectionService(
                transferService,
                new KeyPairService(),
                logService
        );
        MainController controller = new MainController(configService, transferService, secureConnectionService, logService);

        Parent root = controller.createView();
        Scene scene = new Scene(root, 900, 720);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.setTitle("Lera Kindle Transfer");
        Image appIcon = loadAppIcon();
        if (appIcon != null) {
            stage.getIcons().add(appIcon);
        }
        stage.setMinWidth(760);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    private Image loadAppIcon() {
        try (var iconStream = getClass().getResourceAsStream("/icons/lera-kindle-transfer.png")) {
            return iconStream == null ? null : new Image(iconStream);
        } catch (IOException ignored) {
            return null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
