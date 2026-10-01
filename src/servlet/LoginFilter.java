package servlet;

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
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter("/*")
public class LoginFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        // getServletPath() (not a manual getRequestURI()/getContextPath() substring) - the
        // container already resolves this correctly regardless of context-root quirks
        // (spaces, encoding, trailing slash), which a hand-rolled substring is fragile against.
        String path = request.getServletPath();
        boolean isPublic = path.isEmpty() || path.equals("/") || path.equals("/index.jsp")
                || path.equals("/login") || path.startsWith("/assets/")
                || path.equals("/forgot-password") || path.equals("/reset-password")
                || path.startsWith("/api/");

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");

        if (isPublic) {
            chain.doFilter(req, resp);
            return;
        }
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        chain.doFilter(req, resp);
    }

    @Override
    public void destroy() {
    }
}
