<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.admin.UploadTeams"%>
<%@ page import="fll.web.SessionAttributes"%>
<%@ page import="fll.web.ApplicationAttributes"%>

<%@ page import="javax.sql.DataSource"%>
<%@ page import="java.sql.Connection"%>

<!-- query string <c:out value="${request.queryString}"/> -->

<%
  final DataSource datasource = ApplicationAttributes.getDataSource(application);
  final Connection connection = datasource.getConnection();

  if (null == session.getAttribute("columnSelectOptions")) {
    throw new RuntimeException("Error columnSelectOptions not set.  Please start back at administration page and go forward.");
  }
  final String errorMessage = (String) session.getAttribute("errorMessage");
%>
<html>
<head>
<title>Team Column Selection</title>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
  <h1>Team Column Selection</h1>

  <div class='status-message'>${message}</div>
  <%-- clear out the message, so that we don't see it again --%>
  <c:remove var="message" />

  <p>Do not use the forward and back buttons! Use the supplied
    links/buttons.</p>

  <p>
    Match the software information on the left with the names of the columns
    from your data file on the right. The column names are the first row
    of your data file.<b>If a number is expected and you specify a
      column with text that doesn't convert to a number an error will be
      printed specifying the invalid value.</b> You can select the same
    column for multiple pieces of data.
  </p>

  <%@ include file="/WEB-INF/jspf/sanitizeRules.jspf"%>

  <%
    if (null != errorMessage
        && !"".equals(errorMessage)) {
  %>
  <p>
    <font color='red'><%=errorMessage%></font>
  </p>
  <%
    }
  %>

  <form
    action="verifyTeams.jsp"
    method="POST"
    name="verifyTeams">
    <table border='1'>

      <%-- Note all form elements need to match the names of the columns in the database --%>
      <tr>
        <th>Software information</th>
        <th>Datatype - size</th>
        <th>Column from datafile</th>
      </tr>

      <tr bgcolor='yellow'>
        <td>TeamNumber</td>
        <td>Number</td>
        <td><select name='TeamNumber'>
            <option
              value=''
              selected>None</option>
            <%=session.getAttribute("columnSelectOptions")%>
        </select></td>
      </tr>

      <tr>
        <td>Team Name</td>
        <td>Text - 255 characters</td>
        <td><select name='TeamName'>
            <option
              value=''
              selected>None</option>
            <%=session.getAttribute("columnSelectOptions")%>
        </select></td>
      </tr>

      <tr>
        <td>Organization</td>
        <td>Text - 255 characters</td>
        <td><select name='Organization'>
            <option
              value=''
              selected>None</option>
            <%=session.getAttribute("columnSelectOptions")%>
        </select></td>
      </tr>

      <tr>
        <td>Initial Tournament</td>
        <td>Text - 255 characters</td>
        <td><select name='tournament'>
            <option
              value=''
              selected>None</option>
            <%=session.getAttribute("columnSelectOptions")%>
        </select></td>
      </tr>

      <tr>
        <td>Award Group</td>
        <td>Text - 32 characters</td>
        <td><select name='event_division'>
            <option
              value=''
              selected>None</option>
            <%=session.getAttribute("columnSelectOptions")%>
        </select></td>
      </tr>

      <tr>
        <td>Judging Group</td>
        <td>Text - 32 characters</td>
        <td><select name='judging_station'>
            <option
              value=''
              selected>None</option>
            <%=session.getAttribute("columnSelectOptions")%>
        </select></td>
      </tr>


      <tr>
        <td colspan='3'><input
          type='submit'
          id='next'
          value='Next'></td>
      </tr>

    </table>
  </form>


</body>
</html>
