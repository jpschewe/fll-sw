<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<%@ page import="fll.web.admin.UploadTeams" %>

<%@ page import="java.sql.Connection" %>
  
<!-- query string <c:out value="${request.queryString}"/> -->
  
<%
final Connection connection = (Connection)application.getAttribute("connection");

if(null == session.getAttribute("columnSelectOptions")) {
  throw new RuntimeException("Error columnSelectOptions not set.  Please start back at administration page and go forward.");
}
final String errorMessage = (String)session.getAttribute("errorMessage");
if(null == errorMessage || "".equals(errorMessage)) {
  UploadTeams.copyFilteredTeams(connection, request);
}
%>
<html>
  <head>
    <title><x:out select="$challengeDocument/fll/@title"/> (Team Column Selection)</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Team Column Selection)</h1>

    <p>Do not use the forward and back buttons!  Use the supplied links/buttons.</p>

    <p>Select which columns in your data coorespond to the ones the database
    understands.  Highlighted columns are required, all others are optional.
    The datatype column specifies the type of data expected in this column.
    <b>If a number is expected and you specify a column with text that doesn't
    convert to a number it will be converted to 0.</b></p>

<%@ include file="/WEB-INF/jspf/sanitizeRules.jspf" %>
      
    <% if(null != errorMessage && !"".equals(errorMessage)) { %>
    <p><font color='red'><%=errorMessage%></font></p>
    <% } %>
      
    <form action="verifyTeams.jsp" method="POST" name="verifyTeams">
      <table border='1'>

      <%-- Note all form elements need to match the names of the columns in the database --%>
        <tr>
          <th>Database column</th>
          <th>Datatype - size</th>
          <th>You column</th>
        </tr>
          
        <tr bgcolor='yellow'>
          <td>TeamNumber</td>
          <td>Number</td>
          <td>
            <select name='TeamNumber'>
            <option value='' selected>None</option>
            <%=session.getAttribute("columnSelectOptions")%>
            </select>
          </td>
        </tr>

        <tr>
          <td>Team Name</td>
          <td>Text - 255 characters</td>
          <td>
            <select name='TeamName'>
            <option value='' selected>None</option>
            <%=session.getAttribute("columnSelectOptions")%>
            </select>
          </td>
        </tr>          

        <tr>
          <td>Organization</td>
          <td>Text - 255 characters</td>
          <td>
            <select name='Organization'>
            <option value='' selected>None</option>
            <%=session.getAttribute("columnSelectOptions")%>
            </select>
          </td>
        </tr>

        <tr>
          <td>Region</td>
          <td>Text - 16 characters</td>
          <td>
            <select name='Region'>
            <option value='' selected>None</option>
            <%=session.getAttribute("columnSelectOptions")%>
            </select>
          </td>
        </tr>

        <tr>
          <td>Division</td>
          <td>Text - 32 characters</td>
          <td>
            <select name='Division'>
            <option value='' selected>None</option>
            <%=session.getAttribute("columnSelectOptions")%>
            </select>
          </td>
        </tr>
          
        <tr>
          <td>Number of medals needed</td>
          <td>Number</td>
          <td>
            <select name='NumMedals'>
            <option value='' selected>None</option>
            <%=session.getAttribute("columnSelectOptions")%>
            </select>
          </td>
        </tr>
              
        <tr>
          <td colspan='2'><input type='submit' value='Next'></td>
        </tr>
          
      </table> 
    </form>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
