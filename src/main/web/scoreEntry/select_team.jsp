<!DOCTYPE html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="REF" allowSetup="false" />

<%
fll.web.scoreEntry.SelectTeam.populateContext(application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Score Entry [Select Team]</title>

<c:choose>
    <c:when test="${not empty scoreEntrySelectedTable}">
        <style type='text/css'>
SELECT {
    font-size: x-large;
    font-weight: bold;
    background: black;
    color: #e0e0e0;
    font-weight: bold;
}
</style>
    </c:when>
    <c:otherwise>
        <style type='text/css'>
SELECT {
    font-weight: bold;
    background: black;
    color: #e0e0e0;
}
</style>
    </c:otherwise>
</c:choose>
<style type='text/css'>
OPTION {
    color: #e0e0e0;
}

.dark_bg {
    font-weight: bold;
    background-color: black;
    color: #e0e0e0;
}

.delete_button {
    margin-left: 40px;
    margin-top: 10px;
}
</style>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js' />"></script>
<script type='text/javascript' src="<c:url value='/js/fll-storage.js'/>"></script>
<script type='text/javascript' src="scoreEntryModule.js"></script>

<script type='text/javascript'>
  // use var instead of const so that the variables are available globally
  var scoreEntrySelectedTable = "${scoreEntrySelectedTable}";
  var teamSelectData = JSON.parse('${teamSelectDataJson}');
  var tabletMode = Boolean(scoreEntrySelectedTable);
</script>

<script type='text/javascript' src="select_team.js"></script>

</head>
<body>

    <!-- top info bar -->
    <table width="100%" border="0" cellpadding="0" cellspacing="0">
        <tr>
            <td align="center" valign="middle" bgcolor="#e0e0e0"
                colspan='3'>
                <table border="1" cellpadding="0" cellspacing="0"
                    width="100%">
                    <tr align="center" valign="middle">
                        <td>

                            <table border="0" cellpadding="5"
                                cellspacing="0" width="90%">
                                <tr>
                                    <td valign="middle" align="center">
                                        <font face="Arial" size="4">${challengeDescription.title }</font>
                                        <br>
                                        <font face="Arial" size="2">Score
                                            Card Entry and Review Page</font>
                                    </td>
                                </tr>
                            </table>

                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>



    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <a class="wide" href="choose-table.jsp"> <c:choose>
            <c:when test="${not empty scoreEntrySelectedTable}">
Entering scores for table ${scoreEntrySelectedTable}. Teams are sorted in schedule order with this table first.
</c:when>
            <c:otherwise>
Entering scores for all tables. Teams are sorted in schedule order.
</c:otherwise>
        </c:choose> Click here to change the table that scores are being entered
        for.
    </a>

    <div>
        <a class="wide"
            href="<c:url value='/scoreEntry/scoreEntry.jsp?tablet=true&practice=true&showScores=false'/>">Practice
            round score entry</a>
    </div>

    <div id='stored-values'></div>

    <c:if test="${empty scoreEntrySelectedTable}">
        <p>Use the browser search, ctrl-f, to find teams by name,
            number or organization</p>
    </c:if>

    <table id='container'>
        <!-- outer table -->

        <colgroup>
            <c:choose>
                <c:when test="${not empty scoreEntrySelectedTable}">
                    <!-- tablet entry -->
                    <col width="100%" />
                </c:when>
                <c:otherwise>
                    <!-- computer entry -->
                    <col width="50%" />
                    <col width="50%" />
                </c:otherwise>
            </c:choose>
        </colgroup>

        <tr>
            <td>
                <form action="scoreEntry.jsp" method="POST"
                    name="selectTeam">
                    <table>
                        <!-- left table -->

                        <tr align='left' valign='top'>
                            <!-- pick team from a list -->
                            <td>
                                <c:if
                                    test="${empty scoreEntrySelectedTable}">
                                    <div>Sort by selected table
                                        and then:</div>
                                    <div>
                                        <button id='sort-next-perf'
                                            type='button'>Next
                                            Performance</button>
                                        <button id='sort-team-name'
                                            type='button'>Team
                                            Name</button>
                                        <button id='sort-team-number'
                                            type='button'>Team
                                            Number</button>
                                        <button id='sort-organization'
                                            type='button'>Organization</button>
                                    </div>
                                </c:if>

                                <br>
                                <span style="vertical-align: top">Select
                                    team to enter score for:</span>
                                <c:if
                                    test="${empty scoreEntrySelectedTable}">
                                    <br />
                                </c:if>
                                <select size='20' id='select-teamnumber'
                                    name='TeamNumber'
                                    ondblclick='selectTeam.submit()'>
                                </select>
                            </td>
                        </tr>
                        <c:if test="${empty scoreEntrySelectedTable}">

                            <tr>
                                <td>
                                    <table border='1'>
                                        <tr>
                                            <!-- check to edit -->
                                            <td align='left'
                                                valign='bottom'>
                                                <input type="checkbox"
                                                    name='EditFlag'
                                                    id='EditFlagLeft'
                                                    value="true"
                                                    onclick="editFlagBoxClicked()" />
                                                <b>
                                                    <label
                                                        for="EditFlagLeft">Correct
                                                        or double-check
                                                        this score</label>
                                                </b>
                                            </td>
                                        </tr>
                                        <tr>
                                            <!-- pick run number -->
                                            <td align='left'>
                                                <b>
                                                    <span
                                                        id='select_number_text'>Select
                                                        Run Number for
                                                        editing</span>
                                                </b>
                                                <select name='RunNumber'
                                                    disabled='disabled'>
                                                    <option value='0'>Last
                                                        Run</option>
                                                    <c:forEach
                                                        var="index"
                                                        begin="1"
                                                        end="${maxRunNumber}">
                                                        <option
                                                            value='${index }'>${index }</option>
                                                    </c:forEach>
                                                </select>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </c:if>
                        <tr>
                            <!-- submit button -->
                            <td align='left'>
                                <!--<font face='arial' size='4'><b>Submit</b></font>-->
                                <input class='dark_bg' type="submit"
                                    value="Enter score"
                                    id='enter_submit'>
                            </td>
                        </tr>

                    </table>
                </form>
            </td>
            <!-- left table -->

            <c:if test="${empty scoreEntrySelectedTable}">
                <%-- no verification when using tablet for entry --%>

                <td valign='top'>
                    <!-- right table -->
                    <form action="scoreEntry.jsp" method="POST"
                        name="verify">
                        <input type="hidden" name='EditFlag'
                            value="true" />

                        <table>
                            <tr align='left' valign='top'>
                                <td>
                                    <!-- pick team from a list -->
                                    <br>
                                    <font face='arial' size='4'>Unverified
                                        Runs:</font>
                                    <br>
                                    <select size='20'
                                        id='select-verify-teamnumber'
                                        name='TeamNumber'
                                        ondblclick='verify.submit()'>
                                    </select>
                                </td>
                            </tr>
                            <tr>
                                <!-- submit button -->
                                <td align='left'>
                                    <input class='dark_bg' type="submit"
                                        id="verify_submit"
                                        value="Verify Score">
                                </td>
                            </tr>

                        </table>
                    </form>
                </td>
                <!-- right table -->
            </c:if>

        </tr>

    </table>
    <!-- outer table -->

    <c:if test="${empty scoreEntrySelectedTable}">
        <script type="text/javascript" id="reloadruns"
            src="UpdateUnverifiedRuns"></script>
    </c:if>

</body>
</html>
