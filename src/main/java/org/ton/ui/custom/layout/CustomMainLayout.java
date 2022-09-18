package org.ton.ui.custom.layout;

import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import org.ton.ui.custom.control.CustomButton;
import org.ton.ui.custom.control.CustomMenuButton;
import org.ton.ui.custom.control.CustomSearchBar;
import org.ton.ui.custom.media.NoTransactionsController;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class CustomMainLayout extends AnchorPane implements Initializable {

    @FXML
    AnchorPane logoPane, search, buttons, info;

    @FXML
    private CustomSearchBar searchBar;

    @FXML
    private CustomMenuButton blocksBtn, transactionsBtn, validationBtn, accountsBtn, settingBtn, resultsBtn;

    @FXML
    private AnchorPane contentPane, blocksPane, transactionsPane, validationPane, accountsPane, blankPane,
            settingsPane, explorerPane, searchPane, statusPane;

    @FXML
    private HBox topButton;

    @FXML
    private Pane lockPane;

    @FXML
    private AnchorPane settingsButtonsPane, resultsButtonsPane;

    private Node view;

    private CustomButton createButton;

    public CustomMainLayout()  {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main-layout.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void click(Event e) throws IOException, InterruptedException {
        removeCreateButton();
        if(e.getTarget().equals(settingBtn)) {
            rotateButton(settingBtn);
            blankPane.toFront();
        } else if(e.getTarget().equals(resultsBtn)) {
            rotateButton(resultsBtn);
            blankPane.toFront();
        } else {
            if (settingBtn.isOpened()) {
                rotateButton(settingBtn);
                closeSettings(false);
            } else if (resultsBtn.isOpened()) {
                rotateButton(resultsBtn);
                closeResults(false);
            }
            if(e.getTarget().equals(blocksBtn)) {
                blocksPane.toFront();
            } else if(e.getTarget().equals(transactionsBtn)) {
                transactionsPane.toFront();
                //String str = getClass().getResource("/com/github/fabiomqs/custom/media").toExternalForm();
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/github/fabiomqs/custom/media/no-transactions-pane.fxml"));
                view = fxmlLoader.load();
                NoTransactionsController controller = fxmlLoader.getController();
                controller.setMain(this);
                this.contentPane.getChildren().add(view);
            } else if(e.getTarget().equals(validationBtn)) {
                validationPane.toFront();


                //FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("loading-pane.fxml"));
                //view = fxmlLoader.load();
                //this.getChildren().add(view);

            } else if(e.getTarget().equals(accountsBtn)) {
                createButton = new CustomButton();
                createButton.setText("Create");
                createButton.setGraphicTextGap(10.0);
                createButton.setPrefWidth(110.0);
                createButton.setSvgText(
                            "M9.89189 3.93651C9.533 3.99094 9.26048 4.30332 9.26583 4.67632L9.26528 9.26518L4.67415 9.26584C4.41031 9.26209 4.16292 9.40182 4.02884 9.63155C3.89476 9.86127 3.89476 10.1454 4.02884 10.3751L4.09297 10.4679C4.23398 10.6418 4.4483 10.744 4.67627 10.7408L9.26528 10.7402L9.26582 15.3325C9.26207 15.5963 9.40181 15.8437 9.63153 15.9778C9.86126 16.1119 10.1454 16.1119 10.3751 15.9778L10.4679 15.9137C10.6418 15.7727 10.744 15.5584 10.7408 15.3304L10.7403 10.7402L15.3325 10.7408C15.5963 10.7446 15.8437 10.6048 15.9778 10.3751C16.1119 10.1454 16.1119 9.86127 15.9778 9.63155L15.9137 9.53876C15.7727 9.36483 15.5583 9.26263 15.3304 9.26585L10.7403 9.26518L10.7408 4.67417C10.7437 4.4769 10.6656 4.28481 10.5245 4.14387C10.3834 4.00296 10.1913 3.92517 9.99186 3.9282L9.89189 3.93651Z"
                );
                createButton.setOnAction(action -> createAccount());
                this.topButton.getChildren().add(createButton);
                accountsPane.toFront();
            }
        }
    }



    public void handleEvent(Event e) {
        this.getChildren().remove(view);
    }

    private void rotateButton(CustomMenuButton btn) {
        if (!btn.isOpened()) {
            rotate(btn, 90.0);
            if(btn.equals(settingBtn)) {
                if (resultsBtn.isOpened()) {
                    rotate(resultsBtn, 0.0);
                    closeResults(true);
                } else {
                    openSettings();
                }
            } else if(btn.equals(resultsBtn)) {
                if(settingBtn.isOpened()) {
                    rotate(settingBtn, 0.0);
                    closeSettings(true);
                } else {
                    openResults();
                }

            }
        } else {
            rotate(btn, 0.0);
            if(btn.equals(settingBtn)) {
                closeSettings(false);
            } else if(btn.equals(resultsBtn)) {
                closeResults(false);
            }
        }
    }

    private void rotate(CustomMenuButton btn, double angle) {
        Label lb = (Label) btn.getView().lookup("#secondLabel");
        SVGPath svg = (SVGPath) lb.getGraphic();
        RotateTransition transition = new RotateTransition();
        transition.setNode(svg);
        transition.setDuration(Duration.seconds(0.4));
        transition.setToAngle(angle);
        transition.play();
    }

    private void openSettings() {
        TranslateTransition slideResult = new TranslateTransition();
        slideResult.setDuration(Duration.seconds(0.4));
        slideResult.setNode(resultsBtn);
        slideResult.setToY(180.0);
        slideResult.play();

        TranslateTransition slidePane = new TranslateTransition();
        slidePane.setDuration(Duration.seconds(0.4));
        slidePane.setNode(settingsButtonsPane);

        slidePane.setToX(240.0);
        slidePane.setToY(160.0);
        slidePane.play();

    }

    private void closeSettings(boolean opening) {
        TranslateTransition slideResult = new TranslateTransition();
        slideResult.setDuration(Duration.seconds(0.4));
        slideResult.setNode(resultsBtn);
        slideResult.setToY(0.0);
        slideResult.play();

        TranslateTransition slidePane = new TranslateTransition();
        slidePane.setDuration(Duration.seconds(0.4));
        slidePane.setNode(settingsButtonsPane);

        slidePane.setToX(0.0);
        slidePane.setToY(0.0);
        if(opening) {
            slidePane.setOnFinished(e -> {
                openResults();
            });
        }
        slidePane.play();

    }

    private void openResults() {
        TranslateTransition slidePane = new TranslateTransition();
        slidePane.setDuration(Duration.seconds(0.4));
        slidePane.setNode(resultsButtonsPane);

        slidePane.setToX(240.0);
        slidePane.setToY(130.0);
        slidePane.play();
    }

    private void closeResults(boolean opening) {
        TranslateTransition slidePane = new TranslateTransition();
        slidePane.setDuration(Duration.seconds(0.4));
        slidePane.setNode(resultsButtonsPane);

        slidePane.setToX(0.0);
        slidePane.setToY(0.0);
        if(opening) {
            slidePane.setOnFinished(e -> {
                openSettings();
            });
        }
        slidePane.play();
    }



    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public CustomSearchBar getSearchBar() {
        return this.searchBar;
    }

    public final ObservableList<Node> getInfo() {
        return info.getChildren();
    }

    public final ObservableList<Node> getBlocksPane() {
        return blocksPane.getChildren();
    }

    public final ObservableList<Node> getTransactionsPane() {
        return transactionsPane.getChildren();
    }

    public final ObservableList<Node> getValidationPane() {
        return validationPane.getChildren();
    }

    public final ObservableList<Node> getAccountsPane() {
        return accountsPane.getChildren();
    }

    public final ObservableList<Node> getSettingsPane() {
        return settingsPane.getChildren();
    }

    public final ObservableList<Node> getExplorerPane() {
        return explorerPane.getChildren();
    }

    public final ObservableList<Node> getSearchPane() {
        return searchPane.getChildren();
    }

    public final ObservableList<Node> getStatusPane() {
        return statusPane.getChildren();
    }


    public void removeView() {
        this.getChildren().remove(view);
        view = null;
    }

    public void removeNoTransactionsView() {
        this.contentPane.getChildren().remove(view);
        view = null;
    }

    private void removeCreateButton() {
        if(createButton != null) {
            if(this.topButton.getChildren().contains(createButton)) {
                this.topButton.getChildren().remove(createButton);
                createButton = null;
            }
        }
    }

    private void createAccount() {
    }
}
