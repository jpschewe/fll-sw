<%@ include file="/WEB-INF/jspf/init.jspf"%>

<c:if test="${empty dbimport}">
 <c:set var="message" scope="session">
  <p class='error'>Error <tt>dbimport</tt> parameter missing from session (from promptCreateTournament)</p>
 </c:set>
 <c:redirect url='/developer/index.jsp' />
</c:if>
<c:if test="${empty selectedTournament}">
 <c:set var="message" scope="session">
  <p class='error'>Error <tt>selectedTournament</tt> parameter missing from session (from promptCreateTournament)</p>
 </c:set>
 <c:redirect url='/developer/index.jsp' />
</c:if>
<%-- end if not proper workflow --%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Create tournament</title>
</head>

<body>

${message}
<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />

<form name="createTournament" action="CreateTournament">
<p>The tournament '${selectedTournament}' does not exist in the destination database, create it?</p>
	
	<input name='submit' type='submit' value="Yes"/>
    <input name='submit' type='submit' value="No"/>
</form>

<%@ include file="/WEB-INF/jspf/footer.jspf"%>
</body>
</html>
