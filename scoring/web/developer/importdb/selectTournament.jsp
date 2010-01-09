<%@ include file="/WEB-INF/jspf/init.jspf"%>

<c:if test="${empty dbimport}">
 <c:set var="message" scope="session">
  <p class='error'>Error <tt>dbimport</tt> parameter missing from session (from selectTournament)</p>
 </c:set>
 <c:redirect url='/developer/index.jsp' />
</c:if>
<%-- end if not proper workflow --%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Select Tournament to import</title>
</head>

<body>
${message}
<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />

<sql:query dataSource="${dbimport}" var="query_result"
 scope="page">
  SELECT Name, Location from Tournaments ORDER BY Name
</sql:query>
<form name="selectTournament" action="CheckTournamentExists">
<p>Select a tournament to import</p>
<select name="tournament">
<c:forEach var="row" items="${query_result.rowsByIndex}">
 <option value="${row[0]}">${row[1]} [ ${row[0]} ]</option>
</c:forEach>
</select>
	
	<input name='submit' type='submit' value='Select Tournament'/>
</form>

<p>If you're don't want to import any of these tournaments you can 
<a href="<c:url value='/developer/index.jsp'/>">return to the developer index</a>.
</p>


</body>
</html>
