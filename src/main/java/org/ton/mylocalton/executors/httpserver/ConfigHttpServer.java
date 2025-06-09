package org.ton.mylocalton.executors.httpserver;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/** A simple HTTP server that serves the TON global config file. */
@Slf4j
public class ConfigHttpServer {
  static {
    Logger jettyLogger = Log.getLogger("org.eclipse.jetty");
    jettyLogger.setDebugEnabled(false);
  }

  private final int port;
  private final String configFilePath;
  private Server server;

  /**
   * Creates a new HTTP server instance.
   *
   * @param port The port to run the server on
   * @param configFilePath The path to the config file to serve
   */
  public ConfigHttpServer(int port, String configFilePath) {
    this.port = port;
    this.configFilePath = configFilePath;
  }

  /** Starts the HTTP server. */
  public void start() {
    server = new Server(port);

    server.setStopAtShutdown(true);
    server.setDumpAfterStart(false);
    server.setDumpBeforeStop(false);

    ServletHandler handler = new ServletHandler();
    server.setHandler(handler);

    ServletHolder holder = new ServletHolder(new ConfigServlet(configFilePath));
    handler.addServletWithMapping(holder, "/global.config.json");

    try {
      server.start();
      log.info("Config HTTP server started on port {}", port);
    } catch (Exception e) {
      log.error("Failed to start config HTTP server", e);
    }
  }

  /** Stops the HTTP server. */
  public void stop() {
    if (server != null) {
      try {
        server.stop();
        log.info("Config HTTP server stopped");
      } catch (Exception e) {
        log.error("Failed to stop config HTTP server", e);
      }
    }
  }
}
