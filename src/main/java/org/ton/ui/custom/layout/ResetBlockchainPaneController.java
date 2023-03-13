package org.ton.ui.custom.layout;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.ton.ui.custom.events.CustomEvent;
import org.ton.ui.custom.events.event.CustomActionEvent;
import org.ton.utils.MyLocalTonUtils;

import java.net.URL;
import java.util.ResourceBundle;

import static org.ton.ui.custom.events.CustomEventBus.emit;

@Slf4j
public class ResetBlockchainPaneController implements Initializable {

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    @FXML
    private void resetAction() {
        log.debug("do reset");
        emit(new CustomActionEvent(CustomEvent.Type.DIALOG_YES_NO_CLOSE));
        doReset();
    }

    private void doReset() {
        try {
            emit(new CustomActionEvent(CustomEvent.Type.SAVE_SETTINGS));
            Thread.sleep(100);
            if (SystemUtils.IS_OS_WINDOWS) {
                if (MyLocalTonUtils.doShutdown()) {
                    log.info("restarting: cmd /c start java -jar {} restart", MyLocalTonUtils.getMyPath());
                    Runtime.getRuntime().exec("cmd /c start java -jar " + MyLocalTonUtils.getMyPath() + " restart");
                    System.exit(0);
                }
            } else {
                if (MyLocalTonUtils.doShutdown()) {
                    // works on linux
                    log.info("restarting: java -jar {}", MyLocalTonUtils.getMyPath());
                    Runtime.getRuntime().exec("java -jar " + MyLocalTonUtils.getMyPath() + " restart");
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            log.error("Cannot restart MyLocalTon, error " + e.getMessage());
        }
    }
}
