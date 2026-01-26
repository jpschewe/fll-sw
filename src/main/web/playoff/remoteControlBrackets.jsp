<!DOCTYPE html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<%@ page import="fll.web.playoff.RemoteControlBrackets"%>

<%
RemoteControlBrackets.populateContext(application, session, request, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/big_screen.css'/>" />

<style type='text/css'>
TD.Leaf {
    color: #ffffff;
    background-color: #000000
}

TD.Bridge {
    background-color: #808080
}

SPAN.TeamNumber {
    color: #ff8080;
    padding-right: 5px;
}

SPAN.TeamName {
    color: #ffffff;
}

SPAN.TeamScore {
    color: #ffffff;
    font-weight: bold;
}

SPAN.TIE {
    color: #ff0000;
}

.TABLE_ASSIGNMENT {
    font-family: monospace;
    font-size: small;
    background-color: white;
    padding-left: 5%;
    padding-right: 5%;
}
</style>

<script type="text/javascript"
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type="text/javascript">
  const allBracketData = JSON.parse('${allBracketDataJson}');
  const secondsBetweenScrolls = parseInt("${scrollRate}"); // could be here directly as an integer, but the JSTL and auto-formatting don't agree
  const maxNameLength = parseInt("${maxNameLength}"); // could be here directly as an integer, but the JSTL and auto-formatting don't agree
  const displayUuid = "${param.display_uuid}";
  const REGISTER_MESSAGE_TYPE = "${REGISTER_MESSAGE_TYPE}";
  const BRACKET_UPDATE_MESSAGE_TYPE = "${BRACKET_UPDATE_MESSAGE_TYPE}";
  const DISPLAY_UPDATE_MESSAGE_TYPE = "${DISPLAY_UPDATE_MESSAGE_TYPE}";
</script>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js' />"></script>
<script type='text/javascript' src='h2hutils.js'></script>
<script type='text/javascript' src='remoteControlBrackets.js'></script>

<script type="text/javascript">
  document.addEventListener("DOMContentLoaded", function() {
    <c:if test="${empty param.scroll}">
    startScrolling();
    </c:if>
    colorTableLabels();
  });
</script>
</head>
<body>
    <span id="top">&nbsp;</span>

    <br />
    <c:forEach items="${allBracketData}" var="bracketData">

        <div class='center'>Head to Head Round
            ${bracketData.firstRound}, Head to Head Bracket
            ${bracketData.bracketName}</div>
        <br />
                        
   ${bracketData.bracketOutput}
     <c:if test="${allBracketData.size() > 1}">
            <br />
            <br />
            <hr />
            <br />
            <br />
        </c:if>
    </c:forEach>
    <span id="bottom">&nbsp;</span>

</body>
</html>
