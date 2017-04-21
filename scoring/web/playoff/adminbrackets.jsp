<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.playoff.AdminBrackets.populateContext(request, application, pageContext);
%>

<html>
<head>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/fll-sw.css'/>" />
<title>${bracketInfo.bracketName}PrintablePlayoff Bracket</title>
</head>

<script type="text/javascript">
  var bracketInfo = ${bracketInfoJson};  
</script>


<script
  type='text/javascript'
  src='../extlib/jquery-1.11.1.min.js'></script>
<script
  type='text/javascript'
  src='h2hutils.js'></script>
<script
  type='text/javascript'
  src='adminbrackets.js'></script>

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

  ${bracketInfo.adminBracketOutput}

</body>
</html>
