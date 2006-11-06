<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<c:set var="${response.contentType}" value="text/css" />
/*CSS for Bubble Tooltips by Alessandro Fulciniti
- http://pro.html.it - http://web-graphics.com */

.tooltip{
width: 400px; color:#000;
font:lighter 11px/1.3 Arial,sans-serif;
text-decoration:none;text-align:left}

.tooltip span.top{padding: 30px 8px 0;
    background: url(<c:url value="/images/bt.gif"/>) no-repeat top}

.tooltip b.bottom{padding:3px 8px 15px;color: #548912;
    background: url(<c:url value="/images/bt.gif"/>) no-repeat bottom}