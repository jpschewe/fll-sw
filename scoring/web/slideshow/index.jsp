<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Utilities" %>
  
<%@ page import="java.io.File" %>

<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
      
<%
// All images shall be located under slideshow/images in the fll web folder.
String imagePath = application.getRealPath("/slideshow/images");
// This varible holds the name of the last image, relative to imagePath
String lastImage;
if(null != session.getAttribute("slideShowLastImage")) {
  lastImage = (String)session.getAttribute("slideShowLastImage");
} else {
  lastImage = "";
}

File[] directories = {new  File(imagePath)};
List<String> files = new ArrayList<String>();
Utilities.buildGraphicFileList("", directories, files);

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
  slideShowInterval = 5000;
} else {
  slideShowInterval = ((Integer)application.getAttribute("slideShowInterval")).intValue();
}
%>

<html>
<head>
  <style>
    FONT {color: #ffffff; font-family: "Arial"}
  </style>
  <script language=javascript>
    window.setInterval("location.href='index.jsp'",<%=slideShowInterval %>);
    function sizeImage() {
      var d,x,y,i, ox, oy;
      i = document.getElementById("image");
      if(i != null) {
        x = i.width+20;
        y = i.height+20;
        ox = x;
        oy = y;
        if(x > document.body.clientWidth) {
          var scalefactor = document.body.clientWidth/x;
          x = x * scalefactor;
          y = y * scalefactor;
        }
        if(y > document.body.clientHeight) {
          var scalefactor = document.body.clientHeight/y;
          x = x * scalefactor;
          y = y * scalefactor;
        }
        i.width = x;
        i.height = y;
        /*
        var d = document.getElementById("dimensions");
        d.appendChild(document.createTextNode("Original Dimensions: "+ox+","+oy));
        d.appendChild(document.createElement("br"));
        d.appendChild(document.createTextNode("New Dimensions: "+x+","+y));
       */
       var f = document.getElementById("frame");
       f.style.right = document.body.clientWidth;
       f.style.bottom = document.body.clientHeight;
      }
    }
    window.onload=function(){sizeImage()};
  </script>
</head>

<body style="vertical-align:middle; text-align:center;">
  <div id='frame' style='vertical-align:middle; text-align:center; left:0; top:0; border:0; padding:0; margin:0;'>
  <c:set var='lastImage'><%=lastImage %></c:set>
  <c:choose>
    <c:when test="${empty lastImage}">
      <img id='image' src='<c:url value="../images/logo.gif"/>'/>
    </c:when>
    <c:otherwise>
      <img id='image' src='<c:url value="${lastImage}"/>'/>
    </c:otherwise>
  </c:choose>
  </div>
  <!-- div id='dimensions'></div -->
</body>

</html>
