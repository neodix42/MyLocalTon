package org.ton.ui.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.ton.utils.MyLocalTonUtils;

@Slf4j
public class TxInfoController implements Initializable {

  @FXML
  private ScrollPane rootScrollPane;

  // -- Top-level fields --
  @FXML
  private Label origStatusLabel;
  @FXML
  private Label endStatusLabel;
  @FXML
  private Label accountAddrLabel;
  @FXML
  private Label outMsgsCountLabel;
  @FXML
  private Label prevTxHashLabel;
  @FXML
  private Label ltLabel;
  @FXML
  private Label prevTxLtLabel;
  @FXML
  private Label nowLabel;
  @FXML
  private Label oldHashLabel;
  @FXML
  private Label newHashLabel;

  // -- Containers for the inMsg / outMsgs fields --
  @FXML
  private VBox inMsgsContainer;
  @FXML
  private VBox outMsgsContainer;

  // -- totalFees --
  @FXML
  private Label totalFeesToncoinsLabel;
  @FXML
  private Label totalFeesOtherCurrenciesLabel;

  // -- description (type + storage + credit + compute + action + aborted/destroyed/creditFirst) --
  // description -> type
  @FXML
  private Label descTypeLabel;

  // description -> storage
  @FXML
  private Label descStorageFeesCollectedLabel;
  @FXML
  private Label descStorageFeesDueLabel;
  @FXML
  private Label descStorageStatusChangeLabel;

  // description -> credit
  @FXML
  private Label descCreditDueFeesCollectedLabel;
  @FXML
  private Label descCreditCreditToncoinsLabel;
  @FXML
  private Label descCreditCreditOtherCurrenciesLabel;

  // description -> compute
  @FXML
  private Label descComputeGasFeesLabel;
  @FXML
  private Label descComputeGasUsedLabel;
  @FXML
  private Label descComputeGasLimitLabel;
  @FXML
  private Label descComputeGasCreditLabel;
  @FXML
  private Label descComputeVmInitStateHashLabel;
  @FXML
  private Label descComputeVmFinalStateHashLabel;
  @FXML
  private Label descComputeAccountActivatedLabel;
  @FXML
  private Label descComputeMsgStateUsedLabel;
  @FXML
  private Label descComputeSuccessLabel;
  @FXML
  private Label descComputeExitArgLabel;
  @FXML
  private Label descComputeExitCodeLabel;
  @FXML
  private Label descComputeVmStepsLabel;
  @FXML
  private Label descComputeModeLabel;

  // description -> action
  @FXML
  private Label descActionSuccessLabel;
  @FXML
  private Label descActionValidLabel;
  @FXML
  private Label descActionNoFundsLabel;
  @FXML
  private Label descActionStatusChangeLabel;
  @FXML
  private Label descActionTotalFwdFeeLabel;
  @FXML
  private Label descActionTotalActionFeeLabel;
  @FXML
  private Label descActionResultArgLabel;
  @FXML
  private Label descActionResultCodeLabel;
  @FXML
  private Label descActionTotActionsLabel;
  @FXML
  private Label descActionSpecActionsLabel;
  @FXML
  private Label descActionSkippedActionsLabel;
  @FXML
  private Label descActionMsgsCreatedLabel;
  @FXML
  private Label descActionTotalMsgSizeCellsLabel;
  @FXML
  private Label descActionTotalMsgSizeBitsLabel;
  @FXML
  private Label descActionListHashLabel;

  // description -> aborted / destroyed / creditFirst
  @FXML
  private Label descAbortedLabel;
  @FXML
  private Label descDestroyedLabel;
  @FXML
  private Label descCreditFirstLabel;

  @FXML
  private TitledPane transactionTitledPane;
  @FXML
  private TitledPane totalFeesTitledPane;
  @FXML
  private TitledPane descriptionTitledPane;
  @FXML
  private TitledPane storageTitledPane;
  @FXML
  private TitledPane creditTitledPane;
  @FXML
  private TitledPane innerCreditTitledPane;
  @FXML
  private TitledPane computeTitledPane;
  @FXML
  private TitledPane actionTitledPane;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    addExpandedProperty(transactionTitledPane, rootScrollPane);
    addExpandedProperty(totalFeesTitledPane, rootScrollPane);
    addExpandedProperty(descriptionTitledPane, rootScrollPane);
    addExpandedProperty(storageTitledPane, rootScrollPane);
    addExpandedProperty(creditTitledPane, rootScrollPane);
    addExpandedProperty(innerCreditTitledPane, rootScrollPane);
    addExpandedProperty(computeTitledPane, rootScrollPane);
    addExpandedProperty(actionTitledPane, rootScrollPane);
  }

  public void initData(String txJson, Parent rawDumpContent) {
    try {
      Node currentContent = transactionTitledPane.getContent();

      if (currentContent instanceof Pane) {
        ((Pane) currentContent).getChildren().add(rawDumpContent);
      }

      JsonObject root = JsonParser.parseString(txJson).getAsJsonObject();

      // ---------------- Top-level fields ----------------
      origStatusLabel.setText(getStringSafe(root, "origStatus"));
      endStatusLabel.setText(getStringSafe(root, "endStatus"));
      accountAddrLabel.setText(getStringSafe(root, "accountAddr"));
      outMsgsCountLabel.setText(getStringSafe(root, "outMsgsCount"));
      prevTxHashLabel.setText(getStringSafe(root, "prevTxHash"));
      ltLabel.setText(getStringSafe(root, "lt"));
      prevTxLtLabel.setText(getStringSafe(root, "prevTxLt"));
      nowLabel.setText(getStringSafe(root, "now"));
      oldHashLabel.setText(getStringSafe(root, "oldHash"));
      newHashLabel.setText(getStringSafe(root, "newHash"));

      // ---------------- inMsgs (arr) ----------------
//            if (root.has("inMsg") && root.get("inMsg").isJsonArray()) {
//                JsonArray inMsgsArr = root.getAsJsonArray("inMsg");
//                for (JsonElement inMsgEl : inMsgsArr) {
//                    if (inMsgEl.isJsonObject()) {
//                        JsonObject inMsgObj = inMsgEl.getAsJsonObject();
//                        TitledPane pane = createMsgPane(inMsgObj, "inMsg");
//                        inMsgsContainer.getChildren().add(pane);
//                    }
//                }
//            }

      // ---------------- inMsg ----------------
      if (root.has("inMsg") && root.get("inMsg").isJsonObject()) {
        JsonObject inMsgObj = root.getAsJsonObject("inMsg");
        TitledPane pane = createMsgPane(inMsgObj, "inMsg");
        inMsgsContainer.getChildren().add(pane);
      }

      // ---------------- outMsgs (arr) ----------------
      if (root.has("outMsgs") && root.get("outMsgs").isJsonArray()) {
        JsonArray outMsgsArr = root.getAsJsonArray("outMsgs");
        for (JsonElement outMsgEl : outMsgsArr) {
          if (outMsgEl.isJsonObject()) {
            JsonObject outMsgObj = outMsgEl.getAsJsonObject();
            TitledPane pane = createMsgPane(outMsgObj, "ExtInMsg");
            outMsgsContainer.getChildren().add(pane);
          }
        }
      }

      // ---------------- totalFees ----------------
      if (root.has("totalFees") && root.get("totalFees").isJsonObject()) {
        JsonObject totalFeesObj = root.getAsJsonObject("totalFees");

        String amount = getStringSafe(totalFeesObj, "toncoins").isBlank()
            ? ""
            : MyLocalTonUtils.amountFromNano(getStringSafe(totalFeesObj, "toncoins"))
                .toPlainString();

        totalFeesToncoinsLabel.setText(amount);

        if (totalFeesObj.has("otherCurrencies")) {
          JsonArray oc = totalFeesObj.getAsJsonArray("otherCurrencies");
          totalFeesOtherCurrenciesLabel.setText(new Gson().toJson(oc));
        }
      }

      // ---------------- description ----------------
      if (root.has("description") && root.get("description").isJsonObject()) {
        JsonObject descObj = root.getAsJsonObject("description");

        // type
        descTypeLabel.setText(getStringSafe(descObj, "type"));

        // storage
        if (descObj.has("storage") && descObj.get("storage").isJsonObject()) {
          JsonObject storageObj = descObj.getAsJsonObject("storage");
          descStorageFeesCollectedLabel.setText(getStringSafe(storageObj, "feesCollected"));
          descStorageFeesDueLabel.setText(getStringSafe(storageObj, "feesDue"));
          descStorageStatusChangeLabel.setText(getStringSafe(storageObj, "statusChange"));
        }

        // credit
        if (descObj.has("credit") && descObj.get("credit").isJsonObject()) {
          JsonObject creditObj = descObj.getAsJsonObject("credit");
          descCreditDueFeesCollectedLabel.setText(getStringSafe(creditObj, "dueFeesCollected"));

          if (creditObj.has("credit") && creditObj.get("credit").isJsonObject()) {
            JsonObject credit2Obj = creditObj.getAsJsonObject("credit");
            String amount = getStringSafe(credit2Obj, "toncoins").isBlank()
                ? ""
                : MyLocalTonUtils.amountFromNano(getStringSafe(credit2Obj, "toncoins"))
                    .toPlainString();

            descCreditCreditToncoinsLabel.setText(amount);
            if (credit2Obj.has("otherCurrencies")) {
              descCreditCreditOtherCurrenciesLabel.setText(
                  new Gson().toJson(credit2Obj.getAsJsonArray("otherCurrencies"))
              );
            }
          }
        }

        // compute
        if (descObj.has("compute") && descObj.get("compute").isJsonObject()) {
          JsonObject computeObj = descObj.getAsJsonObject("compute");
          descComputeGasFeesLabel.setText(getStringSafe(computeObj, "gasFees"));
          descComputeGasUsedLabel.setText(getStringSafe(computeObj, "gasUsed"));
          descComputeGasLimitLabel.setText(getStringSafe(computeObj, "gasLimit"));
          descComputeGasCreditLabel.setText(getStringSafe(computeObj, "gasCredit"));
          descComputeVmInitStateHashLabel.setText(getStringSafe(computeObj, "vmInitStateHash"));
          descComputeVmFinalStateHashLabel.setText(getStringSafe(computeObj, "vmFinalStateHash"));
          descComputeAccountActivatedLabel.setText(getStringSafe(computeObj, "accountActivated"));
          descComputeMsgStateUsedLabel.setText(getStringSafe(computeObj, "msgStateUsed"));
          descComputeSuccessLabel.setText(getStringSafe(computeObj, "success"));
          descComputeExitArgLabel.setText(getStringSafe(computeObj, "exitArg"));
          descComputeExitCodeLabel.setText(getStringSafe(computeObj, "exitCode"));
          descComputeVmStepsLabel.setText(getStringSafe(computeObj, "vmSteps"));
          descComputeModeLabel.setText(getStringSafe(computeObj, "mode"));
        }

        // action
        if (descObj.has("action") && descObj.get("action").isJsonObject()) {
          JsonObject actionObj = descObj.getAsJsonObject("action");
          descActionSuccessLabel.setText(getStringSafe(actionObj, "success"));
          descActionValidLabel.setText(getStringSafe(actionObj, "valid"));
          descActionNoFundsLabel.setText(getStringSafe(actionObj, "noFunds"));
          descActionStatusChangeLabel.setText(getStringSafe(actionObj, "statusChange"));
          descActionTotalFwdFeeLabel.setText(getStringSafe(actionObj, "totalFwdFee"));
          descActionTotalActionFeeLabel.setText(getStringSafe(actionObj, "totalActionFee"));
          descActionResultArgLabel.setText(getStringSafe(actionObj, "resultArg"));
          descActionResultCodeLabel.setText(getStringSafe(actionObj, "resultCode"));
          descActionTotActionsLabel.setText(getStringSafe(actionObj, "totActions"));
          descActionSpecActionsLabel.setText(getStringSafe(actionObj, "specActions"));
          descActionSkippedActionsLabel.setText(getStringSafe(actionObj, "skippedActions"));
          descActionMsgsCreatedLabel.setText(getStringSafe(actionObj, "msgsCreated"));
          descActionTotalMsgSizeCellsLabel.setText(getStringSafe(actionObj, "totalMsgSizeCells"));
          descActionTotalMsgSizeBitsLabel.setText(getStringSafe(actionObj, "totalMsgSizeBits"));
          descActionListHashLabel.setText(getStringSafe(actionObj, "actionListHash"));
        }

        // aborted / destroyed / creditFirst
        descAbortedLabel.setText(getStringSafe(descObj, "aborted"));
        descDestroyedLabel.setText(getStringSafe(descObj, "destroyed"));
        descCreditFirstLabel.setText(getStringSafe(descObj, "creditFirst"));
      }

    } catch (Exception e) {
      log.error("Error parsing transaction JSON in TxInfoController", e);
    }
  }

  private TitledPane createMsgPane(JsonObject msgObj, String title) {
    TitledPane msgPane = createCollapsibleTitledPane(title, null);

    VBox msgRoot = new VBox(10);
    msgRoot.setPadding(new Insets(10));

    // -- srcAddr --
    if (msgObj.has("srcAddr") && msgObj.get("srcAddr").isJsonObject()) {
      JsonObject srcObj = msgObj.getAsJsonObject("srcAddr");
      TitledPane srcPane = createCollapsibleTitledPane("srcAddr", createAddrGrid(srcObj));
      msgRoot.getChildren().add(srcPane);
    }

    // -- destAddr --
    if (msgObj.has("destAddr") && msgObj.get("destAddr").isJsonObject()) {
      JsonObject destObj = msgObj.getAsJsonObject("destAddr");
      TitledPane destPane = createCollapsibleTitledPane("destAddr", createAddrGrid(destObj));
      msgRoot.getChildren().add(destPane);
    }

    // -- type, createdAt, createdLt, ihrDisabled, bounce, bounced --
    GridPane fieldsGrid = createGridPane();
    addKeyValueRow(fieldsGrid, 0, "type:", getStringSafe(msgObj, "type"));
    addKeyValueRow(fieldsGrid, 1, "createdAt:",
        getStringSafe(msgObj, "createdAt").isBlank()
            ? ""
            : MyLocalTonUtils.toLocal(Long.parseLong(getStringSafe(msgObj, "createdAt"))));

    addKeyValueRow(fieldsGrid, 2, "createdLt:", getStringSafe(msgObj, "createdLt"));
    addKeyValueRow(fieldsGrid, 3, "ihrDisabled:", getStringSafe(msgObj, "ihrDisabled"));
    addKeyValueRow(fieldsGrid, 4, "bounce:", getStringSafe(msgObj, "bounce"));
    addKeyValueRow(fieldsGrid, 5, "bounced:", getStringSafe(msgObj, "bounced"));

    msgRoot.getChildren().add(fieldsGrid);

    // -- value (toncoins + otherCurrencies) --
    if (msgObj.has("value") && msgObj.get("value").isJsonObject()) {
      JsonObject valueObj = msgObj.getAsJsonObject("value");

      GridPane valueGrid = createGridPane();
      String amount = getStringSafe(valueObj, "toncoins").isBlank()
          ? ""
          : MyLocalTonUtils.amountFromNano(getStringSafe(valueObj, "toncoins")).toPlainString();
      addKeyValueRow(valueGrid, 0, "toncoins:", amount);

      // otherCurrencies
      Label ocLabel = new Label("otherCurrencies:");
      valueGrid.add(ocLabel, 0, 1);

      if (valueObj.has("otherCurrencies")) {
        JsonArray arr = valueObj.getAsJsonArray("otherCurrencies");
        Label arrLabel = new Label(new Gson().toJson(arr));
        valueGrid.add(arrLabel, 1, 1);
      }

      TitledPane valuePane = createCollapsibleTitledPane("value", valueGrid);
      msgRoot.getChildren().add(valuePane);
    }

    // -- body (cells) --
    if (msgObj.has("body") && msgObj.get("body").isJsonObject()) {
      JsonObject bodyObj = msgObj.getAsJsonObject("body");

      GridPane bodyGrid = createGridPane();
      addKeyValueRow(bodyGrid, 0, "cells:", "");

      if (bodyObj.has("cells")) {
        JsonArray cellsArr = bodyObj.getAsJsonArray("cells");
        Label bodyLabel = new Label(new Gson().toJson(cellsArr));
        bodyGrid.add(bodyLabel, 1, 0);
      }

      TitledPane bodyPane = createCollapsibleTitledPane("body", bodyGrid);
      msgRoot.getChildren().add(bodyPane);
    }

    // -- init (code, data, library) --
    if (msgObj.has("init") && msgObj.get("init").isJsonObject()) {
      JsonObject initObj = msgObj.getAsJsonObject("init");

      GridPane initGrid = createGridPane();
      // code
      addKeyValueRow(initGrid, 0, "code:", "");
      if (initObj.has("code")) {
        JsonArray codeArr = initObj.getAsJsonArray("code");
        initGrid.add(new Label(new Gson().toJson(codeArr)), 1, 0);
      }

      // data
      addKeyValueRow(initGrid, 1, "data:", "");
      if (initObj.has("data")) {
        JsonArray dataArr = initObj.getAsJsonArray("data");
        initGrid.add(new Label(new Gson().toJson(dataArr)), 1, 1);
      }

      // library
      addKeyValueRow(initGrid, 2, "library:", "");
      if (initObj.has("library")) {
        JsonArray libArr = initObj.getAsJsonArray("library");
        initGrid.add(new Label(new Gson().toJson(libArr)), 1, 2);
      }

      TitledPane initPane = createCollapsibleTitledPane("init", initGrid);
      msgRoot.getChildren().add(initPane);
    }

    // -- fwdFee, importFee, ihrFee --
    GridPane feesGrid = createGridPane();
    addKeyValueRow(feesGrid, 0, "fwdFee:", getStringSafe(msgObj, "fwdFee"));
    addKeyValueRow(feesGrid, 1, "importFee:", getStringSafe(msgObj, "importFee"));
    addKeyValueRow(feesGrid, 2, "ihrFee:", getStringSafe(msgObj, "ihrFee"));

    msgRoot.getChildren().add(feesGrid);

    msgPane.setContent(msgRoot);
    return msgPane;
  }

  private TitledPane createCollapsibleTitledPane(String text, Node content) {
    TitledPane pane = new TitledPane(text, content);
    pane.setExpanded(false);
    pane.setCollapsible(true);

    addExpandedProperty(pane, content);

    return pane;
  }

  private GridPane createGridPane() {
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(5);
    return grid;
  }

  private void addKeyValueRow(GridPane grid, int row, String labelText, String valueText) {
    grid.add(new Label(labelText), 0, row);
    grid.add(new Label(valueText), 1, row);
  }

  private GridPane createAddrGrid(JsonObject addrObj) {
    GridPane addrGrid = createGridPane();

    addKeyValueRow(addrGrid, 0, "wc:", getStringSafe(addrObj, "wc"));
    addKeyValueRow(addrGrid, 1, "addr:", getStringSafe(addrObj, "addr"));

    return addrGrid;
  }

  private String getStringSafe(JsonObject obj, String key) {
    return (obj.has(key) && !obj.get(key).isJsonNull())
        ? obj.get(key).getAsString()
        : "";
  }

  private void addExpandedProperty(TitledPane pane, Node content) {
    pane.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
      if (isNowExpanded) {
        scrollToNode(content);
      }
    });
  }

  private void scrollToNode(Node node) {
    rootScrollPane.layout();

    Bounds nodeBounds = node.localToScene(node.getBoundsInLocal());
    Bounds viewportBounds = rootScrollPane.getViewportBounds();
    double contentHeight = rootScrollPane.getContent().getLayoutBounds().getHeight();

    double nodeTopInScene = nodeBounds.getMinY();
    double contentTopInScene = rootScrollPane.getContent()
        .localToScene(rootScrollPane.getContent().getBoundsInLocal())
        .getMinY();

    double nodeTop = nodeTopInScene - contentTopInScene;

    double wantedVvalue = nodeTop / (contentHeight - viewportBounds.getHeight());

    if (wantedVvalue < 0) {
      wantedVvalue = 0;
    }
    if (wantedVvalue > 1) {
      wantedVvalue = 1;
    }

    double start = rootScrollPane.getVvalue();
    double end = wantedVvalue;

    Timeline timeline = new Timeline(
        new KeyFrame(Duration.ZERO,
            new KeyValue(rootScrollPane.vvalueProperty(), start)),
        new KeyFrame(Duration.millis(300),
            new KeyValue(rootScrollPane.vvalueProperty(), end, Interpolator.EASE_BOTH))
    );

    timeline.play();
  }


}
