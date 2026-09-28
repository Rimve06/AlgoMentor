package com.algomentor;

import com.algomentor.db.DatabaseManager;
import com.algomentor.util.AppConfig;
import com.algomentor.util.AppExecutors;
import com.algomentor.util.SessionContext;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application entry point. Wires up the database, then shows the login
 * screen. The root Scene resizes with the window because every screen's
 * root layout is loaded with no fixed pixel size, and each FXML view uses
 * responsive layout panes (see login.fxml / main.fxml).
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        AppConfig.printStartupReport();

        Path dbPath = Paths.get(System.getProperty("user.home"), ".algomentor", "algomentor.db");
        dbPath.getParent().toFile().mkdirs();

        // This file lives in your home folder, NOT the project folder - so it survives
        // even when you replace every project file with a new zip. Old test accounts
        // from earlier runs (or earlier, buggier versions of this project) stay in it
        // forever unless you clear it. Set RESET_DB_ON_START=true in algomentor.properties
        // for one run to wipe it and start completely clean, then remove that line again.
        if ("true".equalsIgnoreCase(AppConfig.get("RESET_DB_ON_START", "false"))) {
            boolean deleted = dbPath.toFile().delete();
            System.out.println("[Main] RESET_DB_ON_START=true -> " + dbPath
                    + (deleted ? " deleted." : " was already absent (nothing to delete)."));
        }

        DatabaseManager db = new DatabaseManager(dbPath.toString());
        System.out.println("[Main] Database file: " + dbPath.toAbsolutePath());
        SessionContext.init(db);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        stage.setTitle("AlgoMentor \u2014 Algorithm Visualizer & Mentor");
        stage.setMinWidth(760);
        stage.setMinHeight(520);
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(e -> {
            db.close();
            AppExecutors.shutdown();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}