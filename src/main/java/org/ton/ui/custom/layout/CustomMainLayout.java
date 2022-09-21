package org.ton.ui.custom.layout;

import javafx.animation.TranslateTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import org.ton.ui.custom.control.CustomButton;
import org.ton.ui.custom.control.CustomExpandButton;
import org.ton.ui.custom.control.CustomMenuButton;
import org.ton.ui.custom.control.CustomSearchBar;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class CustomMainLayout extends AnchorPane implements Initializable {

    @FXML
    AnchorPane logoPane, search, buttons, info;

    @FXML
    private CustomMenuButton blocksBtn, transactionsBtn, validationBtn, accountsBtn;

    @FXML
    private CustomExpandButton settingBtn, resultsBtn;

    @FXML
    private AnchorPane contentPane, blocksPane, transactionsPane, validationPane, accountsPane, blankPane,
            settingsPane, explorerPane, searchPane, statusPane;

    @FXML
    private HBox topButton;

    @FXML
    private Pane lockPane;

    @FXML
    private CustomSearchBar searchBar;

    //private Node view;

    private CustomButton createButton;

    private Pane lastPane = null;

    private Node loadingPane = null;

    public CustomMainLayout() throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main-layout.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();


        contentPane.getChildren().remove(blocksPane);
        contentPane.getChildren().remove(transactionsPane);
        contentPane.getChildren().remove(validationPane);
        contentPane.getChildren().remove(accountsPane);

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        settingBtn.heightProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> arg0,
                                Number arg1, Number arg2) {
                if(arg1.doubleValue() > 0.0) {
                    //TODO: search result button and explorer button
                    if(hasResult()) {
                        TranslateTransition tr = new TranslateTransition(Duration.seconds(0.001), resultsBtn);
                        tr.setToY(arg2.doubleValue() - settingBtn.getMainButtonHeight());
                        tr.play();
                    }
                }
            }
        });
    }


    private boolean hasResult() {
        return buttons.getChildren().contains(resultsBtn);
    }

    @FXML
    private void clickExpandSettings(MouseEvent e) {
        resetButtons();
        if(!settingBtn.isOpened()) {
            blankPane.toFront();
            settingBtn.activate();
        }
        removeCreateButton();
        settingBtn.rotateIcon();
        if(resultsBtn.isOpened())
            resultsBtn.rotateIcon();
    }

    @FXML
    private void clickExpandResults(MouseEvent e) {
        resetButtons();
        if(!resultsBtn.isOpened()) {
            blankPane.toFront();
            resultsBtn.activate();
        }
        removeCreateButton();
        resultsBtn.rotateIcon();
        if(settingBtn.isOpened())
            settingBtn.rotateIcon();
    }

    @FXML
    private void clickBlocks(Event e) {
        resetButtons();
        blocksBtn.activate();
        changeContent(blocksPane);
    }

    @FXML
    private void clickTransactions(Event e) {
        resetButtons();
        transactionsBtn.activate();
        changeContent(transactionsPane);
    }

    @FXML
    private void clickValidation(Event e) {
        resetButtons();
        validationBtn.activate();
        changeContent(validationPane);
    }

    @FXML
    private void clickAccount(Event e) throws IOException {
        resetButtons();
        accountsBtn.activate();
        createButton = new CustomButton(CustomButton.CustomButtonType.CREATE, 110.0);
        createButton.setOnAction(action -> createAccount());
        this.topButton.getChildren().add(createButton);
        changeContent(accountsPane);
    }


    private void resetButtons() {
        deactivateAllButton();
        if (settingBtn.isOpened()) {
            settingBtn.rotateIcon();
        } else if (resultsBtn.isOpened()) {
            resultsBtn.rotateIcon();
        }
    }

    private void deactivateAllButton() {
        removeCreateButton();

        blocksBtn.deactivate();
        transactionsBtn.deactivate();
        validationBtn.deactivate();
        accountsBtn.deactivate();
        settingBtn.deactivate();
        resultsBtn.deactivate();
    }

    private void changeContent(Pane pane) {
        contentPane.getChildren().remove(lastPane);
        contentPane.getChildren().add(pane);
        lastPane = pane;
    }



    public CustomSearchBar getSearchBar() {
        return this.searchBar;
    }

    public final ObservableList<Node> getSearch() {
        return search.getChildren();
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

    public void showLoadingPane() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("loading-pane.fxml"));
        loadingPane = fxmlLoader.load();
        //CustomLoadingPane controller = fxmlLoader.getController();
        //controller.setMain(this);
        this.getChildren().add(loadingPane);
    }

    public void removeLoadingPane() throws IOException {

        this.getChildren().remove(loadingPane);
        loadingPane = null;

        resetButtons();
        blocksBtn.activate();
        changeContent(blocksPane);
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
