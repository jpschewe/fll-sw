<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="java.sql.Connection"%>
<%@ page import="javax.sql.DataSource"%>

<%@ page import="fll.db.Queries"%>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="fll.web.WebUtils"%>

<%
	final DataSource datasource = ApplicationAttributes.getDataSource(application);
	final Connection connection = datasource.getConnection();
	pageContext.setAttribute("urls", WebUtils.getAllURLs(request));
%>


<html>
<head>
<title>FLL-SW</title>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
	<h1>${challengeDescription.title }</h1>

	<div class='status-message'>${message}</div>
	<%-- clear out the message, so that we don't see it again --%>
	<c:remove var="message" />


	<p>
		The current tournament is <b> <%=Queries.getCurrentTournamentName(connection)%></b>
	</p>

	<h2>Main Pages</h2>
	<ul>
		<li><a href="public">Public landing page</a> A list of pages that
			the public may want to visit if they are allowed to connect to the
			network.</li>

		<li><a href="scoreEntry/select_team.jsp">Score Entry</a> Follow
			this link on the performance score entry computers.</li>

		<li><a href='display.jsp'>Big Screen Display</a> Follow this link
			on the computer that's used to display scores with the projector.</li>

		<li><a href="subjective/Auth">Subjective Web application</a>
			Follow this link on the subjective judge's electronic devices.</li>


	</ul>

	<h2>Pages for the head computer person</h2>
	<ul>
		<li><a href="documentation/index.html">Documentation</a></li>

		<li><a href="troubleshooting/index.jsp">Troubleshooting</a></li>

		<li><a href="<c:url value='/setup'/>">Database setup</a> - Use
			this link to upload a saved database</li>

		<li><a href="admin/index.jsp">Administration</a></li>

		<li><a href="playoff/index.jsp">Head to head</a></li>

		<li><a href="report/index.jsp">Tournament reporting</a></li>
	</ul>


	<h2>Server addresses</h2>
	<ul>
		<c:forEach items="${urls}" var="url">
			<li><a href="${url }">${url }</a></li>
		</c:forEach>
	</ul>


	<h2>Other useful pages</h2>
	<ul>
		<li><a href="DoLogout">Log out</a> Log a computer out so that
			they need to enter the password again to change scores.</li>

		<li><a href='scoreboard/index.jsp'>Scoreboard</a></li>

		<li><a href='playoff/ScoresheetServlet'>Blank scoresheet for
				printing (PDF)</a></li>

		<c:forEach items="${challengeDescription.subjectiveCategories}"
			var="category">
			<li><a href="BlankSubjectiveSheet/${category.name}">Blank
					subjective sheet - ${category.title} (PDF)</a>
		</c:forEach>

		<li><a href='challenge.xml'>Challenge Descriptor</a></li>

		<li><a href="developer/index.jsp">Developer page</a></li>

		<li><a href="credits/credits.jsp">Credits</a></li>
	</ul>

</body>
</html>
