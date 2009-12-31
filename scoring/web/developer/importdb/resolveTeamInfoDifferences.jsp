<%@ include file="/WEB-INF/jspf/init.jspf"%>

<c:if test="${empty dbimport}">
 <c:set var="message" scope="session">
  <p class='error'>Error <tt>dbimport</tt> parameter missing from session (from resolveTeamDifferences)</p>
 </c:set>
 <c:redirect url='/developer/index.jsp' />
</c:if>
<c:if test="${empty selectedTournament}">
 <c:set var="message" scope="session">
  <p class='error'>Error <tt>selectedTournament</tt> parameter missing from session (from resolveTeamDifferences)</p>
 </c:set>
 <c:redirect url='/developer/index.jsp' />
</c:if>
<c:if test="${empty teamDifferences}">
 <c:set var="message" scope="session">
  <p class='error'>Error <tt>teamDifferences</tt> parameter missing from session (from resolveTeamDifferences)</p>
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

<form name="resolveTeamDifferences" action="applyTeamChanges">
<p>There are differences in the team information between the
 source (imported) database and the destination database. 
 You need to choose to accept the value from the source database
 or the value from the destination database.</p>

<p>The tournament '${selectedTournament}' does not exist in the destination database, create it?</p>
	
 <c:forEach items="${teamDifferences}" var="difference" varStatus="loopStatus">
   <p>The ${difference.property} for team ${difference.teamNumber} differs<br/>
   <input type='radio' name='${loopStatus.index}' value='source' checked/> Use source value of '${difference.sourceValue}'<br/>
   <input type='radio' name='${loopStatus.index}' value='dest'/> Use dest value of '${difference.destValue}'<br/>   
   </p> 
 </c:forEach>

 
    <input name='submit' type='submit' value="Apply Changes"/>
</form>

<p>Return to the <a href="selectTournament.jsp">tournament selection page</a>.</p>

<%@ include file="/WEB-INF/jspf/footer.jspf"%>
</body>
</html>
