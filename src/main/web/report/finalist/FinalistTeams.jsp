<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<%
fll.web.report.finalist.FinalistTeams.populateContext(application, pageContext);
%>

<html>
<head>
<title>Finalist teams</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/base.css'/>" />

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/big_screen.css'/>" />

<link rel='stylesheet' type='text/css' href="finalist_teams.css" />

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type="text/javascript">
  let prevScrollTimestamp = 0;
  const secondsBetweenScrolls = parseFloat("${scrollRate}");

  function startScrolling() {
    const topElement = document.getElementById("top");
    const bottomElement = document.getElementById("bottom");
    const scrollElement = window;
    const endPauseSeconds = 3;
    // if less than 1, then Chromebooks don't appear to scroll
    const pixelsToScroll = 2;

    startEndlessScroll(scrollElement, topElement, bottomElement,
        endPauseSeconds, pixelsToScroll, secondsBetweenScrolls)
  }

  document.addEventListener("DOMContentLoaded", function() {
    <c:if test="${param.finalistTeamsScroll}">
    console.log("Starting scroll");
    startScrolling();
    </c:if>
  });
</script>

</head>

<body class='scoreboard fll-sw-hide-cursor'>
    <h1>Teams in finalist judging</h1>

    <div id="top">&nbsp;</div>

    <table border='1'>
        <tr>
            <th>Team #</th>
            <th>Team Name</th>
            <th>Organization</th>
        </tr>

        <c:forEach items="${teams }" var="team">
            <tr>
                <td>${team.teamNumber}</td>
                <td>${team.teamName}</td>
                <td>${team.organization}</td>
            </tr>
        </c:forEach>

    </table>

    <div id="bottom">&nbsp;</div>
</body>
</html>
