<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="java.sql.Connection" %>
<%@ page import="javax.sql.DataSource" %>

<%@ page import="fll.db.Queries" %>
<%@ page import="fll.web.SessionAttributes"%>

<%
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
%>

<html>
<head>
<title>FLL-SW</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
</head>

<body>
 <h1>
  <x:out select="$challengeDocument/fll/@title" />
 </h1>

 ${message}
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 Below are listed the web pages that are available to the public.

 <ul>

  <li>Current Tournament -&gt; <%=Queries.getCurrentTournamentName(connection)%></li>

  <li><a href='../challenge.xml'>Challenge Descriptor</a></li>

  <li><a href="credits/credits.jsp">Credits</a></li>

 </ul>

</body>

</html>
