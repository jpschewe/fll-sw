package fll.web.playoff;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Store playoff parameters from the index page.
 */
@WebServlet("/playoff/StorePlayoffParameters")
public class StorePlayoffParameters extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

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
