<%@ page errorPage="../errorHandler.jsp" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
  
<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="fll.Utilities" %>
  
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>

<%
final Connection connection = (Connection)application.getAttribute("connection");
final String currentTournament = (String)application.getAttribute("currentTournament");
final Statement stmt = connection.createStatement();
final String countSQL = "SELECT COUNT(*)"
  + " FROM Teams,Performance"
  + " WHERE Performance.Tournament = '" + currentTournament + "'"
  + " AND Teams.TeamNumber = Performance.TeamNumber"
  + " ORDER BY Teams.Division, Teams.TeamNumber, Performance.RunNumber";
ResultSet rs = stmt.executeQuery(countSQL);
rs.next();
final int numRecords = rs.getInt(1);
Utilities.closeResultSet(rs);

final String sql = "SELECT Teams.TeamNumber, Teams.Organization, Teams.TeamName, Teams.Region, Teams.Division, Performance.Tournament, Performance.RunNumber, Performance.Bye, Performance.NoShow, Performance.ComputedTotal"
  + " FROM Teams,Performance"
  + " WHERE Performance.Tournament = '" + currentTournament + "'"
  + " AND Teams.TeamNumber = Performance.TeamNumber"
  + " ORDER BY Teams.Organization, Teams.TeamNumber, Performance.RunNumber";
rs = stmt.executeQuery(sql);
%>

<c:set var="thisURL">
  <c:url value="${pageContext.request.servletPath}">
    <c:param name="scroll" value="${param.scroll}" />
  </c:url>
</c:set>
    
<HTML>
<head>
<style>
	FONT {color: #ffffff; font-family: "Arial"}
	TABLE.A {background-color:#000080 }
	TABLE.B {background-color:#0000d0 }
</style>

<script language="JavaScript">
function myHeight() {
  return (document.all.dummy.offsetHeight-300);
}

function myScroll() {
  documentYposition += scrollAmount;
  window.scroll(0,documentYposition);
  if (documentYposition > documentLength) {
    window.clearInterval(IntervalRef);
    location.href='<c:out value="${thisURL}" />'
  }
}

function start() {
<c:if test="${param.scroll}">
  documentLength = myHeight();
  //myScroll();
  <c:choose>
    <c:when test="numRecords >= 4">
  IntervalRef = window.setInterval('myScroll()',iInterval);
    </c:when>
    <c:otherwise>
  window.setTimeout('location.href="<c:out value="${thisURL}" />"',30000);
    </c:otherwise>
  </c:choose>
</c:if>
}

<c:choose>
  <c:when test="numRecords >= 4">
var iInterval = 30;
  </c:when>
  <c:otherwise>
var iInterval = 10000;
  </c:otherwise>
</c:choose>
var IntervalRef;
var documentLength;
var scrollAmount = 2;    // scroll by 100 pixels each time
var documentYposition = 0;
</script>


</head>

<body bgcolor='#000080' onload='start()'>
<div id="dummy" style="position:absolute">
<br><br><br><br><br><br><br><br>

<c:set var="colorStr" value="A" />
                            
<% 	
String divisionStr;
      
      
if(rs.next()) {
  boolean done = false;
  while(!done) {
%>
  <table border='0' cellpadding='0' cellspacing='0' width='99%'
                              class='<c:out value="${colorStr}" />'>
    <tr><td colspan='2'><img src='blank.gif' height='15' width='1'></td></tr>
    <tr align='left'>
<%
  if(rs.getInt("Division") == 1) {
    divisionStr = "#800000";
  } else {
    divisionStr = "#008000";
  }
%>
      <td width='25%' bgcolor='<%=divisionStr%>'>
        <font size='2'><b>&nbsp;&nbsp;Division:&nbsp;<%=rs.getInt("Division")%>&nbsp;&nbsp;</b></font>
     </td>
     <td align='right'>
       <font size='2'><b>Team&nbsp;#:&nbsp;<%=rs.getInt("TeamNumber")%>&nbsp;&nbsp;</b></font>
    </td>
  </tr>
  <tr align='left'>
    <td colspan='2'>
      <font size='4'>&nbsp;&nbsp;<%=rs.getString("Organization")%></font>
    </td>
  </tr>
  <tr align='left'>
    <td colspan='2'>
      <font size='4'>&nbsp;&nbsp;<%=rs.getString("TeamName")%></font>
    </td>
  </tr>
  <tr>
    <td colspan='2'><hr color='#ffffff' width='96%'></td>
  </tr>
  <tr>
  <td colspan='2'>

    <table border='0' cellpadding='1' cellspacing='0'>
      <tr align='center'>
        <td><img src='../images/blank.gif' height='1' width='60'></td>
        <td><font size='4'>Run #</font></td>
        <td><img src='../images/blank.gif' width='20' height='1'></td>
          <td><font size='4'>Score</font></td>
        </tr>
        <%
        int prevNum = rs.getInt("TeamNumber");
        do {
        %>
        <tr align='right'>
          <td><img src='blank.gif' height='1' width='60'></td>
          <td><font size='4'><%=rs.getInt("RunNumber")%></font></td>
          <td><img src='blank.gif' width='20' height='1'></td>
          <td><font size='4'>
          <%if(rs.getBoolean("NoShow")) {%>
            No Show
          <%} else if(rs.getBoolean("Bye")) {%>
            Bye
          <%} else {
              out.println(rs.getInt("ComputedTotal"));
            }
            if(!rs.next()) {
              done = true;
            }
           } while(!done && prevNum == rs.getInt("TeamNumber"));
          %>
          </font></td>
      </table>
      
    </td>
  </tr>
  <tr><td colspan='2'><img src='../images/blank.gif' width='1' height='15'></td></tr>
</table>

<c:choose>
  <c:when test="${'A' == colorStr}">
    <c:set var="colorStr" value="B" />
  </c:when>
  <c:otherwise>
    <c:set var="colorStr" value="A" />
  </c:otherwise>
</c:choose>

<%
  } //end while(!done)
}//end if
%>
                          
</div>
</body>
<%
  Utilities.closeResultSet(rs);
  Utilities.closeStatement(stmt);
%>
</HTML>
