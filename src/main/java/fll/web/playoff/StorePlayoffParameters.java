package fll.web.playoff;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;

/**
 * Store playoff parameters from the index page.
 */
@WebServlet("/playoff/StorePlayoffParameters")
public class StorePlayoffParameters extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    final PlayoffSessionData data = SessionAttributes.getNonNullAttribute(session, PlayoffIndex.SESSION_DATA,
                                                                          PlayoffSessionData.class);

    final String bracketParam = request.getParameter("division");
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("playoff bracket: '"
          + bracketParam + "'");
    }
    data.setBracket(bracketParam);

    final String thirdFourthPlaceBrackets = request.getParameter("enableThird");
    if (null == thirdFourthPlaceBrackets) {
      data.setEnableThird(false);
    } else {
      data.setEnableThird(true);
    }

    session.setAttribute(PlayoffIndex.SESSION_DATA, data);

    response.sendRedirect(response.encodeRedirectURL("bracket_parameters.jsp"));

  }

}
