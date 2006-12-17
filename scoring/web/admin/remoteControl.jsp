<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>

<%@ page import="java.sql.Connection" %>      

<%@ page import="fll.Utilities" %>
<%@ page import="fll.db.Queries" %>

      
<%
final Connection connection = (Connection)application.getAttribute("connection");
      
final List divisions = Queries.getDivisions(connection);

if(null != request.getParameter("division")) {
  application.setAttribute("playoffDivision", request.getParameter("division"));
}
if(null != request.getParameter("playoffRoundNumber")) {
  application.setAttribute("playoffRoundNumber", Utilities.NUMBER_FORMAT_INSTANCE.parse(request.getParameter("playoffRoundNumber")));
}
if(null != request.getParameter("changeSlideInterval")) {
  // guarantee a minimum interval of 1 second
  int newInterval = Integer.parseInt(request.getParameter("slideInterval"));
  if(newInterval < 1) {
    newInterval = 1;
  }
  application.setAttribute("slideShowInterval", new Integer(1000 * newInterval));
}
  
if(null == application.getAttribute("playoffDivision") && !divisions.isEmpty()) {
  application.setAttribute("playoffDivision", divisions.get(0));
}
if(null == application.getAttribute("playoffRoundNumber")) {
  application.setAttribute("playoffRoundNumber", new Integer(1));
}
if(null == application.getAttribute("slideShowInterval")) {
  application.setAttribute("slideShowInterval",new Integer(10000));
}

final String playoffDivision = (String)application.getAttribute("playoffDivision");
final int playoffRunNumber = ((Number)application.getAttribute("playoffRoundNumber")).intValue();
final int numPlayoffRounds = Queries.getNumPlayoffRounds(connection);

%>
      
<html>
  <head>
    <title><x:out select="$challengeDocument/fll/@title"/> (Display Controller)</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <c:if test="${empty displayPage}">
    <c:set var='displayPage' value='welcome' scope='application'/>
  </c:if>
          
  <c:if test="${not empty param.remotePage}">
    <c:set var='displayPage' value='${param.remotePage}' scope='application'/>
  </c:if>
  <c:if test="${not empty param.remoteURL}">
    <c:set var='displayURL' value='${param.remoteURL}' scope='application'/>
  </c:if>
  <c:choose>
    <c:when test="${empty param.slideInterval}">
      <c:set var='slideInterval' value='10'/>
    </c:when>
    <c:otherwise>
      <c:set var='slideInterval' value='${param.slideInterval}'/>
    </c:otherwise>
  </c:choose>
          
  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Display Controller)</h1>

    <p>This page is used to control what page is currently visible on the
    display screen.  Note that it takes some time for the display to
    change, up to 2 minutes.</p>

    <form name='remote' action='remoteControl.jsp' method='post'>
      <c:if test='${displayPage == "welcome"}'  var='welcomePage'>
        Welcome <input type='radio' name='remotePage' value='welcome' checked /><br/>
      </c:if>
      <c:if test='${not welcomePage}'>
        Welcome <input type='radio' name='remotePage' value='welcome' /><br/>
      </c:if>

      <c:if test='${displayPage == "scoreboard"}'  var='scoreboardPage'>
        Scoreboard <input type='radio' name='remotePage' value='scoreboard'
        checked='true' /> Note that when the scorboard comes up the All
        Teams column will be blank until any scores are entered<br/>
      </c:if>
      <c:if test='${not scoreboardPage}'>
        Scoreboard <input type='radio' name='remotePage' value='scoreboard' /><br/>
      </c:if>

      <c:if test='${displayPage eq "slideshow"}' var='slideshowPage'>
        Slide show <input type='radio' name='remotePage' value='slideshow' checked />
      </c:if>
      <c:if test='${not slideshowPage}'>
        Slide show <input type='radio' name='remotePage' value='slideshow' />
      </c:if>
      <input type='text' name='slideInterval' value='<c:out value="${slideInterval}"/>' size='3'/>
      Seconds to show a slide: <input type='submit' name='changeSlideInterval' value='Update Slide Interval'/><br/>
      <br/>
      
      <c:if test='${displayPage == "playoffs"}'  var='playoffsPage'>
        Playoffs <input type='radio' name='remotePage' value='playoffs' checked='true' />
      </c:if>
      <c:if test='${not playoffsPage}'>
        Playoffs <input type='radio' name='remotePage' value='playoffs' />
      </c:if>
      <b>WARNING: Do not select brackets until all seeding runs have been recorded!</b><br/>

      <c:if test='${displayPage == "special"}'  var='specialPage'>
        Specify page relative to /fll-sw <input type='radio' name='remotePage' value='special' checked /> <input type='text' name='remoteURL' value='<c:out value="${displayURL}"/>'><br/>
      </c:if>
      <c:if test='${not specialPage}'>
        Specify page relative to /fll-sw <input type='radio' name='remotePage' value='special' /> <input type='text' name='remoteURL' value='<c:out value="${displayURL}"/>'><br/>
      </c:if>
          
      <input type='submit' value='Change Display Page'/>
    </form>

          <form action='remoteControl.jsp' method='post'>
            Select the division and run number to display in the remote brackets.
            Division: <select name='division'> 
<%
{
final Iterator divisionIter = divisions.iterator();
while(divisionIter.hasNext()) {
  final String div = (String)divisionIter.next();
  out.print("<option value='" + div + "'");
  if(playoffDivision.equals(div)) {
    out.print(" selected");
  }
  out.println(">" + div + "</option>");
}
}
%>
            </select>
            Playoff Round: <select name='playoffRoundNumber'>
<%
for(int numRounds = 1; numRounds <= numPlayoffRounds; numRounds++) {
  out.print("<option value='" + numRounds + "'");
  if(playoffRunNumber == numRounds) {
    out.print(" selected");
  }
  out.println(">" + numRounds + "</option>");
}
%>
            </select>

            <input type='submit' value='Change values'>
          </form>
            

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
