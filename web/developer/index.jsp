<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.xml.GenerateDB" %>
      
<c:if test="${not empty param.changeDatabase || not empty param.resetDatabase}">
  <c:if test="${not empty param.changeDatabase}" var="test">
    <c:set var="database" value="${param.changeDatabase}" scope="application" />
  </c:if>
  <c:if test="${not test}">
    <c:set var="database" value="fll" scope="application" />
  </c:if>

  <%-- just remove the database connections and they'll get recreated on the next page --%>
  <c:remove var="connection" />
  <c:remove var="datasource" />

  <c:set var="message">
    <i>Changed database to <c:out value="${database}"/></i><br>
  </c:set>
</c:if>
            
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Developer Commands)</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Developer Commands)</h1>

    <p><font color='red'><b>This page is indended for developers only.  If you
    don't know what you're doing, LEAVE THIS PAGE!</b></font></p>

    <p><c:out value="${message}"/></p>
            
    <ul>
        
      <li>Current database is <c:out value="${database}"/><br>
          <form action='index.jsp' method='post'><input type='text' name='database'>
          <input type='submit' name='changeDatabase' value='Change Database''>
          <input type='submit' name='resetDatabase' value='Reset to standard database'>
          </form>
      </li>

      <li><a href="<c:url value='/setup'/>">Go to database setup</a></li>
        
    </ul>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
