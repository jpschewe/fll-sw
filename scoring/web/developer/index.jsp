
  
<%@ include file="../WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="fll.xml.GenerateDB" %>
  
<%@ page import="org.w3c.dom.Document" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
%>

<%
final StringBuffer message = new StringBuffer();

if(null != request.getParameter("changeDatabase")
   || null != request.getParameter("resetDatabase")) {
  final String newDatabase;
  if(null != request.getParameter("changeDatabase")) {
    newDatabase = request.getParameter("database");
  } else {
    newDatabase = "fll";
  }
  application.setAttribute("database", newDatabase);
  
  //remove application variables that depend on the database connection
  application.removeAttribute("connection");
  application.removeAttribute("tournamentTeams");
  
  message.append("<i>Changed database to " + newDatabase + "</i><br>");
}

%>

<%-- include a second time in case we need to reconnect to the database --%>
<%@ include file="../WEB-INF/jspf/initializeApplicationVars.jspf" %>

  
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Developer Commands)</title>
  </head>

  <body background="../images/bricks1.gif" bgcolor="#ffffff" topmargin='4'>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Developer Commands)</h1>

    <p><font color='red'><b>This page is indended for developers only.  If you
    don't know what you're doing, LEAVE THIS PAGE!</b></font></p>

    <p><%=message.toString()%></p>
            
    <ul>
        
      <li>Current database is <%=application.getAttribute("database")%><br>
          <form action='index.jsp' method='post'><input type='text' name='database'>
          <input type='submit' name='changeDatabase' value='Change Database''>
          <input type='submit' name='resetDatabase' value='Reset to standard database'>
          </form>
      </li>

      <li><a href="../setup">Go to database setup</a></li>
        
    </ul>

<%@ include file="../WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
