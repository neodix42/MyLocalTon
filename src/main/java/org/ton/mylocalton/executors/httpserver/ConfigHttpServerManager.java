package org.ton.mylocalton.executors.httpserver;

import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.actions.MyLocalTon;
import org.ton.mylocalton.ui.controllers.MainController;
import org.ton.mylocalton.settings.Node;

/** Manager class for the Config HTTP Server. */
@Slf4j
public class ConfigHttpServerManager {

  private static ConfigHttpServer httpServer;

  /**
   * Starts the Config HTTP Server.
   *
   * @param node The node containing the configuration
   * @param port The port to run the server on
   */
  public void startConfigHttpServer(Node node, int port, MainController mainController) {
    log.info("Starting Config HTTP Server on port {}", port);

    String configFilePath = node.getNodeGlobalConfigLocation();
    log.debug("Config file path: {}", configFilePath);

    httpServer = new ConfigHttpServer(port, configFilePath, mainController);
    httpServer.start();

    log.info("Config HTTP Server started at {}:{}", node.getPublicIp(), port);
    log.info(
        "http://127.0.0.1:"
            + MyLocalTon.getInstance().getSettings().getUiSettings().getSimpleHttpServerPort()
            + "/localhost.global.config.json");
  }

  /** Stops the Config HTTP Server. */
  public void stopConfigHttpServer() {
    if (httpServer != null) {
      httpServer.stop();
      httpServer = null;
    }
  }
}
