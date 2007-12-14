<%@ include file="/WEB-INF/jspf/init.jspf"%>

<c:if test="${empty dbimport_url}">
 <c:set var="message" scope="session">
  <p class='error'>You cannot goto the selectTournament for dbimport
  without first importing a database dump</p>
 </c:set>
 <c:redirect url='index.jsp' />

</c:if>
<%-- end if not proper workflow --%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title><x:out select="$challengeDocument/fll/@title" /> (Select
Tournament to import)</title>
</head>

<body>
<h1><x:out select="$challengeDocument/fll/@title" /> (Select
Tournament to Import)</h1>

${message}
<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />

<c:set var="redirect_url" scope="session">
 <c:url value="selectTournament.jsp" />
</c:set>

<%
pageContext.setAttribute("driverName", Utilities.getDBDriverName());
%>
<sql:setDataSource scope="session" var="dbimport_datasource"
 url="${dbimport_url}" driver="${driverName}" />

<sql:query dataSource="${dbimport_datasource}" var="query_result"
 scope="page">
  SELECT Name, Location from Tournaments ORDER BY Name
</sql:query>
<form name="selectTournament" action="CheckDifferences">
<p>Select a tournament to import</p>
<select name="tournament">
<c:forEach var="row" items="${query_result.rowsByIndex}">
 <option value="${row[0]}">${row[1]} [ ${row[0]} ]</option>
</c:forEach>
</select>
	
	<submit/>
</form>


<%@ include file="/WEB-INF/jspf/footer.jspf"%>
</body>
</html>
