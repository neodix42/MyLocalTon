package org.ton.ui.custom.layout;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.ton.actions.MyLocalTon;
import org.ton.settings.MyLocalTonSettings;
import org.ton.ui.custom.events.CustomEvent;
import org.ton.ui.custom.events.event.CustomActionEvent;
import org.ton.utils.Utils;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.sun.javafx.PlatformUtil.isWindows;
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
            if (isWindows()) {
                if (Utils.doShutdown()) {
                    Thread.sleep(200);
                    MyLocalTon.getInstance().getSettings().setActiveNodes(new ConcurrentLinkedQueue<>());
                    MyLocalTon.getInstance().saveSettingsToGson();
                    deleteOldInstance();
                    Thread.sleep(200);
                    System.exit(0);
                }
            } else {
                if (Utils.doShutdown()) {
                    Thread.sleep(200);
                    MyLocalTon.getInstance().getSettings().setActiveNodes(new ConcurrentLinkedQueue<>());
                    MyLocalTon.getInstance().saveSettingsToGson();
                    deleteOldInstance();
                    Thread.sleep(200);
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            log.error("Cannot restart MyLocalTon, error " + e.getMessage());
        }
    }

    private void deleteOldInstance() throws InterruptedException {
        log.info("R E S E T T I N G");
        FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "MyLocalTonDB"));
        FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "genesis"));
        FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "node2"));
        FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "node3"));
        FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "node4"));
        FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "node5"));
        FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "node6"));
        FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "node7"));
        FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "templates"));
        FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "myLocalTon.log"));
        Thread.sleep(500);
        FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + "MyLocalTonDB"));
    }
}
