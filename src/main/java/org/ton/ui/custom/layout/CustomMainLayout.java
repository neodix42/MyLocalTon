package org.ton.ui.custom.layout;

import com.jfoenix.controls.JFXListView;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.ton.main.App;
import org.ton.ui.custom.control.*;
import org.ton.ui.custom.events.CustomEvent;
import org.ton.ui.custom.events.event.CustomActionEvent;
import org.ton.ui.custom.events.event.CustomNotificationEvent;
import org.ton.ui.custom.events.event.CustomRefreshEvent;
import org.ton.ui.custom.events.event.CustomSearchEvent;
import org.ton.ui.custom.media.NoBlocksTransactionsController;
import org.ton.ui.custom.toastr.Notification;
import org.ton.ui.custom.toastr.NotificationAnchorStack;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static org.ton.ui.custom.events.CustomEventBus.emit;
import static org.ton.ui.custom.events.CustomEventBus.listenFor;


public class CustomMainLayout extends AnchorPane implements Initializable {

    @FXML
    private CustomMenuButton blocksBtn, transactionsBtn, validationBtn, accountsBtn, explorerBtn, tonHttpApiBtn;

    @FXML
    private CustomExpandButton settingBtn, resultsBtn;

    @FXML
    private CustomSubMenuButton logsBtn, foundBlocksBtn, foundTxsBtn, foundAccountsBtn, foundAccountsTxsBtn;

    @FXML
    private AnchorPane logoPane, search, buttons, info, contentPane, blocksPane, transactionsPane, validationPane, accountsPane, blankPane,
            logsPane, accountsKeysPane, settingsValidatorsPane, blockchainPane, userInterfacePane,
            aboutPane, explorerPane, tonHttpApiPane, foundBlocksPane, foundAccountsPane, foundTxsPane, foundAccountsTxsPane;

    @FXML
    private HBox topButton, scrollButton, closeButton;

    @FXML
    private CustomSearchBar searchBar;

    @FXML
    private NotificationAnchorStack notificationStack;

    private CustomButton createButton;

    private Node lastPane = null;

    private boolean explorer;

    private boolean tonHttpApi;

    private boolean hasBlocks = false;

    private boolean hasTransactions = false;

    private boolean hasAccounts = false;

    private Node noBlocksTransactions = null;

    private NoBlocksTransactionsController noBlocksTransactionsController = null;

    public CustomMainLayout() throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("main-layout.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.load();

        //remove all the panes but the blank
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
        contentPane.getChildren().remove(tonHttpApiPane);
        contentPane.getChildren().remove(foundBlocksPane);
        contentPane.getChildren().remove(foundAccountsPane);
        contentPane.getChildren().remove(foundTxsPane);
        contentPane.getChildren().remove(foundAccountsTxsPane);

        lastPane = blankPane;

        listenFor(CustomNotificationEvent.class, this::handle);
        listenFor(CustomActionEvent.class, this::handle);
        listenFor(CustomSearchEvent.class, this::handle);
        listenFor(CustomRefreshEvent.class, this::handle);

        //remove explorer, ton-http-api and results buttons
        buttons.getChildren().remove(explorerBtn);
        buttons.getChildren().remove(tonHttpApiBtn);
        buttons.getChildren().remove(resultsBtn);

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //move the buttons below settings buttons when open/close
        settingBtn.heightProperty().addListener(new ChangeListener<Number>() {

            @Override
            public void changed(ObservableValue<? extends Number> arg0,
                                Number arg1, Number arg2) {
                if (arg1.doubleValue() > 0.0) {
                    if (hasResult()) {
                        TranslateTransition tr = new TranslateTransition(Duration.seconds(0.001), resultsBtn);
                        tr.setToY(arg2.doubleValue() - settingBtn.getMainButtonHeight());
                        tr.play();
                    }
                    if (hasExplorer()) {
                        TranslateTransition tr2 = new TranslateTransition(Duration.seconds(0.001), explorerBtn);
                        tr2.setToY(arg2.doubleValue() - explorerBtn.getPrefHeight());
                        tr2.play();
                    }
                    if (hasTonHttpApi()) {
                        TranslateTransition tr2 = new TranslateTransition(Duration.seconds(0.001), tonHttpApiBtn);
                        tr2.setToY(arg2.doubleValue() - tonHttpApiBtn.getPrefHeight());
                        tr2.play();
                    }
                }
            }
        });
    }

    @FXML
    private void handleSettingsLogs(ActionEvent event) {
        changeContent(logsPane);
    }

    @FXML
    private void handleSettingsValidators(ActionEvent event) {
        changeContent(settingsValidatorsPane);
    }

    @FXML
    private void handleSettingsAccountsKeys(ActionEvent event) {
        changeContent(accountsKeysPane);
    }

    @FXML
    private void handleSettingsBlockchain(ActionEvent event) {
        changeContent(blockchainPane);
    }

    @FXML
    private void handleSettingsUserInterface(ActionEvent event) {
        changeContent(userInterfacePane);
    }

    @FXML
    private void handleSettingsAbout(ActionEvent event) {
        changeContent(aboutPane);
    }

    @FXML
    private void handleFoundBlocks(ActionEvent event) {
        changeContent(foundBlocksPane);
    }

    @FXML
    private void handleFoundAccountsTxs() {
        changeContent(foundAccountsTxsPane);
    }

    @FXML
    private void handleFoundAccounts(ActionEvent event) {
        changeContent(foundAccountsPane);
    }

    @FXML
    private void handleFoundTxs(ActionEvent event) {
        changeContent(foundTxsPane);
    }

    @FXML
    private void clickExpandSettings(MouseEvent e) {
        resetButtons();
        removeCreateButton();
        if (resultsBtn.isOpened())
            resultsBtn.rotateIcon();
        if (!settingBtn.isOpened()) {
            if (!isEditingSettings()) {
                logsBtn.requestFocus();
                changeContent(logsPane);
            }
        }
        settingBtn.rotateIcon();
        settingBtn.activate();
    }

    @FXML
    private void clickExpandResults(MouseEvent e) {
        resetButtons();
        removeCreateButton();
        if (settingBtn.isOpened())
            settingBtn.rotateIcon();
        if (!resultsBtn.isOpened()) {
            if (!isViewingResults()) {
                foundBlocksBtn.requestFocus();
                changeContent(foundBlocksPane);
            }
        }
        resultsBtn.activate();
        resultsBtn.rotateIcon();
    }

    @FXML
    private void clickBlocks(Event e) {
        resetButtons();
        blocksBtn.activate();
        if (hasBlocks)
            changeContent(blocksPane);
        else
            checkBlocks();
    }

    @FXML
    private void clickTransactions(Event e) {
        resetButtons();
        transactionsBtn.activate();
        if (hasTransactions)
            changeContent(transactionsPane);
        else
            checkTransactions();
    }


    private boolean hasResult() {
        return buttons.getChildren().contains(resultsBtn);
    }

    private boolean hasExplorer() {
        return this.explorer;
    }

    private boolean hasTonHttpApi() {
        return this.tonHttpApi;
    }

    private void loadNoTransactionsNoBlocks(NoBlocksTransactionsController.ViewType viewType) {
        if (noBlocksTransactions == null) {
            try {
                FXMLLoader loader = new FXMLLoader(App.class.getClassLoader().getResource("org/ton/ui/custom/media/no-blocks-transactions-pane.fxml"));

                noBlocksTransactions = loader.load();

                noBlocksTransactionsController = loader.getController();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

        }
        noBlocksTransactionsController.setViewType(viewType);
        changeContent(noBlocksTransactions);
    }

    private void refreshTransactions() {
        if (((JFXListView) transactionsPane.lookup("#transactionsvboxid")).getItems().size() > 0) {
            hasTransactions = true;
            changeContent(transactionsPane);
        }
    }

    private void refreshBlocks() {
        if (((JFXListView) blocksPane.lookup("#blockslistviewid")).getItems().size() > 0) {
            hasBlocks = true;
            changeContent(blocksPane);
        }
    }

    private void refreshAccounts() throws IOException {
        if (((JFXListView) accountsPane.lookup("#accountsvboxid")).getItems().size() > 0) {
            hasAccounts = true;
            addCreateButton();
            changeContent(accountsPane);
        }
    }

    private void checkBlocks() {
        if (((JFXListView) blocksPane.lookup("#blockslistviewid")).getItems().size() == 0) {
            loadNoTransactionsNoBlocks(NoBlocksTransactionsController.ViewType.BLOCKS);
        } else {
            hasBlocks = true;
            changeContent(blocksPane);
        }
    }

    private void checkTransactions() {
        if (((JFXListView) transactionsPane.lookup("#transactionsvboxid")).getItems().size() == 0) {
            loadNoTransactionsNoBlocks(NoBlocksTransactionsController.ViewType.TRANSACTIONS);
        } else {
            hasTransactions = true;
            changeContent(transactionsPane);
        }
    }

    private void checkAccounts() throws IOException {
        if (((JFXListView) accountsPane.lookup("#accountsvboxid")).getItems().size() == 0) {
            loadNoTransactionsNoBlocks(NoBlocksTransactionsController.ViewType.ACCOUNTS);
        } else {
            hasAccounts = true;
            addCreateButton();
            changeContent(accountsPane);
        }
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
        if (hasAccounts) {
            addCreateButton();
            changeContent(accountsPane);
        } else
            checkAccounts();
    }

    private void addCreateButton() throws IOException {
        createButton = new CustomButton(CustomButton.CustomButtonType.CREATE, 180.0);
        createButton.setOnAction(action -> emit(new CustomActionEvent(CustomEvent.Type.CLICK)));
        createButton.getStyleClass().add("custom-button-btn");
        this.topButton.getChildren().add(createButton);
    }

    @FXML
    private void clickExplorer(Event e) {
        resetButtons();
        explorerBtn.activate();
        changeContent(explorerPane);
    }

    @FXML
    private void clickTonHttpApi(Event e) {
        resetButtons();
        tonHttpApiBtn.activate();
        changeContent(tonHttpApiPane);
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
        tonHttpApiBtn.deactivate();
    }

    private void changeContent(Node pane) {
        try {
            Platform.runLater(() -> emit(new CustomActionEvent(CustomEvent.Type.SAVE_SETTINGS)));
            contentPane.getChildren().remove(lastPane);
            contentPane.getChildren().add(pane);
            lastPane = pane;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setExplorer(boolean explorer) {
        this.explorer = explorer;
        if (explorer) {
            buttons.getChildren().add(explorerBtn);
        }
    }

    public void setTonHttpApi(boolean httpApi) {
        this.tonHttpApi = httpApi;
        if (httpApi) {
            if (hasExplorer()) {
                tonHttpApiBtn.setLayoutY(320);
            } else {
                tonHttpApiBtn.setLayoutY(270);
            }
            buttons.getChildren().add(tonHttpApiBtn);
        }
    }

    private void setNumFoundBlocks(int num) {
        foundBlocksBtn.setText("Blocks ( " + num + " )");
    }

    private void setNumFoundTxs(int num) {
        foundTxsBtn.setText("TXs ( " + num + " )");
    }

    private void setNumFoundAccounts(int num) {
        foundAccountsBtn.setText("Accounts ( " + num + " )");
    }

    private void setNumFoundAccountsTxs(int num, String accountAddr) {
        if (foundAccountsTxsBtn != null)
            resultsBtn.removeButton(foundAccountsTxsBtn);
        try {
            foundAccountsTxsBtn = new CustomSubMenuButton();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        foundAccountsTxsBtn.setText("Account TXs ( " + num + " )");
        foundAccountsTxsBtn.setOnAction(e -> handleFoundAccountsTxs());
        resultsBtn.addButton(foundAccountsTxsBtn);

        ((Label) foundAccountsTxsPane.lookup("#labelAccountsTxs")).setText("Account " + accountAddr + " - TXs ( " + num + " )");
        if (settingBtn.isOpened()) {
            settingBtn.rotateIcon();
            settingBtn.deactivate();
        }
        if (resultsBtn.isOpened()) {
            resultsBtn.rotateIcon();
            resultsBtn.deactivate();
        }

        resetButtons();
        addResultsButton();
        foundAccountsTxsBtn.requestFocus();
        changeContent(foundAccountsTxsPane);
    }

    private void removeFoundAccountsTxs() {
        resultsBtn.removeButton(foundAccountsTxsBtn);
        ((Label) foundAccountsTxsPane.lookup("#labelAccountsTxs")).setText("");
        initialState();
    }


    private void addResultsButton() {
        if (hasExplorer() && hasTonHttpApi()) {
            resultsBtn.setLayoutY(370.0);
        } else if (hasExplorer() || hasTonHttpApi()) {
            resultsBtn.setLayoutY(320.0);
        } else {
            resultsBtn.setLayoutY(270.0);
        }
        if (!buttons.getChildren().contains(resultsBtn))
            buttons.getChildren().add(resultsBtn);
        removeCreateButton();
        resetButtons();
        resultsBtn.activate();
        if (settingBtn.isOpened())
            settingBtn.rotateIcon();
        if (!resultsBtn.isOpened()) {
            resultsBtn.rotateIcon();
        }
        foundBlocksBtn.requestFocus();
        changeContent(foundBlocksPane);

    }

    private void removeResultsButton() {
        buttons.getChildren().remove(resultsBtn);
        resetButtons();
        blocksBtn.activate();
        changeContent(blocksPane);
    }

    private void removeCreateButton() {
        if (createButton != null) {
            if (this.topButton.getChildren().contains(createButton)) {
                this.topButton.getChildren().remove(createButton);
                createButton = null;
            }
        }
    }

    private boolean isEditingSettings() {
        return contentPane.getChildren().contains(logsPane) ||
                contentPane.getChildren().contains(settingsValidatorsPane) ||
                contentPane.getChildren().contains(accountsKeysPane) ||
                contentPane.getChildren().contains(blockchainPane) ||
                contentPane.getChildren().contains(userInterfacePane) ||
                contentPane.getChildren().contains(aboutPane);
    }

    private boolean isViewingResults() {
        return contentPane.getChildren().contains(foundBlocksPane) ||
                contentPane.getChildren().contains(foundAccountsPane) ||
                contentPane.getChildren().contains(foundTxsPane) ||
                contentPane.getChildren().contains(foundAccountsTxsPane);
    }

    public void handle(CustomEvent event) {
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
                if (contentPane.getChildren().contains(blankPane))
                    initialState();
                break;
            case ACCOUNTS_TXS_REMOVE:
                removeFoundAccountsTxs();
                break;
            case SEARCH_SIZE_BLOCKS:
                setNumFoundBlocks(((CustomSearchEvent) event).getSize());
                break;
            case SEARCH_SIZE_ACCOUNTS:
                setNumFoundAccounts(((CustomSearchEvent) event).getSize());
                break;
            case SEARCH_SIZE_TXS:
                setNumFoundTxs(((CustomSearchEvent) event).getSize());
                break;
            case SEARCH_SIZE_ACCOUNTS_TXS:
                setNumFoundAccountsTxs(((CustomSearchEvent) event).getSize(),
                        ((CustomSearchEvent) event).getAccountAddr());
                break;
            case SEARCH_SHOW:
                addResultsButton();
                break;
            case SEARCH_REMOVE:
                removeResultsButton();
                break;
            case REFRESH:
                refresh(((CustomRefreshEvent) event).getViewType());
                break;
            case BLOCKCHAIN_READY:
                blockchainReady();
                break;
            case WALLETS_READY:
                walletsReady();
                break;
        }
    }

    private void blockchainReady() {
        if (noBlocksTransactions != null) {
            if (contentPane.getChildren().contains(noBlocksTransactions)) {
                switch (noBlocksTransactionsController.getViewType()) {
                    case BLOCKS:
                        changeContent(blocksPane);
                        break;
                    case TRANSACTIONS:
                        changeContent(transactionsPane);
                        break;
                }
            }
            noBlocksTransactions = null;
            noBlocksTransactionsController = null;

        }
        hasBlocks = true;
        hasTransactions = true;
    }

    private void walletsReady() {
        if (noBlocksTransactions != null) {
            if (contentPane.getChildren().contains(noBlocksTransactions)) {
                if (noBlocksTransactionsController.getViewType() == NoBlocksTransactionsController.ViewType.ACCOUNTS) {
                    changeContent(accountsPane);
                }
            }
            noBlocksTransactions = null;
            noBlocksTransactionsController = null;
        }
        hasAccounts = true;
    }

    private void refresh(NoBlocksTransactionsController.ViewType viewType) {
        switch (viewType) {
            case BLOCKS:
                refreshBlocks();
                break;
            case TRANSACTIONS:
                refreshTransactions();
                break;
            case ACCOUNTS:
                try {
                    refreshAccounts();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                break;
        }
    }

    private void initialState() {
        resetButtons();
        blocksBtn.activate();
        changeContent(blocksPane);
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

    private void notify(Notification note, final int show) {
        notificationStack.notify(note, show);
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

    public ObservableList<Node> getScrollButton() {
        return scrollButton.getChildren();
    }

    public ObservableList<Node> getCloseButton() {
        return closeButton.getChildren();
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

    public ObservableList<Node> getTonHttpApiPane() {
        return tonHttpApiPane.getChildren();
    }

    public ObservableList<Node> getFoundBlocksPane() {
        return foundBlocksPane.getChildren();
    }

    public ObservableList<Node> getFoundAccountsPane() {
        return foundAccountsPane.getChildren();
    }

    public ObservableList<Node> getFoundTxsPane() {
        return foundTxsPane.getChildren();
    }

    public ObservableList<Node> getFoundAccountsTxsPane() {
        return foundAccountsTxsPane.getChildren();
    }

}
