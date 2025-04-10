package org.ton.graph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.actions.DynamicTreeLayout;
import org.ton.db.entities.TxEntity;

@RunWith(JUnit4.class)
public class GraphTest {

  // Static block to initialize JavaFX Toolkit once
  static {
    new JFXPanel(); // Initializes JavaFX Toolkit on first access
  }

  @Test
  public void testBuildGraph() throws Exception {
    // JavaFX must be initialized statically
    new JFXPanel();
    String txEntityJson =
        IOUtils.toString(
            Objects.requireNonNull(
                getClass()
                    .getResourceAsStream("/txEntities/txEntity_ext-in-and-3int-out-msg.json")),
            StandardCharsets.UTF_8);

    Gson gson = new GsonBuilder().create();
    TxEntity txEntity = gson.fromJson(txEntityJson, TxEntity.class);

    // Latch to wait for JavaFX thread to finish
    CountDownLatch latch = new CountDownLatch(1);

    Platform.runLater(
        () -> {
          try {
            // Let showTree handle its own window
            DynamicTreeLayout layout = new DynamicTreeLayout();
            layout.showTree(txEntity, txEntity.getTx());

            // Schedule closing after 5s
            new Thread(
                    () -> {
                      try {
                        Thread.sleep(50000);
                      } catch (InterruptedException ignored) {
                      }
                      Platform.runLater(
                          () -> {
                            // Close all open stages
                            for (Stage stage :
                                Stage.getWindows().stream()
                                    .filter(w -> w instanceof Stage)
                                    .map(w -> (Stage) w)
                                    .toList()) {
                              stage.close();
                            }
                            latch.countDown();
                          });
                    })
                .start();

          } catch (Exception e) {
            e.printStackTrace();
            latch.countDown(); // Finish test on error
          }
        });

    latch.await(); // Wait until GUI auto-closes
  }
}
