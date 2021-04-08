<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.developer.importdb.SelectTournament.populateContext(session, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Select Tournament to import</title>
</head>

<body>
    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <form name="selectTournament" action="CheckTournamentExists">
        <p>Select a tournament to import</p>
        <select name="tournament">
            <c:forEach var="tournament" items="${tournaments}">

                <c:choose>
                    <c:when
                        test="${tournament.name == selectedTournament}">
                        <option value="${tournament.name}" selected>${tournament.description}
                            [ ${tournament.name} ]</option>
                    </c:when>
                    <c:otherwise>
                        <option value="${tournament.name}">${tournament.description}
                            [ ${tournament.name} ]</option>
                    </c:otherwise>

                </c:choose>

            </c:forEach>
        </select>
        <br />


        <!--  -->
        <input name="importSubjective" id="importSubjective"
            type="checkbox" ${importSubjectiveChecked} />
        <label for="importSubjective">Import Subjective Data</label>
        <br />

        <!--  -->
        <input name="importPerformance" id="importPerformance"
            type="checkbox" ${importPerformanceChecked} />
        <label for="importPerformance">Import Performance Data</label>
        <br />

        <!--  -->
        <input name="importFinalist" id="importFinalist" type="checkbox"
            ${importFinalistChecked} />
        <label for="importFinalist">Import Finalist Data</label>
        <br />

        <!--  -->
        <input name='submit_tournament' type='submit'
            value='Select Tournament' />
    </form>

    <p>
        If you don't want to import any of these tournaments you can <a
            href="<c:url value='/index.jsp'/>">return to the main
            index</a>.
    </p>


</body>
</html>
