<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.playoff.BracketData"%>
<%@ page import="fll.web.playoff.JsonBracketData"%>
<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="java.util.List"%>
<%@ page import="fll.db.Queries" %>
<%@ page import="java.sql.Connection" %>

<%
	/*
  application parameters
  playoffDivision - String for the division
  playoffRoundNumber - Integer for the playoff round number, counted from the 1st playoff round
   */

   final DataSource datasource = SessionAttributes.getDataSource(session);
   final Connection connection = datasource.getConnection();
  final int currentTournament = Queries.getCurrentTournament(connection);

  //response.setContentType("text/plain");

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

  final int numPlayoffRounds = Queries.getNumPlayoffRounds(connection, division);

  final BracketData bracketInfo = new BracketData(connection, division, playoffRoundNumber, playoffRoundNumber + 2, 4, false, true);
  
  final JsonBracketData jsonbd = new JsonBracketData(bracketInfo);
  
  String jsonResponse;
    //TODO: Send headers of text/plain.
  
  if ((request.getParameter("round") == null 
       || request.getParameter("row") == null)
       && request.getParameter("all") == null) {
    jsonResponse = "{\"_rmsg\": \"Error: No Params\"}";
  } else {
        jsonResponse = jsonbd.getBracketLocationJson(Integer.parseInt(request.getParameter("round")),
                                                Integer.parseInt(request.getParameter("row")));
      }
%>
<%=jsonResponse%>