<%@ include file="/WEB-INF/jspf/init.jspf"%>

<c:if test="${not servletLoaded }">
	<c:redirect url="index.jsp" />
</c:if>
<c:remove var="servletLoaded" />

<html>
<head>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/style.jsp'/>" />
<title>Playoff's</title>
</head>

<body>
	<h1>
		<x:out select="$challengeDocument/fll/@title" />
		(Playoff menu)
	</h1>
	<ol>
		<li>If using the automatic table assignment feature for
			scoresheet generation, make certain to set up labels for each of your
			tables, available from the Admin page or by clicking <a
			href='<c:url value="/admin/tables.jsp"/>'>here</a>.
		</li>

		<li>Check to make sure all teams have scores entered for each
			seeding round.<br />
			<form name='check' action='check.jsp' method='get'>
				Select Division: <select id='check-division' name='division'>
					<c:forEach items="${playoffDivisions }" var="division">
						<option value='${division}'>${division}</option>
					</c:forEach>
				</select> <input type='submit' id='check_seeding_rounds'
					value='Check Seeding Rounds' />
			</form>
		<li><b>WARNING: Do not initialize playoff brackets for a
				division until all seeding runs for that division have been
				recorded!</b><br>
			<form name='initialize' action='initializebrackets.jsp' method='post'>
				Select Division: <select id='initialize-division' name='division'>
					<c:forEach items="${playoffDivisions }" var="division">
						<option value='${division}'>${division}</option>
					</c:forEach>
				</select><br> <input type='checkbox' name='enableThird' value='yes' />Check
				to enable 3rd/4th place brackets<br> <input type='submit'
					id='initialize_brackets' value='Initialize Brackets' />
			</form>
		<li>
			<form name='admin' action='adminbrackets.jsp' method='get'>
				<b>Printable Brackets</b><br /> Select Division: <select
					name='division'>
					<c:forEach items="${playoffDivisions }" var="division">
						<option value='${division}'>${division}</option>
					</c:forEach>
				</select> from round <select name='firstRound'>
					<c:forEach begin="1" end="${numPlayoffRounds }" var="numRounds">
						<c:choose>
							<c:when test="${numRounds == 1 }">
								<option value='${numRounds }' selected>${numRounds }</option>
							</c:when>
							<c:otherwise>
								<option value='${numRounds }'>${numRounds }</option>
							</c:otherwise>
						</c:choose>
					</c:forEach>
				</select> to
				<!-- numPlayoffRounds+1 == the column in which the 1st place winner is displayed  -->
				<select name='lastRound'>
					<c:forEach begin="2" end="${numPlayoffRounds+1 }" var="numRounds">
						<c:choose>
							<c:when test="${numRounds == numPlayoffRounds+1 }">
								<option value='${numRounds }' selected>${numRounds }</option>
							</c:when>
							<c:otherwise>
								<option value='${numRounds }'>${numRounds }</option>
							</c:otherwise>
						</c:choose>
					</c:forEach>
				</select> <input type='submit' id='display_printable_brackets'
					value='Display Brackets'>
			</form>
		</li>

		<li>
			<form name='printable' action='scoregenbrackets.jsp' method='get'>
				<b>Scoresheet Generation Brackets</b><br /> Select Division: <select
					name='division'>
					<c:forEach items="${playoffDivisions }" var="division">
						<option value='${division}'>${division}</option>
					</c:forEach>
				</select> from round <select name='firstRound'>
					<c:forEach begin="1" end="${numPlayoffRounds }" var="numRounds">
						<c:choose>
							<c:when test="${numRounds == 1 }">
								<option value='${numRounds }' selected>${numRounds }</option>
							</c:when>
							<c:otherwise>
								<option value='${numRounds }'>${numRounds }</option>
							</c:otherwise>
						</c:choose>
					</c:forEach>
				</select> to

				<!-- numPlayoffRounds+1 == the column in which the 1st place winner is displayed  -->
				<select name='lastRound'>
					<c:forEach begin="2" end="${numPlayoffRounds+1 }" var="numRounds">
						<c:choose>
							<c:when test="${numRounds == numPlayoffRounds+1 }">
								<option value='${numRounds }' selected>${numRounds }</option>
							</c:when>
							<c:otherwise>
								<option value='${numRounds }'>${numRounds }</option>
							</c:otherwise>
						</c:choose>
					</c:forEach>
				</select> <input type='submit' id='display_scoregen_brackets'
					value='Display Brackets'>
			</form>
		</li>

		<li><b>Scrolling Brackets</b> (as on big screen display)<br /> <a
			href="remoteMain.jsp">Display brackets</a><br /> Division and round
			must be selected from the big screen display <a
			href="<c:url value='/admin/remoteControl.jsp'/>">remote control</a>
			page.</li>
	</ol>


</body>
</html>
