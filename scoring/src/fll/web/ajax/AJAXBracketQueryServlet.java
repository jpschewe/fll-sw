package fll.web.ajax;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.util.JsonUtilities;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.DisplayInfo;
import fll.web.DisplayInfo.H2HBracketDisplay;
import fll.web.SessionAttributes;
import fll.web.playoff.BracketData;
import fll.xml.ChallengeDescription;
import net.mtu.eggplant.util.ComparisonUtils;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Talk to client brackets in json.
 */
@WebServlet("/ajax/BracketQuery")
public class AJAXBracketQueryServlet extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final PrintWriter writer = response.getWriter();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final DisplayInfo displayInfo = DisplayInfo.getInfoForDisplay(application, session);

      // can't put types inside a session
      @SuppressWarnings("unchecked")
      final List<DisplayInfo.H2HBracketDisplay> storedBrackets = SessionAttributes.getAttribute(session, "brackets",
                                                                                                List.class);
      final boolean bracketsSame = compareBrackets(storedBrackets, displayInfo.getBrackets());
      if (!bracketsSame) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Session bracket information is different than application bracket information, forcing refresh");
        }

        response.reset();
        response.setContentType("application/json");
        writer.print("{\"refresh\":\"true\"}");
        return;
      }

      final DisplayInfo.H2HBracketDisplay bracketInfo = displayInfo.getBrackets().get(0); // HACK
                                                                                          // just
                                                                                          // for
                                                                                          // now

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Displaying bracket: "
            + bracketInfo.getBracket());
      }

      final String multiParam = request.getParameter("multi");
      final String division = bracketInfo.getBracket();
      if (multiParam != null) {
        // Send off request to helpers
        handleMultipleQuery(bracketInfo, parseInputToMap(multiParam), division, writer, application, response,
                            connection);
      } else {
        response.reset();
        response.setContentType("application/json");
        writer.print("{\"_rmsg\": \"Error: No Params\"}");
      }
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }
  }

  /**
   * Compare 2 sets of brackets
   * 
   * @return true if they are the same
   */
  private boolean compareBrackets(final List<H2HBracketDisplay> storedBrackets,
                                  final List<H2HBracketDisplay> brackets) {
    if (storedBrackets.size() != brackets.size()) {
      return false;
    }
    for (int i = 0; i < storedBrackets.size(); ++i) {
      final H2HBracketDisplay one = storedBrackets.get(i);
      final H2HBracketDisplay two = brackets.get(i);

      if (one.getFirstRound() != two.getFirstRound()) {
        return false;
      }
      if (!ComparisonUtils.safeEquals(one.getBracket(), two.getBracket())) {
        return false;
      }
    }

    return true;
  }

  private Map<Integer, Integer> parseInputToMap(final String param) {
    final String[] pairs = param.split("\\|");
    final Map<Integer, Integer> pairedMap = new HashMap<Integer, Integer>();
    for (final String pair : pairs) {
      final String[] pieces = pair.split("\\-");
      if (pieces.length >= 2) {
        final String one = pieces[0];
        final String two = pieces[1];
        pairedMap.put(Integer.parseInt(one), Integer.parseInt(two));
      }
    }
    return pairedMap;
  }

  private void handleMultipleQuery(final H2HBracketDisplay bracketInfo,
                                   final Map<Integer, Integer> pairedMap,
                                   final String division,
                                   final PrintWriter writer,
                                   final ServletContext application,
                                   final HttpServletResponse response,
                                   final Connection connection)
      throws SQLException {
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    final int playoffRoundNumber = bracketInfo.getFirstRound();
    final int roundsLong = 3;
    final int rowsPerTeam = 4;
    final boolean showFinalsScores = false;
    final boolean showOnlyVerifiedScores = true;

    final BracketData bd = new BracketData(connection, bracketInfo.getBracket(), playoffRoundNumber, playoffRoundNumber
        + roundsLong - 1, rowsPerTeam, showFinalsScores, showOnlyVerifiedScores);

    response.reset();
    response.setContentType("application/json");
    writer.print(JsonUtilities.generateJsonBracketInfo(division, pairedMap, connection, description.getPerformance(),
                                                       bd, showOnlyVerifiedScores, showFinalsScores));
  }

}
