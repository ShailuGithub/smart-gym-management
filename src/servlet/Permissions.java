package servlet;

import model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Small helper for enforcing per-user Create/Edit/Delete rights in CRUD servlets. */
final class Permissions {

    private Permissions() {
    }

    static User userOf(HttpServletRequest request) {
        return (User) request.getSession().getAttribute("user");
    }

    /** Redirects to redirectPath with a permError flag. Caller must return immediately after calling this. */
    static void deny(HttpServletRequest request, HttpServletResponse response, String redirectPath) throws IOException {
        String separator = redirectPath.contains("?") ? "&" : "?";
        response.sendRedirect(request.getContextPath() + redirectPath + separator + "permError=1");
    }
}
