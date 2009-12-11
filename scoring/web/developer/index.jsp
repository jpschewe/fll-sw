<%@ include file="/WEB-INF/jspf/init.jspf"%>

<c:if
 test="${not empty param.changeDatabase || not empty param.resetDatabase}">
 <c:choose>
  <c:when test="${not empty param.changeDatabase}">
   <c:set var="database" value="${param.database}" scope="application" />
  </c:when>
  <c:otherwise>
   <%-- just remove the database variable and it'll get recreated on the redirect --%>
   <c:remove var="database" scope="application" />
  </c:otherwise>
 </c:choose>

 <%-- shutdown the database --%>
 <sql:update dataSource="${datasource}" sql="SHUTDOWN COMPACT"/>
 
 <%-- just remove the database connections and they'll get recreated on the redirect --%>
 <c:remove var="connection" />
 <c:remove var="datasource" />
 <c:remove var="challengeDocument" />
 <c:redirect url='index.jsp'>
  <c:param name="message">
      <p><i>Changed database to <c:out value="${database}" /></i></p>
  </c:param>
 </c:redirect>

</c:if>
<%-- end if a form submission --%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Developer Commands</title>
</head>

<body>
<h1><x:out select="$challengeDocument/fll/@title" /> (Developer
Commands)</h1>

<p><font color='red'><b>This page is intended for
developers only. If you don't know what you're doing, LEAVE THIS PAGE!</b></font></p>

${message}

<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />

<c:set var="redirect_url" scope="session">
 <c:url value="/developer/index.jsp" />
</c:set>

<ul>

 <li>Current database is <c:out value="${database}" /><br>
 <form action='index.jsp' method='post'><input type='text'
  name='database' size='50'> <input type='submit'
  name='changeDatabase' value='Change Database''> <input
  type='submit' name='resetDatabase' value='Reset to standard database'></form>
 </li>

 <li><a href="query.jsp">Do SQL queries and updates</a></li>

 <li><a href="<c:url value='/setup'/>">Go to database setup</a></li>

<li>
 <form id='import' action='importdb/ImportDBDump' method='post'
  enctype='multipart/form-data'>

 <p>Import data from a database dump into the current database.</p>
 <input type='file' size='32' name='dbdump'> <input
  type='submit' name='importdb' value='Import Database' /></form>
</li>

 <li>inside.test: <%=System.getProperty("inside.test")%> -- <%=Boolean.getBoolean("inside.test")%></li>
 </ul>

<%@ include file="/WEB-INF/jspf/footer.jspf"%>
</body>
</html>
