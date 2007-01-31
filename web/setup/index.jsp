<%@ page import="fll.db.GenerateDB"%>
<%@ page import="fll.xml.ChallengeParser"%>

<%@ page import="java.io.InputStreamReader"%>
<%@ page import="fll.web.UploadProcessor"%>
<%@ page import="org.w3c.dom.Document"%>
<%@ page import="org.apache.commons.fileupload.FileItem"%>

<%
final StringBuffer message = new StringBuffer();
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>FLL (Database setup)</title>
</head>

<body>
<h1>FLL (Database setup)</h1>

<%
        if ("POST".equals(request.getMethod())) {

        UploadProcessor.processUpload(request);
        final FileItem xmlFileItem = (FileItem) request.getAttribute("xmldocument");

        if (null != request.getAttribute("reinitializeDatabase")) {
          final boolean forceRebuild = "1".equals(request.getAttribute("force_rebuild"));
          if (null == xmlFileItem) {
            message.append("<p class='error'>XML description document not specified</p>");
          } else {
            //          For debugging only
            final java.io.File testoutput = new java.io.File("/tmp/fll.xml");
            xmlFileItem.write(testoutput);

            final Document document = ChallengeParser.parse(new InputStreamReader(xmlFileItem.getInputStream()));

            final String db = config.getServletContext().getRealPath("/WEB-INF/flldb");
            GenerateDB.generateDB(document, db, forceRebuild);

            //remove application variables that depend on the database
            application.removeAttribute("connection");
            application.removeAttribute("challengeDocument");

            message.append("<p id='success'><i>Successfully initialized database</i></p>");
          }
        } else {
          message.append("reinitializeDatabase attribute not set?");
        } /* end if reinitializeDatabase */
      } /* end if POST */
%>


<p><%=message.toString()%> This will create a database called fll.</p>
<form id='setup' action='index.jsp' method='post'
 enctype='multipart/form-data'>XML description document <input
 type='file' size='32' name='xmldocument'><br />

<input type='checkbox' name='force_rebuild' value='1' /> Rebuild the
whole database, including team data?<br />

<input type='submit' name='reinitializeDatabase'
 value='Initialize Database'
 onclick='return confirm("This will erase ALL scores in the database fll (if it already exists), are you sure?")'>

</form>

<%@ include file="/WEB-INF/jspf/footer.jspf"%>

</body>
</html>
