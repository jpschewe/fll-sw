<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>

<%@ page import="java.sql.Connection" %>      

<%@ page import="fll.Utilities" %>
<%@ page import="fll.db.Queries" %>
<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="javax.sql.DataSource" %>
      
<%
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
      
final List<String> divisions = Queries.getDivisions(connection);
pageContext.setAttribute("divisions", divisions);

if(null == application.getAttribute("playoffDivision") && !divisions.isEmpty()) {
  application.setAttribute("playoffDivision", divisions.get(0));
}
if(null == application.getAttribute("playoffRoundNumber")) {
  application.setAttribute("playoffRoundNumber", new Integer(1));
}
if(null == application.getAttribute("slideShowInterval")) {
  application.setAttribute("slideShowInterval", new Integer(10));
}

pageContext.setAttribute("numPlayoffRounds", Queries.getNumPlayoffRounds(connection));

%>
      
<html>
  <head>
    <title><x:out select="$challengeDocument/fll/@title"/> (Display Controller)</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    
<script type="text/javascript">
  function display(id) {
    document.getElementById(id).style.display = "block";
  }
  function hide(id) {
    document.getElementById(id).style.display = "none";
  }
</script>
    
  </head>          
  <body>

  <c:if test='${param.whichDisplay == "ALL"}'>
    <%-- Unset all specific display variables --%>  
    <c:if test="${not empty displayNames}">
      <c:forEach items="${displayNames}" var="displayName">
        <%-- Can't use EL inside the "var" attribute.
        <c:remove var='${displayName}-displayPage' scope='application'/>
        <c:remove var='${displayName}-displayURL' scope='application'/>
        --%>
        <% 
        application.removeAttribute(pageContext.getAttribute("displayName") + "_displayPage");
        application.removeAttribute(pageContext.getAttribute("displayName") + "_displayURL");
        application.removeAttribute(pageContext.getAttribute("displayName") + "_slideShowInterval");
        %>
      </c:forEach>
    </c:if>
  </c:if>
  
  <%-- decide if we're working with a specific display or the default --%>
  <c:choose>
    <c:when test='${param.whichDisplay == "ALL" || param.whichDisplay == "DEFAULT"}'>
      <c:set var="displayPageKey" value="displayPage"/>
      <c:set var="displayURLKey" value="displayURL"/>
      <c:set var="slideShowIntervalKey" value="slideShowInterval"/>
      <c:set var="playoffDivisionKey" value="playoffDivision"/>
      <c:set var="playoffRoundNumberKey" value="playoffRoundNumber"/>
    </c:when>
    <c:otherwise>
      <c:set var="displayPageKey" value="${param.whichDisplay}_displayPage"/>
      <c:set var="displayURLKey" value="${param.whichDisplay}_displayURL"/>
      <c:set var="slideShowIntervalKey" value="${param.whichDisplay}_slideShowInterval"/>
      <c:set var="playoffDivisionKey" value="${param.whichDisplay}_playoffDivision"/>
      <c:set var="playoffRoundNumberKey" value="${param.whichDisplay}_playoffRoundNumber"/>
    </c:otherwise>
  </c:choose>
    
  <%-- set the appropriate application variables --%>
  <c:if test="${empty pageScope[displayPageKey]}">
    <%--<c:set var='${pageScope[displayPageKey]}' value='welcome' scope='application'/>--%>
    <% application.setAttribute((String)pageContext.getAttribute("displayPageKey"), "welcome"); %>
  </c:if>
  
  <%-- common parameters --%>        
  <c:if test="${not empty param.remotePage}">
    <%--<c:set var='${pageScope[displayPageKey]}' value='${param.remotePage}' scope='application'/>--%>
    <% application.setAttribute((String)pageContext.getAttribute("displayPageKey"), request.getParameter("remotePage")); %>
  </c:if>
  <c:if test="${not empty param.remoteURL}">
    <%--<c:set var='${pageScope[displayURLKey]}' value='${param.remoteURL}' scope='application'/>--%>
    <% application.setAttribute((String)pageContext.getAttribute("displayURLKey"), request.getParameter("remoteURL")); %>
    
  </c:if>
  
  <%-- slide show parameters --%>
  <c:choose>
    <c:when test="${empty param.slideInterval}">
      <%--<c:set var='${pageScope[slideShowIntervalKey]}' value='10' scope='application'/>--%>
      <% application.setAttribute((String)pageContext.getAttribute("slideShowIntervalKey"), 10); %>
    </c:when>
    <c:otherwise>
      <%--<c:set var='${pageScope[slideShowIntervalKey]}' value='${param.slideInterval}' scope='application'/>--%>
      <% application.setAttribute((String)pageContext.getAttribute("slideShowIntervalKey"), Utilities.NUMBER_FORMAT_INSTANCE.parse(request.getParameter("slideInterval"))); %>
    </c:otherwise>
  </c:choose>
  
  <%-- Playoff parameters --%>
  <c:if test="${not empty param.division}">
    <% application.setAttribute((String)pageContext.getAttribute("playoffDivisionKey"), request.getParameter("division")); %>
  </c:if>
  <c:if test="${not empty param.playoffRoundNumber}">
    <% application.setAttribute((String)pageContext.getAttribute("playoffRoundNumberKey"), Utilities.NUMBER_FORMAT_INSTANCE.parse(request.getParameter("playoffRoundNumber"))); %>
  </c:if>

    <h1><x:out select="$challengeDocument/fll/@title"/> (Display Controller)</h1>

    <p><i>Just set parameters for display ${param.whichDisplay}.</i></p>
    
    <p>This page is used to control what page is currently visible on the
    display screen.  Note that it takes some time for the display to
    change, up to 2 minutes.</p>
    
    <p>The information below shows the values for the "default" context.</p>

    <form name='remote' action='remoteControl.jsp' method='post'>
    
      <!-- welcome -->
      <label for="welcome">Welcome</label>
      <c:if test='${displayPage == "welcome"}'  var='welcomePage'>
        <input type='radio' id='welcome' name='remotePage' value='welcome' checked /><br/>
      </c:if>
      <c:if test='${not welcomePage}'>
        <input type='radio' id='welcome' name='remotePage' value='welcome' /><br/>
      </c:if>


      <!--  scoreboard -->
      <label for='scoreboard'>Scoreboard</label>
      <c:if test='${displayPage == "scoreboard"}'  var='scoreboardPage'>
        <input type='radio' id='scoreboard' name='remotePage' value='scoreboard'
        checked /> Note that when the scoreboard comes up the All
        Teams column will be blank until any scores are entered<br/>
      </c:if>
      <c:if test='${not scoreboardPage}'>
        <input type='radio' id='scoreboard' name='remotePage' value='scoreboard' /><br/>
      </c:if>


      <!--  slide show -->
      <label for="slideshow">Slide show</label>
      <c:if test='${displayPage eq "slideshow"}' var='slideshowPage'>
        <input type='radio' id="slideshow" name='remotePage' value='slideshow' checked />
      </c:if>
      <c:if test='${not slideshowPage}'>
        <input type='radio' id="slideshow" name='remotePage' value='slideshow' />
      </c:if>
      
      Seconds to show a slide:      
      <input type='text' name='slideInterval' value='<c:out value="${slideShowInterval}"/>' size='3'/>
      <br/>
      
      
      <!--  playoffs -->
      <label for='playoffs'>Playoffs</label>
      <c:if test='${displayPage == "playoffs"}'  var='playoffsPage'>
        <input type='radio' id='playoffs' name='remotePage' value='playoffs' checked />
      </c:if>
      <c:if test='${not playoffsPage}'>
        <input type='radio' id='playoffs' name='remotePage' value='playoffs' />
      </c:if>
      <b>WARNING: Do not select brackets until all seeding runs have been recorded!</b><br/>
            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Division: <select name='division'>
            <c:forEach items="${divisions}" var="division">
              <c:choose>
                <c:when test="${division == playoffDivision}">
                  <option value="${division}" selected>${division}</option>
                </c:when>
                <c:otherwise>
                  <option value="${division}">${division}</option>
                </c:otherwise>
              </c:choose>
            </c:forEach> 
            </select>
            
            Playoff Round: <select name='playoffRoundNumber'>
            <c:forEach begin="1" end="${numPlayoffRounds}" var="numRounds">
              <c:choose>
                <c:when test="${numRounds == playoffRoundNumber}">
                  <option value="${numRounds}" selected>${numRounds}</option>
                </c:when>
                <c:otherwise>
                  <option value="${numRounds}">${numRounds}</option>
                </c:otherwise>
              </c:choose>
            </c:forEach>
            </select><br/>


      <!-- special page -->
      <label for='special'>Specify page relative to /fll-sw</label>
      <c:if test='${displayPage == "special"}'  var='specialPage'>
        <input type='radio' id='special' name='remotePage' value='special' checked /> <input type='text' name='remoteURL' value='<c:out value="${displayURL}"/>'>
      </c:if>
      <c:if test='${not specialPage}'>
        <input type='radio' id='special' name='remotePage' value='special' /> <input type='text' name='remoteURL' value='<c:out value="${displayURL}"/>'>
      </c:if>
      <br/>
          
      Apply changes to display:
      <select name="whichDisplay">
        <option value="DEFAULT">Default</option>
        <option value="ALL">All</option>
        <c:if test="${not empty displayNames}">
          <c:forEach items="${displayNames}" var="displayName">
            <option value="${displayName}">${displayName}</option>
          </c:forEach>
        </c:if>
      </select>
      <a href='javascript:display("whichDisplayHelp")'>[help]</a>
 <div id='whichDisplayHelp' class='help' style='display:none'>
 "Default" will change the display for all displays that haven't had a
 specific value set previously (this is what you want most of the time).
 "All" will reset all displays to follow the default values.
 Selecting another display name will set the values for that display.<br/>
 <a href='javascript:hide("whichDisplayHelp")'>[hide]</a></div> 
       
      <br/>
      
      <input type='submit' value='Submit'/>
      
    </form>
            

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
