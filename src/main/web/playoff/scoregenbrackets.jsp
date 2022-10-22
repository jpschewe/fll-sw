<%@page import="fll.web.playoff.BracketData.TopRightCornerStyle"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.playoff.BracketData"%>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="fll.db.Queries"%>
<%@ page import="fll.db.TableInformation"%>
<%@ page import="java.sql.Connection"%>
<%@ page import="javax.sql.DataSource"%>
<%@ page import="org.w3c.dom.Document"%>
<%@ page import="java.util.List"%>

<fll-sw:required-roles roles="ADMIN,REF" allowSetup="false" />

<%
fll.web.playoff.ScoregenBrackets.populateContext(application, request, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>${division}&nbsp;-&nbsp;Head&nbsp;to&nbsp;head&nbsp;Bracket</title>

<style type='text/css'>
TD.Leaf {
    font-family: Arial;
    border-bottom: solid
}

TD.BridgeBottom {
    border-left: solid;
    border-right: solid;
    border-bottom: solid
}

TD.BridgeMiddle {
    border-left: solid;
    border-right: solid
}

TD.BridgeTop {
    border-bottom: solid
}

FONT {
    font-family: Arial
}

FONT.TeamNumber {
    font-weight: bold
}

FONT.TeamName {
    font-weight: bold
}

FONT.TeamScore {
    font-weight: bold;
    font-size: 10pt
}

FONT.TIE {
    color: #ff0000;
    font-weight: bold
}
</style>

<script type='text/javascript'>
  function matchTables(selectChanged, selectMatch) {

    <c:forEach items="${tableInfo}" var="info">

    if (selectChanged.value == "${info.sideA}") {
      if (selectMatch.value != "${info.sideB}") {
        selectMatch.value = "${info.sideB}";
      }
      return;
    } else if (selectChanged.value == "${info.sideB}") {
      if (selectMatch.value != "${info.sideA}") {
        selectMatch.value = "${info.sideA}";
      }
      return;
    }

    </c:forEach>
  }

  document.addEventListener("DOMContentLoaded", function() {
    <%-- must be on 1 line --%>
    ${bracketInfo.tableSyncFunctionsOutput}
  });
</script>

<script type='text/javascript' src='scoregenbrackets.js'></script>

<c:choose>
    <c:when test="${param.editTables}">
        <c:set var="form_target" value="" />
    </c:when>
    <c:otherwise>
        <c:set var="form_target" value="_new" />
    </c:otherwise>
</c:choose>

</head>


<body>
    <h2>Head to head Bracket: ${division }</h2>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>
        <a href="index.jsp">Return to Head to head menu</a>
    </p>

    <form name='printScoreSheets' method='post'
        action='ScoresheetServlet' target='${form_target}'>
        <input type='hidden' name='division' value='${division}' />
        <input type='hidden' name='numMatches' value='${numMatches}' />
        <input type='hidden' name='editTables'
            value='${param.editTables}' />

        <c:choose>
            <c:when test="${param.editTables}">
                <input type='submit' value='Save table assignments'
                    id='save_table_assignments' />
            </c:when>
            <c:otherwise>
                <input type='submit' value='Print scoresheets'
                    id='print_scoresheets'
                    onclick='return checkSomethingToPrint()' />
        -
        <b>Print the scoresheets for the matches that have their
                    boxes checked.</b>
            </c:otherwise>
        </c:choose>


        ${bracketInfo.bracketOutput}

        <p>
            <a href="index.jsp">Return to Head to head menu</a>
        </p>
    </form>

</body>
</html>
