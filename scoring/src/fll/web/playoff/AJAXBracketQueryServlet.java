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
          final String displayName = (String) session.getAttribute("displayName");
          final String division;
          final int playoffRoundNumber;
          if (session.getAttribute(displayName+"_"+"playoffDivision") == null) {
            //No special session attribute stuff
            division = (String) session.getAttribute("playoffDivision");
            playoffRoundNumber = ((Number) session.getAttribute("playoffRoundNumber")).intValue();
          } else {
            division = (String) session.getAttribute(displayName+"_"+"playoffDivision");
            playoffRoundNumber = ((Number) session.getAttribute(displayName+"_"+"playoffRoundNumber")).intValue();
          }
          JsonBracketData jsonbd = new JsonBracketData(new BracketData(connection, division, playoffRoundNumber, playoffRoundNumber + 2, 4, false, true));
          response.reset();
          response.setContentType("text/plain");
          os.print(jsonbd.getBracketLocationJson(Integer.parseInt(request.getParameter("round")), Integer.parseInt(request.getParameter("row"))));
      } else if (request.getParameter("multi") != null) {
        //Make row-round map
        String[] pairs = request.getParameter("multi").split("\\|");
        Map<Integer, Integer> pairedMap = new HashMap<Integer, Integer>();
        for (String pair : pairs) {
          pairedMap.put(Integer.parseInt(pair.split("\\-")[0]), Integer.parseInt(pair.split("\\-")[1]));
        }
        //JsonBD that request!
        final String divisionKey = "playoffDivision";
        final String roundNumberKey = "playoffRoundNumber";
        final String displayName = (String)session.getAttribute("displayName");
        final String sessionDivision;
        final Number sessionRoundNumber;
        if (null != displayName) {
          sessionDivision = (String) application.getAttribute(displayName
              + "_" + divisionKey);
          sessionRoundNumber = (Number) application.getAttribute(displayName
              + "_" + roundNumberKey);
        } else {
          sessionDivision = null;
          sessionRoundNumber = null;
        }
        final String division;
        if (null != sessionDivision) {
          division = sessionDivision;
        } else if (null == application.getAttribute(divisionKey)) {
          final List<String> divisions = Queries.getEventDivisions(connection);
          if (!divisions.isEmpty()) {
            division = divisions.get(0);
          } else {
            throw new RuntimeException("No division specified and no divisions in the database!");
          }
        } else {
          division = (String) application.getAttribute(divisionKey);
        }
        final int playoffRoundNumber;
        if (null != sessionRoundNumber) {
          playoffRoundNumber = sessionRoundNumber.intValue();
        } else if (null == application.getAttribute(roundNumberKey)) {
          playoffRoundNumber = 1;
        } else {
          playoffRoundNumber = ((Number) application.getAttribute(roundNumberKey)).intValue();
        }
        JsonBracketData jsonbd = new JsonBracketData(new BracketData(connection, division, playoffRoundNumber, playoffRoundNumber + 2, 4, false, true));
        final Element rootElement = ApplicationAttributes.getChallengeDocument(application).getDocumentElement();
        final Element perfElement = (Element) rootElement.getElementsByTagName("Performance").item(0);  
        response.reset();
        response.setContentType("text/plain");
        os.print(jsonbd.getMultipleBracketLocationsJson(pairedMap, datasource, perfElement));
      } else {
        os.print( "{\"_rmsg\": \"Error: No Params\"}");
      }
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } catch (final ParseException e) {
      throw new RuntimeException(e);
    }
  }
}