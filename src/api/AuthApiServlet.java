package api;

import dao.ApiTokenDAO;
import dao.UserDAO;
import model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * POST /api/auth/login - the Flutter app signs in with the exact same accounts as the web
 * dashboard (reuses UserDAO.authenticate(), the same bcrypt-checking/lockout logic LoginServlet
 * uses) and gets back a bearer token instead of a session cookie, since mobile clients are
 * stateless. Exempted from ApiAuthFilter's token check (see that class) since there's obviously no
 * token yet at login time.
 */
@WebServlet("/api/auth/login")
public class AuthApiServlet extends HttpServlet {

    // 30 days: a mobile app's login is expected to "stick" far longer than a browser session -
    // there's no sliding-expiry/refresh-token scheme here, a token is just re-issued on next login
    // once it expires, which is an acceptable trade-off for this app's scale.
    private static final long TOKEN_VALIDITY_DAYS = 30;

    private final UserDAO userDAO = new UserDAO();
    private final ApiTokenDAO apiTokenDAO = new ApiTokenDAO();
    private final SecureRandom random = new SecureRandom();

    private static final class LoginRequest {
        String username;
        String password;
    }

    private static final class UserDto {
        final int userId;
        final String username;
        final String fullName;
        final String email;
        final String role;
        final Integer branchId;

        UserDto(User u) {
            this.userId = u.getUserId();
            this.username = u.getUsername();
            this.fullName = u.getFullName();
            this.email = u.getEmail();
            this.role = u.getRole();
            this.branchId = u.getBranchId();
        }
    }

    private static final class LoginResponse {
        final String token;
        final String expiresAt;
        final UserDto user;

        LoginResponse(String token, LocalDateTime expiresAt, User user) {
            this.token = token;
            this.expiresAt = expiresAt.toString();
            this.user = new UserDto(user);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LoginRequest body;
        try {
            body = JsonUtil.readJson(request, LoginRequest.class);
        } catch (Exception e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON body.");
            return;
        }
        if (body == null || body.username == null || body.password == null) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "username and password are required.");
            return;
        }

        try {
            User user;
            try {
                user = userDAO.authenticate(body.username, body.password);
            } catch (UserDAO.AccountLockedException e) {
                JsonUtil.writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Too many failed login attempts. Try again in about " + e.getMinutesRemaining() + " minute(s).");
                return;
            }
            if (user == null) {
                JsonUtil.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password.");
                return;
            }

            String token = generateToken();
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(TOKEN_VALIDITY_DAYS);
            apiTokenDAO.insert(user.getUserId(), token, expiresAt);

            JsonUtil.writeJson(response, new LoginResponse(token, expiresAt, user));
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }

    /** 256 bits of randomness, base64url-encoded - long/unguessable, unlike a UUID. */
    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
