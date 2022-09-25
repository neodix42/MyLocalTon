package org.ton.ui.custom.layout;

import javafx.animation.TranslateTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
import org.ton.ui.custom.events.CustomEvent;
import org.ton.ui.custom.events.event.CustomActionEvent;
import org.ton.ui.custom.events.event.CustomNotificationEvent;
import org.ton.ui.custom.toastr.Notification;
import org.ton.ui.custom.toastr.NotificationAnchorStack;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static org.ton.ui.custom.events.CustomEventBus.emit;
import static org.ton.ui.custom.events.CustomEventBus.listenFor;


public class CustomMainLayout extends AnchorPane implements Initializable {

    @FXML
    AnchorPane logoPane, search, buttons, info;

    @FXML
    private CustomMenuButton blocksBtn, transactionsBtn, validationBtn, accountsBtn, explorerBtn;

    @FXML
    private CustomExpandButton settingBtn, resultsBtn;

    @FXML
    private AnchorPane contentPane, blocksPane, transactionsPane, validationPane, accountsPane, blankPane,
            settingsPane, logsPane, accountsKeysPane, settingsValidatorsPane, blockchainPane, userInterfacePane,
            aboutPane, explorerPane, searchPane;
    //, statusPane;

    @FXML
    private HBox topButton;

    @FXML
    private Pane lockPane;

    @FXML
    private CustomSearchBar searchBar;

    @FXML
    private NotificationAnchorStack notificationStack;

    //private Node view;

    private CustomButton createButton;

    private Pane lastPane = null;

    private Node loadingPane = null;

    private boolean explorer;

    public CustomMainLayout() throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main-layout.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();


        contentPane.getChildren().remove(blocksPane);
        contentPane.getChildren().remove(transactionsPane);
        contentPane.getChildren().remove(validationPane);
        contentPane.getChildren().remove(accountsPane);

        contentPane.getChildren().remove(logsPane);
        contentPane.getChildren().remove(settingsValidatorsPane);
        contentPane.getChildren().remove(accountsKeysPane);
        contentPane.getChildren().remove(blockchainPane);
        contentPane.getChildren().remove(userInterfacePane);
        contentPane.getChildren().remove(aboutPane);
        contentPane.getChildren().remove(explorerPane);
        lastPane = blankPane;
        listenFor(CustomNotificationEvent.class, this::handle);
        listenFor(CustomActionEvent.class, this::handle);

        buttons.getChildren().remove(explorerBtn);

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
                    if(hasExplorer()) {
                        TranslateTransition tr2 = new TranslateTransition(Duration.seconds(0.001), explorerBtn);
                        tr2.setToY(arg2.doubleValue() - explorerBtn.getPrefHeight());
                        tr2.play();
                    }
                }
            }
        });
    }



    @FXML
    public void handleSettingsLogs(ActionEvent event) {
                changeContent(logsPane);
    }
    @FXML
    public void handleSettingsValidators(ActionEvent event) {
        changeContent(settingsValidatorsPane);
    }
    @FXML
    public void handleSettingsAccountsKeys(ActionEvent event){
        changeContent(accountsKeysPane);
    }
    @FXML
    public void handleSettingsBlockchain(ActionEvent event){
        changeContent(blockchainPane);
    }
    @FXML
    public void handleSettingsUserInterface(ActionEvent event) {
        changeContent(userInterfacePane);
    }
    @FXML
    public void handleSettingsAbout(ActionEvent event) {
        changeContent(aboutPane);
    }



    private boolean hasResult() {
        return buttons.getChildren().contains(resultsBtn);
    }

    private boolean hasExplorer() {
        return this.explorer;
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
        createButton.setOnAction(action -> emit(new CustomActionEvent(CustomEvent.Type.CLICK)));
        createButton.getStyleClass().add("custom-button-btn");
        this.topButton.getChildren().add(createButton);
        changeContent(accountsPane);
    }

    @FXML
    private void clickExplorer(Event e) {
        resetButtons();
        explorerBtn.activate();
        changeContent(explorerPane);
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
        explorerBtn.deactivate();
    }

    private void changeContent(Pane pane) {
        contentPane.getChildren().remove(lastPane);
        contentPane.getChildren().add(pane);
        lastPane = pane;
    }



    public CustomSearchBar getSearchBar() {
        return this.searchBar;
    }

    public ObservableList<Node> getSearch() {
        return search.getChildren();
    }

    public ObservableList<Node> getInfo() {
        return info.getChildren();
    }

    public ObservableList<Node> getBlocksPane() {
        return blocksPane.getChildren();
    }

    public ObservableList<Node> getTransactionsPane() {
        return transactionsPane.getChildren();
    }

    public ObservableList<Node> getValidationPane() {
        return validationPane.getChildren();
    }

    public ObservableList<Node> getAccountsPane() {
        return accountsPane.getChildren();
    }

    public ObservableList<Node> getSettingsPane() {
        return settingsPane.getChildren();
    }

    public ObservableList<Node> getLogsPane() {
        return logsPane.getChildren();
    }

    public ObservableList<Node> getAccountsKeysPane() {
        return accountsKeysPane.getChildren();
    }

    public ObservableList<Node> getSettingsValidatorsPane() {
        return settingsValidatorsPane.getChildren();
    }

    public ObservableList<Node> getBlockchainPane() {
        return blockchainPane.getChildren();
    }

    public ObservableList<Node> getUserInterfacePane() {
        return userInterfacePane.getChildren();
    }

    public ObservableList<Node> getAboutPane() {
        return aboutPane.getChildren();
    }

    public ObservableList<Node> getExplorerPane() {
        return explorerPane.getChildren();
    }

    public ObservableList<Node> getSearchPane() {
        return searchPane.getChildren();
    }

    public void setExplorer(boolean explorer) {
        this.explorer = explorer;
        if(explorer) {
            if(hasResult()) {
                buttons.getChildren().remove(resultsBtn);
                resultsBtn.setLayoutY(320.0);
                buttons.getChildren().add(resultsBtn);
            }
            //270.0
            buttons.getChildren().add(explorerBtn);
        }
    }


    private void removeCreateButton() {
        if(createButton != null) {
            if(this.topButton.getChildren().contains(createButton)) {
                this.topButton.getChildren().remove(createButton);
                createButton = null;
            }
        }
    }

    public void handle(CustomEvent event){
        switch (event.getEventType()) {
            case INFO:
                addInfo((CustomNotificationEvent) event);
                break;
            case SUCCESS:
                addSuccess((CustomNotificationEvent) event);
                break;
            case WARNING:
                addWarning((CustomNotificationEvent) event);
                break;
            case ERROR:
                addError((CustomNotificationEvent) event);
                break;
            case START:
                blocksBtn.activate();
                changeContent(blocksPane);
                break;
        }
    }

    private void addInfo(CustomNotificationEvent event) {
        Notification note = new Notification(Notification.NotificationType.INFO, event.getMessage());
        notificationStack.notify(note, (int) event.getSeconds() * 1000);
    }

    private void addSuccess(CustomNotificationEvent event) {
        Notification note = new Notification(Notification.NotificationType.SUCCESS, event.getMessage());
        notificationStack.notify(note, (int) event.getSeconds() * 1000);
    }

    private void addError(CustomNotificationEvent event) {
        Notification note = new Notification(Notification.NotificationType.ERROR, event.getMessage());
        notificationStack.notify(note, (int) event.getSeconds() * 1000);
    }

    private void addWarning(CustomNotificationEvent event) {
        Notification note = new Notification(Notification.NotificationType.WARNING, event.getMessage());
        notificationStack.notify(note, (int) event.getSeconds() * 1000);
    }

    private void notify(Notification note, final int show ) {
        notificationStack.notify(note, show);
    }

}
