<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Utilities" %>
<%@ page import="fll.db.Queries" %>

<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Connection" %>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Finalist Scheduling)</title>
  </head>

  <body>
    <p>${message}</p>

    <p>Would you like to define another category to schedule finalists for?<br/>
    <form action='FinalistSchedulerUI'>
      <input type="text" name="new-category" size="30"/>
      <input type='submit' name='create-category' value='Yes'/>
      <input type='submit' name='done' value='done'/>
    </form>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
