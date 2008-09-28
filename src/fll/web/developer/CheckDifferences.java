package fll.web.developer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.mtu.eggplant.util.sql.SQLFunctions;
import fll.Utilities;

/**
 * Check for differences in team information between two databases.
 * 
 * @author jpschewe
 * @version $Revision$
 * 
 */
public class CheckDifferences extends HttpServlet {

  /**
   * 
   * @param request
   * @param response
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    final StringBuilder message = new StringBuilder();
    final HttpSession session = request.getSession();

    Connection memConnection = null;
    Statement memStmt = null;

    Utilities.loadDBDriver();
    try {
      final String tournament = request.getParameter("tournament");
      if(null == tournament) {
        message.append("You must select a tournament!");
      } else {
        // first check if the selected tournament exists in the destination database
        

      }
    } finally {
      SQLFunctions.closeStatement(memStmt);
      SQLFunctions.closeConnection(memConnection);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL((String)session.getAttribute("redirect_url")));
  }
}
