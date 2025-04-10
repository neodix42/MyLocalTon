package org.ton.mylocalton.ui.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.utils.MyLocalTonUtils;

@Slf4j
public class MsgInfoController {

  @FXML private ScrollPane rootScrollPane;
  @FXML private TitledPane msgTitledPane;
  @FXML private Label srcAddrWcLabel;
  @FXML private Label srcAddrAddrLabel;
  @FXML private Label destAddrWcLabel;
  @FXML private Label destAddrAddrLabel;
  @FXML private Label typeLabel;
  @FXML private Label createdAtLabel;
  @FXML private Label createdLtLabel;
  @FXML private Label ihrDisabledLabel;
  @FXML private Label bounceLabel;
  @FXML private Label bouncedLabel;
  @FXML private Label valueToncoinsLabel;
  @FXML private Label valueOtherCurrenciesLabel;
  @FXML private Label bodyCellsLabel;
  @FXML private Label initCodeLabel;
  @FXML private Label initDataLabel;
  @FXML private Label initLibraryLabel;
  @FXML private Label fwdFeeLabel;
  @FXML private Label importFeeLabel;
  @FXML private Label ihrFeeLabel;

  public void initData(String msgJson, Parent rawDumpContent) {
    try {
      Node currentContent = msgTitledPane.getContent();

      if (currentContent instanceof Pane) {
        ((Pane) currentContent).getChildren().add(0, rawDumpContent);
      }

      JsonObject root = JsonParser.parseString(msgJson).getAsJsonObject();

      // srcAddr
      if (root.has("srcAddr")) {
        JsonObject srcObj = root.getAsJsonObject("srcAddr");
        srcAddrWcLabel.setText(srcObj.has("wc") ? srcObj.get("wc").getAsString() : "");
        srcAddrAddrLabel.setText(srcObj.has("addr") ? srcObj.get("addr").getAsString() : "");
      }

      // destAddr
      if (root.has("destAddr")) {
        JsonObject destObj = root.getAsJsonObject("destAddr");
        destAddrWcLabel.setText(destObj.has("wc") ? destObj.get("wc").getAsString() : "");
        destAddrAddrLabel.setText(destObj.has("addr") ? destObj.get("addr").getAsString() : "");
      }

      // type
      typeLabel.setText(root.has("type") ? root.get("type").getAsString() : "");

      // createdAt, createdLt, ihrDisabled, bounce, bounced
      createdAtLabel.setText(
          root.has("createdAt") ? MyLocalTonUtils.toLocal(root.get("createdAt").getAsLong()) : "");
      createdLtLabel.setText(root.has("createdLt") ? root.get("createdLt").getAsString() : "");
      ihrDisabledLabel.setText(
          root.has("ihrDisabled") ? root.get("ihrDisabled").getAsString() : "");
      bounceLabel.setText(root.has("bounce") ? root.get("bounce").getAsString() : "");
      bouncedLabel.setText(root.has("bounced") ? root.get("bounced").getAsString() : "");

      // value
      if (root.has("value")) {
        JsonObject valObj = root.getAsJsonObject("value");
        String amount =
            valObj.has("toncoins")
                ? MyLocalTonUtils.amountFromNano(valObj.get("toncoins").getAsString())
                    .toPlainString()
                : "";
        valueToncoinsLabel.setText(amount);
        if (valObj.has("otherCurrencies")) {
          JsonArray arr = valObj.getAsJsonArray("otherCurrencies");
          valueOtherCurrenciesLabel.setText(new Gson().toJson(arr));
        }
      }

      // body
      if (root.has("body")) {
        JsonObject bodyObj = root.getAsJsonObject("body");
        if (bodyObj.has("cells")) {
          JsonArray cellsArr = bodyObj.getAsJsonArray("cells");
          bodyCellsLabel.setText(new Gson().toJson(cellsArr));
        }
      }

      // init
      if (root.has("init")) {
        JsonObject initObj = root.getAsJsonObject("init");
        if (initObj.has("code")) {
          JsonArray codeArr = initObj.getAsJsonArray("code");
          initCodeLabel.setText(new Gson().toJson(codeArr));
        }
        if (initObj.has("data")) {
          JsonArray dataArr = initObj.getAsJsonArray("data");
          initDataLabel.setText(new Gson().toJson(dataArr));
        }
        if (initObj.has("library")) {
          JsonArray libArr = initObj.getAsJsonArray("library");
          initLibraryLabel.setText(new Gson().toJson(libArr));
        }
      }

      // fees
      fwdFeeLabel.setText(root.has("fwdFee") ? root.get("fwdFee").getAsString() : "");
      importFeeLabel.setText(root.has("importFee") ? root.get("importFee").getAsString() : "");
      ihrFeeLabel.setText(root.has("ihrFee") ? root.get("ihrFee").getAsString() : "");

    } catch (Exception e) {
      log.error("Error parsing JSON in MsgInfoController", e);
    }
  }
}
