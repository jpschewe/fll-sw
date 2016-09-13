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

    <!--img height="40%" align='middle' src='<c:url value="/images/logo.gif"/>' /><br /-->
    <img
      height="20%"
      align='middle'
      src='<c:url value="/images/htk_logo.jpg"/>' /><br />

    <table>
      <tr>

        <%
          Welcome.outputLogos(application, out);
        %>

        <td valign="middle"><img
          width='100%'
          src='images/fll_logo.gif' /></td>

      </tr>
    </table>

    <h2>${ScorePageText }</h2>

  </div>

</body>
</html>
