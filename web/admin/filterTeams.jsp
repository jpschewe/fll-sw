<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<%@ page import="fll.web.admin.UploadTeams" %>

<%@ page import="fll.web.ApplicationAttributes" %>

<%@ page import="java.sql.Connection" %>

<c:if test="${not empty param.next}">
  <%-- next button was clicked --%>
  <c:redirect url="teamColumnSelection.jsp">
    <c:forEach items="${paramValues}" var="parameter">
      <c:forEach items="${parameter.value}" var="parameterValue">
        <c:param name="${parameter.key}" value="${parameterValue}"/>
        param: <c:out value="${parameter.key}"/> value: <c:out value="${parameterValue}"/>
      </c:forEach>
    </c:forEach>
   </c:redirect>
</c:if>
  
<%
final Connection connection = (Connection)application.getAttribute(ApplicationAttributes.CONNECTION);

if(null == session.getAttribute("columnSelectOptions")) {
  throw new RuntimeException("Error columnSelectOptions not set.  Please start back at administration page and go forward.");
}
%>
  
<html>
  <head>
    <title><x:out select="$challengeDocument/fll/@title"/> (Filter Teams)</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Filter Teams)</h1>

    <p>There are <%=UploadTeams.applyFilters(connection, request)%> teams currently
    selected.  Do not use the forward and back buttons!  Use the supplied
    links/buttons.</p>

<%@ include file="/WEB-INF/jspf/sanitizeRules.jspf" %>

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
          <td><input type='submit' name='apply' value='Apply Changes'></td>
          <td><input type='submit' name='next' value='Next'>  Click here when all appropriate filters have been applied.</td>
      </table>
    </form>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
