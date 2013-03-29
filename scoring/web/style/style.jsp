<%@ page contentType="text/css" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

BODY { 
  background: url(<c:url value="/images/bricks1.gif"/>) white;
  margin-top: 4;
}

<%-- How to include another css file? --%>
.help {
  background-color:#fafafa;
  border:1px solid black;
  width:80%;
  padding: 3px;
  font-family:sans-serif;
  font-size: 10pt;
}

.error {
  color: red;
  font-weight: bold;
  background-color: black;
}

.warning {
  color: yellow;
  font-weight: bold;
  background-color: black;
}

.hard-violation {
  background-color: red;
}

.soft-violation {
  background-color: yellow;
}

.bold {
  font-weight: bold;
}
  
  