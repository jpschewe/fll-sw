
  
<%@ page import="fll.Queries" %>
<%@ page import="fll.web.scoreEntry.Submit" %>

<%@ page import="java.sql.Connection" %>
  
<%@ page import="org.w3c.dom.Document" %>
    
<%@ include file="../WEB-INF/jspf/init.jspf" %>
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
 
final String lDeleteFlag = request.getParameter("delete");
final String lEditFlag = request.getParameter("EditFlag");

//insert into database here
final Connection connection = (Connection)application.getAttribute("connection");
final String sql;
if(null != lDeleteFlag) {
  sql = Queries.deletePerformanceScore(connection, request);
} else if("1".equals(lEditFlag)) {      
  sql = Queries.updatePerformanceScore(challengeDocument, connection, request);
} else {
  sql = Queries.insertPerformanceScore(challengeDocument, connection, request);
}
%>
  
    
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Submit Scores)</title>
  </head>

  <body>
    <p>SQL executed: <br>&nbsp;&nbsp;<%=sql%></p>
      
    <h1>Submit Scores</h1>
      <table border='1'>          
         <tr>
           <td>Team Number</td>
           <td><%=request.getParameter("TeamNumber")%></td>
         </tr>
         <tr>
           <td>Run Number</td>
           <td><%=request.getParameter("RunNumber")%></td>
         </tr>                
         <tr>
           <td>NoShow</td>
           <td><%=request.getParameter("NoShow")%></td>
         </tr>
         <tr>
           <td>Edit?</td>
           <td><%=lEditFlag%></td>
         </tr>
          <tr>
            <td>Delete?</td>
            <td><%=lDeleteFlag%></td>
          </tr>
         <%Submit.generateParameterTableRows(out, challengeDocument, request);%>
       </table>

       <a href="select_team.jsp">Normally you'd be redirected here</a>
      <% response.sendRedirect(response.encodeRedirectURL("select_team.jsp")); %>
<%@ include file="../WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
