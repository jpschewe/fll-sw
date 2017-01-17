package fll.web.ajax;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.util.JsonUtilities;
import fll.util.JsonUtilities.BracketLeafResultSet;
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

    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

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

        writeRefresh(response, writer);
        return;
      }

      final String multiParam = request.getParameter("multi");
      if (multiParam != null) {
        // Send off request to helpers
        final Map<DisplayInfo.H2HBracketDisplay, Map<Integer, Integer>> inputMap = parseInputToMap(displayInfo.getBrackets(),
                                                                                                   multiParam);

        final List<BracketLeafResultSet> allLeaves = new LinkedList<>();

        for (final Map.Entry<DisplayInfo.H2HBracketDisplay, Map<Integer, Integer>> mapEntry : inputMap.entrySet()) {
          final DisplayInfo.H2HBracketDisplay bracketInfo = mapEntry.getKey();
          final Map<Integer, Integer> pairedMap = mapEntry.getValue();

          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("Handling bracket: %s index: %d map: %s", bracketInfo.getBracket(),
                                       bracketInfo.getIndex(), pairedMap));
          }

          final List<BracketLeafResultSet> leaves = handleMultipleQuery(description, bracketInfo, pairedMap,
                                                                        connection);
          if (null == leaves) {
            writeRefresh(response, writer);
            return;
          } else {
            allLeaves.addAll(leaves);
          }

        }

        response.reset();
        response.setContentType("application/json");
        try {
          final ObjectMapper jsonMapper = new ObjectMapper();
          writer.write(jsonMapper.writeValueAsString(allLeaves));
        } catch (final JsonProcessingException e) {
          throw new RuntimeException(e);
        }

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
   * Write a refresh response.
   * You should return from the servlet immediately.
   */
  private void writeRefresh(final HttpServletResponse response,
                            final PrintWriter writer) {
    response.reset();
    response.setContentType("application/json");
    writer.print("{\"refresh\":\"true\"}");
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

  private Map<DisplayInfo.H2HBracketDisplay, Map<Integer, Integer>> parseInputToMap(final List<DisplayInfo.H2HBracketDisplay> allBrackets,
                                                                                    final String param) {

    final String[] lids = param.split("\\|");
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(String.format("parsing input: %s", Arrays.asList(lids)));
    }

    final Map<DisplayInfo.H2HBracketDisplay, Map<Integer, Integer>> retval = new HashMap<>();
    for (final String lid : lids) {
      final ImmutableTriple<Integer, Integer, Integer> parsed = BracketData.parseLeafId(lid);
      final int bracketIdx = parsed.left;
      final int row = parsed.middle;
      final int round = parsed.right;

      if(LOGGER.isTraceEnabled()) {
        LOGGER.trace(String.format("lid: '%s' bracketIdx: %d row: %d round: %d", bracketIdx, row, round));
      }
      if (bracketIdx < 0
          || bracketIdx >= allBrackets.size()) {
        LOGGER.error(String.format("Got bracket index out of range %d is outside [%d, %d]", bracketIdx, 0,
                                   allBrackets.size()));
        return null;
      }

      final DisplayInfo.H2HBracketDisplay bracket = allBrackets.get(bracketIdx);
      if (!retval.containsKey(bracket)) {
        retval.put(bracket, new HashMap<>());
      }

      final Map<Integer, Integer> pairedMap = retval.get(bracket);
      pairedMap.put(row, round);
    }
    return retval;
  }

  /**
   * @see JsonUtilities#generateJsonBracketInfo(String, Map, Connection,
   *      fll.xml.PerformanceScoreCategory, BracketData, boolean, boolean)
   * @throws SQLException
   */
  private List<BracketLeafResultSet> handleMultipleQuery(final ChallengeDescription description,
                                                         final H2HBracketDisplay bracketInfo,
                                                         final Map<Integer, Integer> pairedMap,
                                                         final Connection connection)
      throws SQLException {

    final int playoffRoundNumber = bracketInfo.getFirstRound();
    final int roundsLong = 3;
    final int rowsPerTeam = 4;
    final boolean showFinalsScores = false;
    final boolean showOnlyVerifiedScores = true;

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(String.format("handleMultipleQuery bracket: %s index: %d", bracketInfo.getBracket(),
                                 bracketInfo.getIndex()));
    }

    final BracketData bd = new BracketData(connection, bracketInfo.getBracket(), playoffRoundNumber, playoffRoundNumber
        + roundsLong - 1, rowsPerTeam, showFinalsScores, showOnlyVerifiedScores, bracketInfo.getIndex());

    return JsonUtilities.generateJsonBracketInfo(bracketInfo.getBracket(), pairedMap, connection,
                                                 description.getPerformance(), bd, showOnlyVerifiedScores,
                                                 showFinalsScores);
  }

}
