<%@ page errorPage="../errorHandler.jsp" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
  
<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>
  
<%@ page import="fll.web.admin.UploadTeams" %>

<%@ page import="org.w3c.dom.Document" %>
  
<%@ page import="java.sql.Connection" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final Connection connection = (Connection)application.getAttribute("connection");

if(null == session.getAttribute("columnSelectOptions")) {
  throw new RuntimeException("Error columnSelectOptions not set.  Please start back at administration page and go forward.");
}
%>
  
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Filter Teams)</title>
  </head>

  <body background="<c:url value="/images/bricks1.gif" />" bgcolor="#ffffff" topmargin='4'>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Filter Teams)</h1>

    <p>There are <%=UploadTeams.applyFilters(connection, request)%> currently
    selected.  Do not use the forward and back buttons!  Use the supplied
    links/buttons.</p>

<%@ include file="sanitizeRules.jsp" %>

    <form action="filterTeams.jsp" method="POST" name="filterTeams">
      <table border='1'>
        <tr>
          <th>Column to filter on</th>
          <th>Text to match in column.  % matches anything.  Text is matched without case.</th>
          <th>Delete filter</th>
        </tr>
<%
int parameterCount = 0; //count parameters separate in case they're empty
int filterCount = 0;
String filterColumn = request.getParameter("filterColumn" + parameterCount);
String filterText = request.getParameter("filterText" + parameterCount);
String filterDelete = request.getParameter("filterDelete" + parameterCount);
while(null != filterColumn) {
  if(null != filterText && !"".equals(filterText) && !"1".equals(filterDelete)) {
%>
        <tr>
          <td><%=filterColumn%> <input type='hidden' name='filterColumn<%=filterCount%>' value='<%=filterColumn%>'></td>
          <td><%=filterText%> <input type='hidden' name='filterText<%=filterCount%>' value='<%=filterText%>'></td>
          <td><input type='checkbox' name='filterDelete<%=filterCount%>' value='1'></td>
        </tr>
<%
    filterCount++;
  }
  parameterCount++;
  filterColumn = request.getParameter("filterColumn" + parameterCount);
  filterText = request.getParameter("filterText" + parameterCount);
  filterDelete = request.getParameter("filterDelete" + parameterCount);
}
%>
        <tr>
          <td>
            <select name='filterColumn<%=filterCount%>'>
              <%=session.getAttribute("columnSelectOptions")%>
            </select>
          </td>
          <td>
            <input type='text' name='filterText<%=filterCount%>'>
          </td>
          <td>&nbsp;</td>
        </tr>

        <tr>
          <td><input type='submit' value='Apply Changes'></td>
          <td><input type='submit' value='Next' onclick='document.filterTeams.action="teamColumnSelection.jsp"'>  Click here when all appropriate filters have been applied.</td>
      </table>
    </form>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
