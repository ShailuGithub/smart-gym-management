package api;

import dao.ApiTokenDAO;
import dao.UserDAO;
import model.User;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Token-based auth for the JSON API - the stateless-request equivalent of servlet.LoginFilter,
 * which only ever checks the web app's HttpSession (mobile clients have none). Reads an
 * "Authorization: Bearer <token>" header, resolves it via ApiTokenDAO, and stashes the User on the
 * request as "apiUser" for downstream api.* servlets to read (deliberately NOT the HttpSession -
 * each request re-authenticates independently, matching how a mobile client actually calls this).
 *
 * "/api/auth/login" is the one path under /api/* that must stay reachable without a token.
 * servlet.LoginFilter already exempts the whole "/api/" prefix from its own session check, so this
 * filter is the only gate /api/* requests pass through.
 *
 * Also handles CORS: the native Android/iOS app doesn't need it (no browser same-origin policy),
 * but Flutter Web does, since the app (e.g. http://localhost:8766) and this API
 * (http://localhost:8080) are different origins. Every response gets Access-Control-Allow-Origin
 * so the browser will actually read it, and an OPTIONS preflight is answered immediately (200, no
 * body, no auth check - the browser sends it before it has any token to attach) rather than being
 * treated as a real request.
 */
@WebFilter("/api/*")
public class ApiAuthFilter implements Filter {

    private final ApiTokenDAO apiTokenDAO = new ApiTokenDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Max-Age", "3600");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String path = request.getServletPath();
        if ("/api/auth/login".equals(path)) {
            chain.doFilter(req, resp);
            return;
        }

        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null;
        if (token == null || token.isEmpty()) {
            JsonUtil.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing Authorization: Bearer <token> header.");
            return;
        }

        try {
            ApiTokenDAO.TokenInfo info = apiTokenDAO.findByToken(token);
            if (info == null || info.isExpired()) {
                JsonUtil.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token.");
                return;
            }
            User user = userDAO.findById(info.getUserId());
            if (user == null) {
                JsonUtil.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token.");
                return;
            }
            request.setAttribute("apiUser", user);
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
            return;
        }

        chain.doFilter(req, resp);
    }

    @Override
    public void destroy() {
    }
}
