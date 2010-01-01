<%@ include file="/WEB-INF/jspf/init.jspf"%>

<c:if test="${empty dbimport}">
 <c:set var="message" scope="session">
  <p class='error'>Error <tt>dbimport</tt> parameter missing from session (from resolveTournamentDifferences)</p>
 </c:set>
 <c:redirect url='/developer/index.jsp' />
</c:if>
<c:if test="${empty selectedTournament}">
 <c:set var="message" scope="session">
  <p class='error'>Error <tt>selectedTournament</tt> parameter missing from session (from resolveTournamentDifferences)</p>
 </c:set>
 <c:redirect url='/developer/index.jsp' />
</c:if>
<c:if test="${null == tournamentDifferences}">
 <c:set var="message" scope="session">
  <p class='error'>Error <tt>tournamentDifferences</tt> parameter missing from session (from resolveTournamentDifferences)</p>
 </c:set>
 <c:redirect url='/developer/index.jsp' />
</c:if>
<%-- end if not proper workflow --%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Resolve Team information differences</title>
</head>

<body>

${message}
<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />

<form name="resolveTournamentDifferences" action="CommitTournamentChanges">

<p>There are some teams in different tournaments in the two databases, please choose which tournament is correct. 
In most cases this should be the source database, otherwise the scores will not get imported.</p>
 <c:forEach items="${tournamentDifferences}" var="difference" varStatus="loopStatus">
   <p>The tournament for team ${difference.teamNumber} differs<br/>
   <input type='radio' name='${loopStatus.index}' value='source' checked/> Use source value of '${difference.sourceTournament}'<br/>
   <input type='radio' name='${loopStatus.index}' value='dest'/> Use dest value of '${difference.destTournament}'<br/>   
   </p> 
 </c:forEach>

    <input name='submit' type='submit' value="Apply Changes"/>
</form>

<p>Return to the <a href="selectTournament.jsp">tournament selection page</a>.</p>

<%@ include file="/WEB-INF/jspf/footer.jspf"%>
</body>
</html>
