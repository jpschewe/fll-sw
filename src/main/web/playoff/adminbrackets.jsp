<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN,REF" allowSetup="false" />

<%
fll.web.playoff.AdminBrackets.populateContext(request, application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>${bracketInfo.bracketName}-PrintablePlayoffBracket</title>
</head>

<script type="text/javascript">
  const bracketInfo = JSON.parse('${bracketInfoJson}')';
  const REGISTER_MESSAGE_TYPE = "${REGISTER_MESSAGE_TYPE}";
  const BRACKET_UPDATE_MESSAGE_TYPE = "${BRACKET_UPDATE_MESSAGE_TYPE}";
  const DISPLAY_UPDATE_MESSAGE_TYPE = "${DISPLAY_UPDATE_MESSAGE_TYPE}";
</script>


<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js' />"></script>
<script type='text/javascript' src='h2hutils.js'></script>
<script type='text/javascript' src='adminbrackets.js'></script>

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

.TABLE_ASSIGNMENT {
    font-family: monospace;
    font-size: 85%;
    font-weight: bold;
    background-color: white;
    padding-left: 5%;
    padding-right: 5%
}
</style>

<body>

    <h2>Playoff Bracket: ${bracketInfo.bracketName}</h2>

    ${bracketInfo.bracketOutput}

</body>
</html>
