package kr.co.fanplace.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import kr.co.fanplace.setting.ErrorType;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErrorControllerImpl implements ErrorController
{
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model)
    {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        int statusCode = 500;
        if (statusObj != null)
        {
            try { statusCode = Integer.parseInt(statusObj.toString()); }
            catch (NumberFormatException ignored) {}
        }

        ErrorType type = ErrorType.fromStatus(statusCode);

        model.addAttribute("status", statusCode);
        model.addAttribute("title", type.getTitle());
        model.addAttribute("message", type.getMessage());

        return "error";
    }
}