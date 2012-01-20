package fll.web.playoff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.w3c.dom.Element;

import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Talk to client brackets in json.
 * 
 * @web.servlet name="AJAXBracketQuery"
 * @web.servlet-mapping url-pattern="/ajax/BracketQuery"
 */
public class AJAXBracketQueryServlet extends BaseFLLServlet {
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final DataSource datasource = SessionAttributes.getDataSource(session);
    try {
      final Connection connection = datasource.getConnection();
      final ServletOutputStream os = response.getOutputStream();
      if (request.getParameter("round") != null && request.getParameter("row") != null) {
        BracketData bd = constructBracketData(connection, session, application);
        JsonBracketData jsonbd = new JsonBracketData(bd);
        response.reset();
        response.setContentType("text/plain");
        os.print(jsonbd.getBracketLocationJson(Integer.parseInt(request.getParameter("round")), Integer.parseInt(request.getParameter("row"))));
      } else if (request.getParameter("multi") != null) {
        //Send off request to helpers
        handleMultipleQuery(parseInputToMap(request.getParameter("multi")), os, application, session, response, connection, datasource);
      } else {
        response.reset();
        response.setContentType("text/plain");
        os.print( "{\"_rmsg\": \"Error: No Params\"}");
      }
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }
  private Map<Integer, Integer> parseInputToMap(final String param) {
    String[] pairs = param.split("\\|");
    Map<Integer, Integer> pairedMap = new HashMap<Integer, Integer>();
    for (String pair : pairs) {
      pairedMap.put(Integer.parseInt(pair.split("\\-")[0]), Integer.parseInt(pair.split("\\-")[1]));
    }
    return pairedMap;
  }
  private void handleMultipleQuery(final Map<Integer, Integer> pairedMap,
                                   final ServletOutputStream os,
                                   final ServletContext application,
                                   final HttpSession session,
                                   final HttpServletResponse response,
                                   final Connection connection, 
                                   final DataSource datasource) {
    try {
      BracketData bd = constructBracketData(connection, session, application);
      JsonBracketData jsonbd = new JsonBracketData(bd);
      final Element rootElement = ApplicationAttributes.getChallengeDocument(application).getDocumentElement();
      final Element perfElement = (Element) rootElement.getElementsByTagName("Performance").item(0);  
      response.reset();
      response.setContentType("text/plain");
      os.print(jsonbd.getMultipleBracketLocationsJson(pairedMap, datasource, perfElement));
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    } catch (final ParseException e) {
      throw new RuntimeException(e);
    } 
  }
  private BracketData constructBracketData(Connection connection, HttpSession session, ServletContext application) {
    final String divisionKey = "playoffDivision";
    final String roundNumberKey = "playoffRoundNumber";
    final String displayNameKey = "displayName";
    final String displayName = SessionAttributes.getAttribute(session, displayNameKey, String.class);
    final String sessionDivision;
    final Number sessionRoundNumber;
    if (null != displayName) {
      sessionDivision = ApplicationAttributes.getAttribute(application, displayName+"_"+divisionKey, String.class);
      sessionRoundNumber = ApplicationAttributes.getAttribute(application, displayName+"_"+roundNumberKey, Number.class);
    } else {
      sessionDivision = null;
      sessionRoundNumber = null;
    }
    final String division;
    if (null != sessionDivision) {
      division = sessionDivision;
    } else if (null == ApplicationAttributes.getAttribute(application, divisionKey, String.class)) {
      try {
        final List<String> divisions = Queries.getEventDivisions(connection);
        if (!divisions.isEmpty()) {
          division = divisions.get(0);
        } else {
          throw new RuntimeException("No division specified and no divisions in the database!");
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    } else {
      division = ApplicationAttributes.getAttribute(application, divisionKey, String.class);
    }
    final int playoffRoundNumber;
    if (null != sessionRoundNumber) {
      playoffRoundNumber = sessionRoundNumber.intValue();
    } else if (null == ApplicationAttributes.getAttribute(application, roundNumberKey, Number.class)) {
      playoffRoundNumber = 1;
    } else {
      playoffRoundNumber = ApplicationAttributes.getAttribute(application, roundNumberKey, Number.class).intValue();
    }
    final int roundsLong = 3;
    final int rowsPerTeam = 4;
    final boolean showFinalsScores = false;
    final boolean onlyShowVerifiedScores = true;
    try {
      return new BracketData(connection, division, playoffRoundNumber, playoffRoundNumber+roundsLong-1, rowsPerTeam, showFinalsScores, onlyShowVerifiedScores);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}