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
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

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
        stage.getIcons().add(createPlaceholderIcon());
        stage.setMinWidth(760);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    private WritableImage createPlaceholderIcon() {
        int size = 64;
        WritableImage image = new WritableImage(size, size);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean border = x < 6 || y < 6 || x >= size - 6 || y >= size - 6;
                boolean spine = x >= 15 && x <= 20 && y >= 12 && y <= 52;
                boolean page = x >= 22 && x <= 49 && y >= 12 && y <= 52;
                Color color = Color.TRANSPARENT;
                if (border) {
                    color = Color.rgb(36, 48, 62);
                } else if (spine) {
                    color = Color.rgb(47, 111, 139);
                } else if (page) {
                    color = Color.rgb(238, 232, 217);
                }
                image.getPixelWriter().setColor(x, y, color);
            }
        }
        return image;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
