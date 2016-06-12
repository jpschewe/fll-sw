<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.admin.ChangePassword.populateContext(request, application, pageContext);
%>

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

 <c:choose>
  <c:when test="${not empty fll_user}">
   <p>Change password for ${fll_user}.</p>

   <form method="POST" action="ChangePassword" name="change_password">
    Old password : <input type="password" size="15" maxlength="64"
     name="old_password"><br /> New password : <input
     type="password" size="15" name="pass"><br /> Repeat new
    password : <input type="password" size="15" name="pass_check"><br />

    <input name="submit_change_password" value="Change Password"
     type="submit" />
   </form>
  </c:when>
  <c:otherwise>
   <p>
    You are not currently logged in as a user, therefore you cannot
    change your password. If you are connected from the server you
    should visit the <a href="<c:url value='/login.jsp'/>">login
     page</a>.
   </p>
  </c:otherwise>
 </c:choose>

</body>

</html>