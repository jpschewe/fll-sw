package fll.web.developer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.mtu.eggplant.util.sql.SQLFunctions;
import fll.Utilities;
import fll.web.BaseFLLServlet;

/**
 * Check for differences in team information between two databases.
 * 
 * @author jpschewe
 * @version $Revision$
 */
public final class CheckDifferences extends BaseFLLServlet {

  private CheckDifferences() {
    // no instances
  }
  
  protected void processRequest(final HttpServletRequest request, final HttpServletResponse response, final ServletContext application, final HttpSession session)throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();

    final Connection memConnection = null;
    final Statement memStmt = null;

    Utilities.loadDBDriver();
    try {
      final String tournament = request.getParameter("tournament");
      if (null == tournament) {
        message.append("You must select a tournament!");
      } else {
        // TODO first check if the selected tournament exists in the destination
        // database
      }
    } finally {
      SQLFunctions.closeStatement(memStmt);
      SQLFunctions.closeConnection(memConnection);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL((String) session.getAttribute("redirect_url")));
  }
}
