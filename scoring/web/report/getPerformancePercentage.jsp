<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.report.PromptSummarizeScores"%>
<%
  if (PromptSummarizeScores.checkIfSummaryUpdated(response, application, session,
                                                  "/report/getPerformancePercentage.jsp")) {
    return;
  }
%>

<html>
<head>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/style.jsp'/>" />
<title>Get Performance Percentage</title>
</head>

<body>

  <form
    action="FinalComputedScores"
    method="post"
    target="_blank">

    <p>To advance to the next tournament a team's top performance
      score must be in the top X% of all teams top performance scores.
      That percentage may be specified here. Those scores meeting this
      requirement will show their performance scores in bold. If you
      don't know what this is or don't care, just leave the value at the
      default of 0%.</p>

    <!--  TODO issue:129 need to validate that this is a number -->
    <input
      type="text"
      name="percentage"
      value="0" />% <br /> <input type="submit" />

  </form>
  <br />

  <a href="index.jsp">Return to the Reporting index page.</a>

</body>
</html>
