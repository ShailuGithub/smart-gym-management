package api;

import dao.ApiTokenDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** POST /api/auth/logout - invalidates the calling bearer token so it can't be reused. */
@WebServlet("/api/auth/logout")
public class LogoutApiServlet extends HttpServlet {

    private final ApiTokenDAO apiTokenDAO = new ApiTokenDAO();

    private static final class SuccessBody {
        final boolean success = true;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // ApiAuthFilter already validated this token exists and set apiUser - re-parse the header
        // here only to know which token row to delete (the filter doesn't hand the raw token along).
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null;
        if (token == null || token.isEmpty()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization: Bearer <token> header.");
            return;
        }
        try {
            apiTokenDAO.delete(token);
            JsonUtil.writeJson(response, new SuccessBody());
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }
}
