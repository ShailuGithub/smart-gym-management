package api;

import dao.GymBranchDAO;
import model.GymBranch;
import model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** GET /api/branches - branches visible to the authenticated user: all for an ADMIN, just their own for a MANAGER/STAFF. */
@WebServlet("/api/branches")
public class BranchApiServlet extends HttpServlet {

    private final GymBranchDAO gymBranchDAO = new GymBranchDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = ApiSupport.currentUser(request);
        if (user == null) {
            ApiSupport.requireUser(response);
            return;
        }
        try {
            if (user.isAdmin()) {
                JsonUtil.writeJson(response, gymBranchDAO.findAll());
                return;
            }
            List<GymBranch> own = new ArrayList<>();
            if (user.getBranchId() != null) {
                GymBranch b = gymBranchDAO.findById(user.getBranchId());
                if (b != null) {
                    own.add(b);
                }
            }
            JsonUtil.writeJson(response, own);
        } catch (SQLException e) {
            JsonUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }
}
