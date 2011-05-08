<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>FLL (Database setup)</title>
</head>

<body>
<h1>FLL (Database setup)</h1>

<p>${message}</p>
<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />

<c:set var="redirect_url" scope="session">
 <c:url value="/" />
</c:set>

<p>
This will create a database called fll.
</p>
<form id='setup' action='CreateDB' method='post'
 enctype='multipart/form-data'>XML description document <input
 type='file' size='32' name='xmldocument'><br />

<input type='checkbox' name='force_rebuild' value='1' /> Rebuild the
whole database, including team data?<br />

<input type='submit' name='reinitializeDatabase'
 value='Initialize Database'
 onclick='return confirm("This will erase ALL scores in the database fll (if it already exists), are you sure?")' />

</form>

<form id='import' action='CreateDB' method='post'
 enctype='multipart/form-data'>

<p>Create database from a database dump.</p>
<input type='file' size='32' name='dbdump'> <input type='submit'
 name='createdb' value='Create Database' /></form>



</body>
</html>
