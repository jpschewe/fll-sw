<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.ScoreStandardization"%>
<%@ page import="fll.db.Queries"%>
<%@ page import="java.sql.Connection"%>
<%@ page import="fll.xml.ChallengeDescription"%>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="fll.web.SessionAttributes"%>
<%@ page import="javax.sql.DataSource"%>
<%@ page import="fll.web.report.PromptSummarizeScores"%>
<%@ page import="fll.web.WebUtils"%>

<%
  final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
  final DataSource datasource = ApplicationAttributes.getDataSource(application);
  final Connection connection = datasource.getConnection();
  final int currentTournament = Queries.getCurrentTournament(connection);

  ScoreStandardization.updateTeamTotalScores(connection, description, currentTournament);
  final String errorMsg = ScoreStandardization.checkDataConsistency(connection);
  pageContext.setAttribute("errorMsg", errorMsg);

  final String url = SessionAttributes.getAttribute(session, PromptSummarizeScores.SUMMARY_REDIRECT_KEY,
                                                    String.class);
  if (null != url) {
    WebUtils.sendRedirect(application, response, url);
    return;
  }
%>

<html>
<head>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/fll-sw.css'/>" />
<title>Summarize Scores</title>
</head>

<body>
  <h1>Summarize Scores</h1>

  <c:choose>
    <c:when test="${empty errorMsg }">
      <c:set
        var="message"
        scope="session">
        <p id='success'>
          <i>Successfully summarized scores</i>
        </p>
      </c:set>
      <c:redirect url="index.jsp"></c:redirect>
    </c:when>
    <c:otherwise>
      <font class='error'>${errorMsg}</font>
    </c:otherwise>
  </c:choose>

</body>
</html>
