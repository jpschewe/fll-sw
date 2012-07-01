<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.ApplicationAttributes" %>
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
<%@ page import="com.google.gson.Gson" %>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Arrays"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.Iterator"%>

<%
  final DataSource datasource = ApplicationAttributes.getDataSource();
  final Connection connection = datasource.getConnection();
  final int currentTournament = Queries.getCurrentTournament(connection);
  
  //Get XML element paraphernalia here
  final Element rootElement = ApplicationAttributes.getChallengeDocument(application).getDocumentElement();
  final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);

  //Single score or multiple?
  if ((request.getParameter("run") == null || request.getParameter("team") == null) && request.getParameter("multi") != null) {
      //do multi
      List<String> ids = Arrays.asList(request.getParameter("multi").split("\\|"));
      List<Double> datalist = new ArrayList<Double>();
      for (Iterator<String> i = ids.iterator(); i.hasNext(); ) {
          String item = i.next();
          String[] params = item.split("-");
          final TeamScore teamScore = new DatabaseTeamScore(performanceElement, currentTournament, Integer.parseInt(params[0]), Integer.parseInt(params[1]), connection);
          if (ScoreUtils.computeTotalScore(teamScore) == Double.NaN) {
              datalist.add(-1.0);
          } else {
              datalist.add(ScoreUtils.computeTotalScore(teamScore));
          }
      }
      Gson gson = new Gson();
      out.print(gson.toJson(datalist));
  } else if (request.getParameter("run") != null && request.getParameter("team") != null) {
      final TeamScore teamScore = new DatabaseTeamScore(performanceElement, currentTournament, Integer.parseInt(request.getParameter("team")), Integer.parseInt(request.getParameter("run")), connection);
      out.print(ScoreUtils.computeTotalScore(teamScore));
  }
%>