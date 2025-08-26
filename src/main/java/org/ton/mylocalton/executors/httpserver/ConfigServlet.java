package org.ton.mylocalton.executors.httpserver;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * Servlet that serves the TON global config file.
 */
@Slf4j
public class ConfigServlet extends HttpServlet {
    private final String configFilePath;
    
    /**
     * Creates a new servlet instance.
     *
     * @param configFilePath The path to the config file to serve
     */
    public ConfigServlet(String configFilePath) {
        this.configFilePath = configFilePath;
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.debug("Received request for localhost.global.config.json");
        
        File file = new File(configFilePath);
        if (file.exists()) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
            
            try (InputStream in = new FileInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                log.debug("Served global.config.json successfully");
            } catch (IOException e) {
                log.error("Error serving config file", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println("Error reading config file: " + e.getMessage());
            }
        } else {
            log.error("Config file not found at path: {}", configFilePath);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().println("Config file not found");
        }
    }
}
