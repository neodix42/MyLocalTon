package org.ton.mylocalton.services;

import static java.util.Objects.nonNull;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.ton.mylocalton.actions.MyLocalTon;
import org.ton.mylocalton.settings.MyLocalTonSettings;
import org.ton.mylocalton.utils.MyLocalTonUtils;
import org.ton.mylocalton.ui.controllers.MainController;
import javafx.application.Platform;
import javafx.scene.control.Tab;

@Slf4j
public class ValidatorCreationService {

    public static class CreateNewNodeResult {
        public String nodeName;
        public Exception exception;
    }

    
    private final MyLocalTonSettings settings;
    private final MainController mainController;
    
    public ValidatorCreationService(MyLocalTonSettings settings, MainController mainController) {
        this.settings = settings;
        this.mainController = mainController;
    }
    
    public CreateNewNodeResult createNewValidator() {
        boolean inretactive = mainController != null;
        CreateNewNodeResult result = new CreateNewNodeResult();
        try {
          if (inretactive) {
            mainController.addValidatorBtn.setDisable(true);
          }
          org.ton.mylocalton.settings.Node node = MyLocalTonUtils.getNewNode();
    
          if (nonNull(node)) {
            log.info("creating validator {}", node.getNodeName());
            result.nodeName = node.getNodeName();
            node.setTonLogLevel(settings.getGenesisNode().getTonLogLevel());
    
            // delete unfinished or failed node creation
            FileUtils.deleteQuietly(
                new File(
                    MyLocalTonSettings.MY_APP_DIR + File.separator + node.getNodeName()));
    
            MyLocalTon.getInstance().createFullnode(node, true, true);
    
            if (inretactive) {
                Tab newTab = MyLocalTonUtils.getNewNodeTab();
                Platform.runLater(() -> mainController.validationTabs.getTabs().add(newTab));
            }
    
            settings.getActiveNodes().add(node.getNodeName());
            MyLocalTon.getInstance().saveSettingsToGson();
    
            String msg_cloned = "Validator " + node.getNodeName() + " has been cloned from genesis, now synchronizing and creating main wallet.";
            if (inretactive) {
              mainController.showDialogMessage("Completed", msg_cloned);
            } else {
              log.info(msg_cloned);
            }
    
            log.info(
                "Creating new validator controlling smart-contract (wallet) for node {}",
                node.getNodeName());
    
            MyLocalTon.getInstance().createValidatorControllingSmartContract(node);
    
            String msg_main_wallet = "Main wallet for validator " + node.getNodeName() + " has been successfully created.";
            if (inretactive) {
              mainController.addValidatorBtn.setDisable(false);
              mainController.showInfoMsg(msg_main_wallet, 5);
            } else {
              log.info(msg_main_wallet);
            }
          } else {
            String msg_limit_reached = "It is possible to have up to 6 additional validators. The first one is reserved, thus in total you may have 7 validators.";
            result.exception = new Exception(msg_limit_reached);
            if (inretactive) {
              mainController.showDialogMessage(
                  "The limit has been reached",
                  msg_limit_reached);
            } else {
              log.error(msg_limit_reached);
            }
          }
        } catch (Exception e) {
          result.nodeName = null;
          result.exception = e;
          log.error("Error creating validator: {}", e.getMessage());
          if (inretactive) {
            mainController.showErrorMsg("Error creating validator", 3);
          }
        } finally {
          if (inretactive) {
            mainController.addValidatorBtn.setDisable(false);
          }
        }
        return result;
    }
} 