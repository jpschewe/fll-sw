<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<%@ page import="fll.web.admin.Tables" %>
  
<html>
  <head>
    <title>Table Labels</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/fll-sw.css'/>" />
  </head>

  <body>
    <h1>Table Labels</h1>
<% Tables.generatePage(out, application, session, request, response); %>

</body></html>
