<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.admin.UploadSubjectiveData"%>

<%@ page import="java.io.File"%>
<%@ page import="org.apache.commons.fileupload.FileItem"%>
<%@ page import="fll.web.UploadProcessor"%>

<%@ page import="java.sql.Connection"%>
<%@ page import="fll.db.Queries"%>
<%@ page import="org.w3c.dom.Document"%>
<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Upload Subjective Data</title>
</head>

<body>
<p>
<ul>
 <%
       UploadProcessor.processUpload(request);
       final FileItem subjectiveFileItem = (FileItem) request.getAttribute("subjectiveFile");
       final File file = File.createTempFile("fll", null);
       subjectiveFileItem.write(file);

       final Connection connection = (Connection) application.getAttribute("connection");
       UploadSubjectiveData.saveSubjectiveData(out, file, Queries.getCurrentTournament(connection), (Document) application.getAttribute("challengeDocument"),
           connection);
       file.delete();
 %>
 </li>
</ul>
</p>

<p>
<ul>
 <li>Normally you'd be redirected <a
  href="<%=response.encodeRedirectURL("index.jsp?message=Subjective+data+uploaded+successfully")%>">here.</a></li>
</ul>
</p>

<%
response.sendRedirect(response.encodeRedirectURL("index.jsp?message=Subjective+data+uploaded+successfully"));
%>

<%@ include file="/WEB-INF/jspf/footer.jspf"%>
</body>
</html>
