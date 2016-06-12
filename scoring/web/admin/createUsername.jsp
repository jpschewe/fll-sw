<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>

<head>
<title>Administration</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>

 <div class='status-message'>${message}</div>
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

<p>Specify a username and password that is used to be able to edit information in the database.</p>

 <form method="POST" action="CreateUser" name="create_user">
  Username : <input type="text" size="15" maxlength="64" name="user"><br />
  Password : <input type="password" size="15" name="pass"><br />
  Repeat Password : <input type="password" size="15" name="pass_check"><br />

  <input name="submit_create_user" value="Create User" type="submit" />
 </form>


</body>

</html>