package org.ton.mylocalton.executors.httpserver;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.actions.MyLocalTon;
import org.ton.mylocalton.services.ValidatorCreationService;
import org.ton.mylocalton.ui.controllers.MainController;

/**
 * Servlet that serves the add validator request.
 */
@Slf4j
public class AddValidatorServlet extends HttpServlet {
    private final MainController mainController;
    
    /**
     * Creates a new servlet instance.
     *
     * @param mainController The main controller instance
     */
    public AddValidatorServlet(MainController mainController) {
        this.mainController = mainController;
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.debug("Received request for add-validator");

        ValidatorCreationService validatorCreationService = new ValidatorCreationService(MyLocalTon.getInstance().getSettings(), mainController);
        ValidatorCreationService.CreateNewNodeResult result = validatorCreationService.createNewValidator();
        response.setContentType("application/json");
        if (result.nodeName != null) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("{\"status\": \"ok\", \"node\": \"" + result.nodeName + "\", \"message\": \"Validator created\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("{\"status\": \"error\", \"message\": \"" + result.exception.getMessage() + "\", \"node\": \"" + result.nodeName + "\"}");
        }
    }
}
