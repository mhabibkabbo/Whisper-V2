module com.example.drafts {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires java.prefs;
    requires javafx.media;

    opens com.example.drafts to javafx.fxml;
    opens com.example.drafts.controllers to javafx.fxml;
    exports com.example.drafts;
}