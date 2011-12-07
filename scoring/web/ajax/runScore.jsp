<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="fll.web.ApplicationAttributes" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="java.util.List"%>
<%@ page import="fll.db.Queries" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="fll.util.ScoreUtils"%>
<%@ page import="fll.Team"%>
<%@ page import="org.w3c.dom.Element" %>
<%@ page import="fll.web.playoff.TeamScore" %>
<%@ page import="fll.web.playoff.DatabaseTeamScore" %>

<%
  final DataSource datasource = SessionAttributes.getDataSource(session);
  final Connection connection = datasource.getConnection();
  final int currentTournament = Queries.getCurrentTournament(connection);
  
  //Get XML element paraphernalia here
  final Element rootElement = ApplicationAttributes.getChallengeDocument(application).getDocumentElement();
  final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);
  final TeamScore teamScore = new DatabaseTeamScore(performanceElement, currentTournament, Integer.parseInt(request.getParameter("team")), Integer.parseInt(request.getParameter("run")), connection);
  
  //TODO: Send headers of text/plain.
  out.print(ScoreUtils.computeTotalScore(teamScore));
%>