<%@ include file="/WEB-INF/jspf/init.jspf"%>

<% fll.web.PublicIndex.populateContext(application, pageContext); %>

<html>
<head>
<title>FLL-SW</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
</head>

<body>
 <h1>${challengeDescription.title }</h1>

 ${message}
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 Below are listed the web pages that are available to the public.

 <ul>

  <li>${ScorePageText }</li>
  <li>Current Tournament -&gt; ${tournamentName }</li>

  <li><a href='<c:url value="/challenge.xml"/>'>Challenge
    Descriptor</a></li>

  <li><a href='<c:url value="/welcome.jsp"/>'>Welcome Page</a>
  <li><a href='<c:url value="/scoreboard/main.jsp" />'>Performance
    Scoreboard</a></li>

  <li><a href='<c:url value="/scoreboard/allteams.jsp"/>'>All
    Teams, All Performance Runs</a></li>
  <li><a href='<c:url value="/scoreboard/Last8"/>'>Last 8
    performance scores</a></li>
  <li><a href='<c:url value="/scoreboard/Top10"/>'>Top 10
    performance scores</a></li>

  <li><a href='<c:url value="/playoff/remoteMain.jsp"/>'>Playoff
    brackets that are currently on the big screen</a></li>

  <li>
   <form ACTION='<c:url value="/report/finalist/PublicFinalistSchedule"/>' METHOD='POST'>
    <select name='division'>
     <c:forEach var="division" items="${finalistDivisions }">
      <option value='${division }'>${division }</option>
     </c:forEach>
    </select> <input type='submit' value='Finalist Schedule - PDF' />
   </form>
  </li>


  <li><a href='<c:url value="/credits/credits.jsp"/>'>Credits</a></li>

 </ul>

</body>

</html>
