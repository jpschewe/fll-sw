<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Reporting</title>
</head>

<body>

 <form action="FinalComputedScores" method="post">

  <p>To advance to the next tournament a team's top performance
   score must be in the top X% of all teams top performance scores. That
   percentage may be specified here. Those scores meeting this
   requirement will show their performance scores in bold. If you don't
   know what this is or don't care, just leave the value at the default
   of 0%.</p>

  <!--  TODO issue:129 need to validate that this is a number -->
  <input type="text" name="percentage" value="0" />% <br /> <input
   type="submit" />


 </form>

</body>
</html>
