<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Utilities" %>
<%@ page import="fll.Queries" %>
  
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>

<%
final Connection connection = (Connection)application.getAttribute("connection");
pageContext.setAttribute("currentTournament", Queries.getCurrentTournament(connection));
%>
  
<HTML>
<head>
<style>
        FONT {color: #ffffff; font-family: "Arial"}
</style>
<script language=javascript>
  window.setInterval("location.href='last8.jsp'",30000);
</script>
</head>

<body bgcolor='#000080'>
  <center>
    <table border='0' cellpadding='0' cellspacing='0' width='98%'>
      <tr align='center'>
        <td colspan='6'><font size='3'><b>Most Recent Performance Scores</b></font></td>
      </tr>
      <tr>
        <td colspan='6'><img src='<c:url value="/images/blank.gif"/>' width='1' height='4'></td>
      </tr>
      <tr align='center' valign='middle'>
        <td width='10%'><font size='2'><b>Team<br>Num.</b></font></td>
        <td width='28%'><font size='2'><b>Team<br>Name</b></font></td>
        <td><font size='2'><b><br>Organization</b></font></td>
        <td width='5%'><font size='2'><b><br>Div.</b></font></td>
        <td width='5%'><font size='2'><b><br>Run</b></font></td>
        <td width='8%'><font size='2'><b><br>Score</b></font></td>
      </tr>
      <tr>
        <td colspan='6' align='center'>
          <sql:query var="result" dataSource="${datasource}">
            SELECT Teams.TeamNumber, Teams.Organization, Teams.TeamName, Teams.Division, Performance.Tournament, Performance.RunNumber, Performance.Bye, Performance.NoShow, Performance.TimeStamp, Performance.ComputedTotal
              FROM Teams,Performance
              WHERE Performance.Tournament = '<c:out value="${currentTournament}"/>'
                AND Teams.TeamNumber = Performance.TeamNumber
              ORDER BY Performance.TimeStamp DESC, Teams.TeamNumber ASC LIMIT 8
          </sql:query>
                    
          <!-- scores here -->
          <table border='1' bordercolor='#aaaaaa' cellpadding='4' cellspacing='0' width='100%'>
            <c:forEach items="${result.rows}" var="row">
              <tr align='left'>
                <td width='10%' align='right'>
                  <font size='3'><c:out value="${row.TeamNumber}"/></font>
                </td>
                <td width='28%'>
                  <font size='3'>
                    <c:set var="teamName" value="${row.TeamName}"/>
                    <%
                    if(null != pageContext.getAttribute("teamName") && ((String)pageContext.getAttribute("teamName")).length() > 20) {
                      out.println(((String)pageContext.getAttribute("teamName")).substring(0, 20) + "&nbsp;");
                    } else {
                      out.println(pageContext.getAttribute("teamName") + "&nbsp;");
                    }
                    %>
                  </font>
                </td>
                <td>
                  <font size='3'>
                    <c:set var="organization" value="${row.Organization}"/>
                    <%
                    if(null != pageContext.getAttribute("organization") && ((String)pageContext.getAttribute("organization")).length() > 35) {
                      out.println(((String)pageContext.getAttribute("organization")).substring(0, 35) + "&nbsp;");
                    } else {
                      out.println(pageContext.getAttribute("organization") + "&nbsp;");
                    }
                    %>
                  </font>
                </td>
                <td width='5%' align='right'>
                  <font size='3'><c:out value="${row.Division}"/></font>
                </td>
                <td width='5%' align='right'>
                  <font size='3'><c:out value="${row.RunNumber}"/></font>
                </td>
                <td width='8%' align='right'>
                  <font size='3'>
                    <c:choose>
                      <c:when test="${row.NoShow == 1}">
                        No Show
                      </c:when>
                      <c:when test="${row.Bye == 1}">
                        Bye
                      </c:when>
                      <c:otherwise>
                        <c:out value="${row.ComputedTotal}"/>
                      </c:otherwise>
                    </c:choose>
                  </font>
                </td>
              </tr>
            </c:forEach>
          </table>
          <!-- end scores -->
        </td>
      </tr>
    </table>
  </center>
</body>
</HTML>
