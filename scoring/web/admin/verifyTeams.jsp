<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<%@ page import="fll.web.admin.UploadTeams" %>
<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="fll.web.ApplicationAttributes" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="java.sql.Connection" %>

<%
final DataSource datasource = ApplicationAttributes.getDataSource(application);
final Connection connection = datasource.getConnection();

if(null == session.getAttribute("columnSelectOptions")) {
	session.setAttribute(SessionAttributes.MESSAGE, 
			"<p class='error'>Error columnSelectOptions not set.</p>");
	response.sendRedirect(response.encodeRedirectURL("index.jsp"));
	return;
}
%>
  
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Verify Teams</title>
  </head>

  <body>
    <h1>Verify Teams</h1>

    <% if(UploadTeams.verifyTeams(connection, request, response, session, out)) { %>
    <c:set var="message" scope='session' value='<p id="success"><i>Teams successfully uploaded</i></p>'/>
    <c:redirect url="index.jsp"></c:redirect>
    <% } %>


  </body>
</html>
