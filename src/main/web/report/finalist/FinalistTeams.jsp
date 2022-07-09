<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="HEAD_JUDGE" allowSetup="false" />

<%
fll.web.report.finalist.FinalistTeams.populateContext(application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/base.css'/>" />

<link rel='stylesheet' type='text/css'
    href='<c:url value="/scoreboard/score_style.css"/>' />


<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type="text/javascript">
  let prevScrollTimestamp = 0;
  const secondsBetweenScrolls = parseFloat("${scrollRate}");
  const pixelsToScroll = 1;

  function reload() {
    window.scrollTo(0, 0);
    location.reload(true);
  }

  function scrollDown(timestamp) {
    if (!elementIsVisible(document.getElementById("bottom"))) {
      const diff = timestamp - prevScrollTimestamp;
      if (diff >= secondsBetweenScrolls) {
        window.scrollBy(0, pixelsToScroll);
        prevScrollTimestamp = timestamp;
      }
      requestAnimationFrame(scrollDown);
    } else {
      // show the last scores for a bit and then reload
      setTimeout(reload, 3000);
    }
  }

  document.addEventListener("DOMContentLoaded", function() {
    <c:if test="${param.finalistTeamsScroll}">
    console.log("Starting scroll");
    requestAnimationFrame(scrollDown);
    </c:if>
  });
</script>

</head>

<body class='scoreboard'>
    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <h1>Teams in finalist judging</h1>

    <p>Please send a coach to the information desk to pickup your
        finalist schedule.</p>

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
