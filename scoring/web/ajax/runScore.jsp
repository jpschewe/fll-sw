<%@page import="fll.xml.PerformanceScoreCategory"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="fll.xml.PerformanceScoreCategory"%>
<%@ page import="javax.sql.DataSource"%>
<%@ page import="java.util.List"%>
<%@ page import="fll.db.Queries"%>
<%@ page import="java.sql.Connection"%>
<%@ page import="fll.util.ScoreUtils"%>
<%@ page import="fll.Team"%>
<%@ page import="fll.xml.ChallengeDescription"%>
<%@ page import="fll.web.playoff.TeamScore"%>
<%@ page import="fll.web.playoff.DatabaseTeamScore"%>
<%@ page import="com.google.gson.Gson"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Arrays"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.Iterator"%>

<%
	final DataSource datasource = ApplicationAttributes
			.getDataSource(application);
	final Connection connection = datasource.getConnection();
	final int currentTournament = Queries
			.getCurrentTournament(connection);

	//Get XML element paraphernalia here
	final ChallengeDescription rootElement = ApplicationAttributes
			.getChallengeDescription(application);
	final PerformanceScoreCategory performanceElement = rootElement
			.getPerformance();

	//Single score or multiple?
	if ((request.getParameter("run") == null || request
			.getParameter("team") == null)
			&& request.getParameter("multi") != null) {
		//do multi
		List<String> ids = Arrays.asList(request.getParameter("multi")
				.split("\\|"));
		List<Double> datalist = new ArrayList<Double>();
		for (Iterator<String> i = ids.iterator(); i.hasNext();) {
			String item = i.next();
			String[] params = item.split("-");
			final TeamScore teamScore = new DatabaseTeamScore(
					"Performance", currentTournament,
					Integer.parseInt(params[0]),
					Integer.parseInt(params[1]), connection);
			final double score = performanceElement.evaluate(teamScore);
			if (Double.isNaN(score)) {
				datalist.add(-1.0);
			} else {
				datalist.add(score);
			}
		}
		Gson gson = new Gson();
		out.print(gson.toJson(datalist));
	} else if (request.getParameter("run") != null
			&& request.getParameter("team") != null) {
		final TeamScore teamScore = new DatabaseTeamScore(
				"Performance", currentTournament,
				Integer.parseInt(request.getParameter("team")),
				Integer.parseInt(request.getParameter("run")),
				connection);
		final double score = performanceElement.evaluate(teamScore);
		out.print(score);
	}
%>