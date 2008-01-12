<%@ include file="/WEB-INF/jspf/init.jspf"%>

<c:choose>
  <c:when test="${param.submit_yes}">
    <%
      ImportDBDump.createTournament(session);
    %>
    <c:redirect url="${redirect_url}"/>
  </c:when>
  <c:when test="${param.submit_no}">
    <c:remove var="${selected_tournament}"/>
    <c:redirect url="selectTournament.jsp"/>
  </c:when>
  <c:when test="${empty param.tournament}">
   <c:set var="message" scope="session">
    <p class='error'>You need to go back and selct a tournament first</p>
   </c:set>
   <c:redirect url='selectTournament.jsp' />
  </c:choose>
  <c:otherwise>
    <c:set var="selected_tournament" scope="session" value="${param.tournament}"/>
  </c:otherwise>
</c:choose>
<%-- end if not proper workflow --%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title><x:out select="$challengeDocument/fll/@title" /></title>
</head>

<body>
<h1><x:out select="$challengeDocument/fll/@title" /></h1>

${message}
<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />

<c:set var="redirect_url" scope="session">
 <c:url value="CheckDifferences" />
</c:set>

<form action='checkTournamentExists'>
<%
if(Queries.getTournamentNames(connection).contains(request.getParameter("tournament))) {
  response.sendRedirect(response.encodeRedirectURL((String)session.getAttribute("redirect_url")));
} else {
%>
  <p>The tournament ${tournament} doesn't exist.  Would you like to create it?<br/>
  <input type='submit' name='submit_yes' value='Yes'><br/>
  <input type='submit' name='submit_no' value='No'><br/>
<%
}  
%>
</form>


<%@ include file="/WEB-INF/jspf/footer.jspf"%>
</body>
</html>
