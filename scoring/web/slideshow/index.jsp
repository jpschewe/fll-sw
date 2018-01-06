<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Utilities" %>
<%@ page import="fll.web.ApplicationAttributes" %>
<%@ page import="fll.web.SessionAttributes" %>
  
<%@ page import="java.io.File" %>

<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
      
<%
// All images shall be located under slideshow/ in the fll web folder.
String imagePath = application.getRealPath("/slideshow");
// This varible holds the name of the last image, relative to imagePath
String lastImage;
if(null != session.getAttribute("slideShowLastImage")) {
  lastImage = SessionAttributes.getAttribute(session, "slideShowLastImage", String.class);
} else {
  lastImage = "../images/logo.gif";
}

List<String> files = Utilities.getGraphicFiles(new File(imagePath));

if(files.size() == 0) {
  lastImage = "";
}
else {
  int oldFileIdx = files.indexOf(lastImage);
  if(oldFileIdx < 0 || oldFileIdx == files.size()-1)
    lastImage = files.get(0);
  else
    lastImage = files.get(oldFileIdx+1);
}
session.setAttribute("slideShowLastImage",lastImage);

int slideShowInterval;
if(null == application.getAttribute("slideShowInterval")) {
  slideShowInterval = 10000;
} else {
  slideShowInterval = ApplicationAttributes.getAttribute(application, "slideShowInterval", Number.class).intValue() * 1000;
}

if(slideShowInterval < 1) {
  slideShowInterval = 1 * 1000;
}

pageContext.setAttribute("lastImage", lastImage);
%>

<!-- <%=lastImage%> -->
<html>
<head>
  <style>
    FONT {color: #ffffff; font-family: "Arial"}
body {
  margin: 0;
  padding: 0;
  }
div.img {
  background-image: url("<c:url value="/${lastImage}"/>");
  background-position: center center;
  background-repeat: no-repeat;
  background-size: contain;
  width: 100vw;
  height: 100vh;
  }

  </style>
  <script type='text/javascript'>
    window.setInterval("location.href='index.jsp'",<%=slideShowInterval %>);
  </script>
</head>

<body style="vertical-align:middle; text-align:center;">
    <div class="img"></div>

</body>

</html>
