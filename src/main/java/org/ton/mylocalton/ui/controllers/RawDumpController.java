package org.ton.mylocalton.ui.controllers;

import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.actions.MyLocalTon;
import org.ton.mylocalton.enums.LiteClientEnum;
import org.ton.mylocalton.executors.liteclient.LiteClient;
import org.ton.mylocalton.utils.NotificationUtils;

@Slf4j
public class RawDumpController {

  @FXML private JFXButton showDumpBtn;

  private static Scene getScene(String finalStdout, Stage stage) {
    TextArea textArea = new TextArea();
    textArea.setText(finalStdout);
    textArea.setEditable(false);

    BorderPane borderPane = new BorderPane();
    borderPane.setCenter(textArea);

    Scene scene = new Scene(borderPane, 1000, 700);
    scene.setOnKeyPressed(
        keyEvent -> {
          if (keyEvent.getCode().equals(KeyCode.ESCAPE)) {
            stage.close();
          }
        });
    return scene;
  }

  public void showRawDump(ActionEvent actionEvent) {
    Node node = (Node) actionEvent.getSource();
    String userData = node.getUserData().toString();
    log.debug("clicked, userData {}", userData);

    String[] s = userData.split("#");
    String action = s[0];
    String data = s[1];

    Stage parentStage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();

    String msg = "Loading dump ...";
    NotificationUtils.showRawDumpNotification(parentStage, msg);

    new Thread(
            () -> {
              String stdout = null;
              try {
                switch (action) {
                  case "block":
                    log.debug("retrieving raw dump, command: dumpblock {}", data);
                    stdout =
                        LiteClient.getInstance(LiteClientEnum.GLOBAL)
                            .executeDumpblock(
                                MyLocalTon.getInstance().getSettings().getGenesisNode(), data);
                    if (stdout.contains("block contents is (")) {
                      stdout = stdout.substring(stdout.indexOf("block contents is ("));
                    }
                    break;
                  case "tx":
                    log.debug("retrieving raw dump, command: dumptrans {}", data);
                    stdout =
                        LiteClient.getInstance(LiteClientEnum.GLOBAL)
                            .executeDumptrans(
                                MyLocalTon.getInstance().getSettings().getGenesisNode(), data);
                    if (stdout.contains("transaction is (")) {
                      stdout = stdout.substring(stdout.indexOf("transaction is ("));
                    }
                    break;
                  case "account":
                    log.debug("retrieving raw dump, command: getaccount {}", data);
                    stdout =
                        LiteClient.getInstance(LiteClientEnum.GLOBAL)
                            .executeGetAccount(
                                MyLocalTon.getInstance().getSettings().getGenesisNode(), data);
                    if (stdout.contains("account state is (")) {
                      stdout = stdout.substring(stdout.indexOf("account state is ("));
                    }
                    break;
                  default:
                    log.warn("Unknown action for raw dump: {}", action);
                    break;
                }
              } catch (Exception ex) {
                log.error("Error retrieving raw dump", ex);
              }

              final String finalStdout = stdout;

              Platform.runLater(
                  () -> {
                    Stage stage = new Stage();
                    stage.initModality(Modality.NONE);
                    stage.initStyle(StageStyle.DECORATED);
                    stage.setTitle("Raw dump " + data);

                    Scene scene = getScene(finalStdout, stage);

                    stage.setScene(scene);
                    stage.show();
                  });
            })
        .start();
  }
}
