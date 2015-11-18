<%@ include file="/WEB-INF/jspf/init.jspf"%>

<!DOCTYPE HTML>
<html>
<head>
<title>Login Page</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
 
 <link
  rel="stylesheet"
  href="extlib/jquery.mobile-1.4.5.min.css" />

<script
  type='text/javascript'
  src='extlib/jquery-1.11.1.min.js'></script>
<script
  type='text/javascript'
  src='extlib/jquery.mobile-1.4.5.min.js'></script>
 
</head>
<body>
 <h1>Login to FLL</h1>

 ${message}
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 <form method="POST" action="DoLogin" name="login" data-ajax="false">
  Username : <input type="text" size="15" maxlength="64" name="user" autocorrect="off" autocapitalize="off" autocomplete="off" spellcheck="false"/><br />
  Password : <input type="password" size="15" name="pass" /><br /> <input
   name="submit_login" value="Login" type="submit" />
 </form>
</body>
</html>
