<%@ include file="/WEB-INF/jspf/init.jspf"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>Login Page</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
</head>
<body>
 <h1>Login to FLL</h1>

 ${message}
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 <form method="POST" action="DoLogin" name="login">
  Username : <input type="text" size="15" maxlength="64" name="user" /><br />
  Password : <input type="password" size="15" name="pass" /><br /> <input
   name="submit_login" value="Login" type="submit" />
 </form>
</body>
</html>
