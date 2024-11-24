module DeosoundApp {
    requires java.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires com.google.gson;
	requires javafx.media;
	requires java.net.http;
	requires javafx.base;

    opens com.deosound to javafx.fxml;
    exports com.deosound;
}
