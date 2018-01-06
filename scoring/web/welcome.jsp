<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@page import="fll.web.Welcome"%>

<html>
<head>

<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/base.css'/>" />

<meta
  http-equiv='refresh'
  content='90' />

<title>Welcome</title>
<style type='text/css'>
html {
	margin-top: 5px;
	margin-bottom: 5px;
	margin-left: 5px;
	margin-right: 5px;
}

body {
	margin-top: 4;
}

.title {
	font-weight: bold;
	font-size: 200%;
	padding: 20px;
}
</style>

</head>

<body>

  <div class='center'>
    <h1>${challengeDescription.title }</h1>

    <table width="100%">
      <tr>
        <td width="50%">
          <img src='<c:url value="/images/htk_logo.jpg"/>' width="100%" align="middle"/>
        </td>
        <td width="50%">
          <img src='images/fll_logo.gif' width="100%"/>          
        </td>
    </table>

        <%
          Welcome.outputLogos(application, out);
        %>

    <h2>${ScorePageText }</h2>

  </div>

</body>
</html>
