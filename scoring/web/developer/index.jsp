<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Developer Commands</title>
</head>

<body>
<h1><x:out select="$challengeDocument/fll/@title" /> (Developer
Commands)</h1>

<p><font color='red'><b>This page is intended for
developers only. If you don't know what you're doing, LEAVE THIS PAGE!</b></font></p>

${message}

<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />

<c:set var="redirect_url" scope="session">
 <c:url value="/developer/index.jsp" />
</c:set>

<ul>

 <li><a href="query.jsp">Do SQL queries and updates</a></li>

<li>
 <form id='import' action='importdb/ImportDBDump' method='post'
  enctype='multipart/form-data'>

 <p>Import data from a database dump into the current database.</p>
 <input type='file' size='32' name='dbdump'/> <input
  type='submit' name='importdb' value='Import Database' /></form>
</li>

<li>

<form id='replace-descriptor' action='ReplaceChallengeDescriptor' 
method='post' enctype='multipart/form-data'>
<p>Replace the current challenge descriptor. Will succeed if there
are no changes to the structure of the database.<br/>
<input type='file' size='32' name='xmldoc'/>
<input type='submit' value='Submit'/> 
</p>
</form>
</li>

 <li>inside.test: <%=System.getProperty("inside.test")%> -- <%=Boolean.getBoolean("inside.test")%></li>

 <li>Database is located at: ${database}</li>
      
 
 </ul>


</body>
</html>
