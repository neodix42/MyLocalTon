package org.ton.ui.custom.toastr;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

public class Notification extends VBox {
    private Label header;

    private HBox headerRow;

    private HBox contentRow;

    private Text body;

    private Label icon;

    private String pathInfo = "M12,0a12,12,0,0,0,0,24H24V12A12.013,12.013,0,0,0,12,0Zm0,5a1.5,1.5,0,1,1-1.5,1.5A1.5,1.5,0,0,1,12,5Zm2,14H12V12H10V10h2a2,2,0,0,1,2,2Z";
    private String pathSuccess = "M 12 0 A 12 12 0 1 0 24 12 A 12.013 12.013 0 0 0 12 0 Z m 0 16 a 2.993 2.993 0 0 1 -1.987 -0.752 c -0.327 -0.291 -0.637 -0.574 -0.84 -0.777 L 8 13 a 1 1 0 0 1 1.4 -1.426 L 10.58 13.05 c 0.188 0.187 0.468 0.441 0.759 0.7 a 1 1 0 0 0 1.323 0 c 0.29 -0.258 0.57 -0.512 0.752 -0.693 L 16.3 10.221 a 1 1 0 1 1 1.4 1.426 l -2.879 2.829 c -0.2 0.2 -0.507 0.48 -0.833 0.769 A 2.99 2.99 0 0 1 12 16 Z";
    private String pathWarning = "m19.944 2.634-7.944-2.634-7.944 2.634a3 3 0 0 0 -2.056 2.848v6.509c0 7.524 9.2 11.679 9.594 11.852l.354.157.368-.122c.395-.131 9.684-3.31 9.684-11.887v-6.509a3 3 0 0 0 -2.056-2.848zm-6.944 16.418h-2v-2h2zm0-4.052h-2v-10h2z";
    private String pathError = "M12,0A12,12,0,1,0,24,12,12,12,0,0,0,12,0Zm4.707,15.293-1.414,1.414L12,13.414,8.707,16.707,7.293,15.293,10.586,12,7.293,8.707,8.707,7.293,12,10.586l3.293-3.293,1.414,1.414L13.414,12Z";

    private String infoColor = "#2196f3";
    private String successColor = "#4caf50";
    private String warningColor = "#ff9800";
    private String errorColor = "#f44336";

    private boolean withErrorHeader = false;

    public Notification(NotificationType type, String text) {
        super(5);
        setStyle(buildRootStyle());
        setOpacity(0.0);
        body = new Text();
        icon = new Label();
        icon.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        SVGPath svg = new SVGPath();
        switch (type) {
            case INFO:
                svg.setContent(pathInfo);
                svg.setStyle(buildSvgStyle(svg, infoColor));
                break;
            case SUCCESS:
                svg.setContent(pathSuccess);
                svg.setStyle(buildSvgStyle(svg, successColor));
                break;
            case WARNING:
                svg.setContent(pathWarning);
                svg.setStyle(buildSvgStyle(svg, warningColor));
                break;
            case ERROR:
                if(withErrorHeader) {
                    headerRow = new HBox();
                    headerRow.setAlignment(Pos.CENTER);
                    header = new Label("Error");
                    header.setStyle(buildHeaderStyle());
                    header.setText("Error");
                    headerRow.getChildren().add(header);
                    getChildren().add(headerRow);
                }
                svg.setContent(pathError);
                svg.setStyle(buildSvgStyle(svg, errorColor));
                break;
        }
        icon.setGraphic(svg);
        contentRow = new HBox(20);
        body.setText(text);
        body.setStyle(buildBodyStyle());
        body.managedProperty().bind(body.textProperty().isNotEmpty());
        contentRow.getChildren().addAll(icon, body);
        getChildren().add(contentRow);
    }


    protected String buildRootStyle() {
        return getStyle() + "-fx-padding: 10px;" + "-fx-effect: dropshadow(" + "  gaussian," + "  rgba(105, 105, 105, 0.3)," + "  10," + "  0.2," + "  4," + "  4" + ");" + "-fx-background-color: whitesmoke;";
    }

    protected String buildHeaderStyle() {
        return header.getStyle() + "-fx-text-fill: #1d1d1d;" + "-fx-font-size: 1.5em;";
    }

    protected String buildBodyStyle() {
        return body.getStyle() + "-fx-text-fill: #000000;" + "-fx-font-size: 1.2em;";
    }

    private String buildSvgStyle(SVGPath svg, String color) {
        return svg.getStyle() + "-fx-fill: " + color + ";";
    }

    public enum NotificationType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR;
    }

}
