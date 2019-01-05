<%@ page import="fll.web.admin.Tournaments"%>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
	fll.web.admin.Tournaments.populateContext(application, pageContext);
%>

<html>
<head>
<title>Edit Tournaments</title>

<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/fll-sw.css'/>" />

<script type="text/javascript"
	src="<c:url value='/extlib/jquery-1.11.1.min.js' />"></script>

<link rel="stylesheet"
	href="<c:url value='/extlib/jquery-ui-1.12.1/jquery-ui.min.css' />" />

<script type="text/javascript"
	src="<c:url value='/extlib/jquery-ui-1.12.1/jquery-ui.min.js' />"></script>

<script type="text/javascript" src="tournaments.js"></script>

</head>

<body>
	<h1>Edit Tournaments</h1>

	<div class='status-message'>${message}</div>
	<%-- clear out the message, so that we don't see it again --%>
	<c:remove var="message" />

	<p>
		<b>Tournament name's must be unique. Tournaments can be removed by
			erasing the name.</b>
	</p>

	<form action="<c:url value='/admin/StoreTournamentData' />"
		method="POST" name="tournaments">

		<input type="hidden" id="newTournamentId" value="${newTournamentId}" />

		<table border="1" id="tournamentsTable">
			<tr>
				<th>Date</th>
				<th>Name</th>
				<th>Description</th>
			</tr>

			<c:forEach items="${tournaments }" var="tournament"
				varStatus="loopStatus">
				<tr>
					<td><input type="hidden" name="key${loopStatus.index}"
						value="${tournament.tournamentID}" /> <input type="text"
						name="date${loopStatus.index}" id="date${loopStatus.index}"
						value="${tournament.dateString}" size="8" /></td>

					<td><input type="text" name="name${loopStatus.index}"
						id="name${loopStatus.index}" value="${tournament.name}"
						maxlength="128" size="32" /></td>

					<td><input type="text" name="description${loopStatus.index}"
						value="${tournament.description}" size="64" /></td>
				</tr>
			</c:forEach>
		</table>

		<input type="hidden" name="numRows" id="numRows"
			value="${fn:length(tournaments)}" />
		<!--  -->
		<button id='addRow'>Add Row</button>
		<!--  -->
		<input type='submit' name='commit' value='Finished'
			onclick='return checkTournamentNames()' />
	</form>

</body>
</html>
