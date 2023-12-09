module com.garcialnk.desksearx {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    requires MaterialFX;
    requires fr.brouillard.oss.cssfx;

    requires org.apache.tika.core;
    requires org.apache.lucene.core;

    opens com.garcialnk.desksearx to javafx.fxml;

    exports com.garcialnk.desksearx;
}