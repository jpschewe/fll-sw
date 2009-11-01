<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%@ page import="java.io.File"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.List"%>
<%@ page import="fll.Utilities"%>

<%
//All logos shall be located under sponsor_logos in the fll web folder.
String imagePath = application.getRealPath("sponsor_logos");
File[] directories = {new  File(imagePath)};
List<String> logoFiles = new ArrayList<String>();
Utilities.buildGraphicFileList("", directories, logoFiles);

//This varible holds the index of the last image, relative to imagePath
final int numLogos = logoFiles.size();
int lastLogoIndex;
if(numLogos < 1) {
	lastLogoIndex = -1;
} else if(null != session.getAttribute("lastLogoIndex")) {
	lastLogoIndex = ((Integer)session.getAttribute("lastLogoIndex")).intValue();
} else {
	lastLogoIndex = numLogos - 1;
}
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Sponsor frame</title>
  <script language=javascript>
    window.setInterval("location.href='sponsors.jsp'", 15000);
  </script>
</head>
<body>
<% if (numLogos > 0) { %>
<table align="center">
  <tr>
    <td width="50%" style="text-align:right; vertical-align:middle">
      This tournament sponsored by:
    </td>
    <td width="50%" style="text-align:left; vertical-align:middle">
      <% lastLogoIndex = (lastLogoIndex + 1) % numLogos; %>
     <% out.print("<img src='../" + logoFiles.get(lastLogoIndex) + "'/>"); %>
      <% session.setAttribute("lastLogoIndex",lastLogoIndex); %>
    </td>
  </tr>
</table>
<% } // end if %>
</body>
</html>
