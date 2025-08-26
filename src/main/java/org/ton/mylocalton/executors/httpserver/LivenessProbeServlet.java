package org.ton.mylocalton.executors.httpserver;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.actions.MyLocalTon;


/**
 * Servlet that serves liveness probe.
 */
@Slf4j
public class LivenessProbeServlet extends HttpServlet {    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.debug("Received request for /live");
        response.setContentType("application/json");
        if (MyLocalTon.getInstance().getSettings().getActiveNodes().isEmpty() || MyLocalTon.getInstance().getSettings().getActiveNodes().size() < 1) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().println("UNAVAILABLE");

        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("OK");
        }
    }
}
