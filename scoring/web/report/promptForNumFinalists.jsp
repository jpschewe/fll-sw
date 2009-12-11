<%@ include file="/WEB-INF/jspf/init.jspf" %>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Finalist Scheduling</title>
    <script type='text/javascript'>
      function check_text() {
        var value = document.getElementById('num-finalists').value;
        if(!isInteger(value)) {
          alert("You must enter a number")
          document.getElementById('num-finalists').value = ""
        }   
      }
    </script>
  </head>

  <body>
    <p>${message}</p>
    <c:remove var="message"/>

    <p>How many teams should be picked from each score group (per division) for finalists?</p>
    <form action='FinalistSchedulerUI'>
      <input type='hidden' name='init' value='1'/>
      <input type="text" id='num-finalists' name="num-finalists" onblur='check_text' size="10"/>
      <input type='submit' />
    </form>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>
