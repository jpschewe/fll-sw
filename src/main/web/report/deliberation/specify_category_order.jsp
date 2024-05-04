<%@ include file="/WEB-INF/jspf/init.jspf"%>

<!DOCTYPE HTML>
<html>
<head>


<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type='text/javascript'
    src="<c:url value='/extlib/js-joda/js-joda.min.js'/>"></script>


<script type='text/javascript' src="<c:url value='/js/fll-objects.js'/>"></script>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type='text/javascript' src="<c:url value='/js/fll-objects.js'/>"></script>

<script type='text/javascript' src="<c:url value='/js/fll-storage.js'/>"></script>

<script type='text/javascript'
    src="<c:url value='/report/finalist/finalist.js'/>"></script>

<script type='text/javascript' src='specify_category_order.js'></script>

<title>Deliberation Category Order</title>
</head>

<body>

    <p>
        Select the appropriate award group:
        <select id='award-groups'></select>
    </p>

    <ul id="category-order"></ul>

    <button type='button' id='save'>Save Category Order</button>

    <div class="fll-sw-ui-dialog fll-sw-ui-inactive"
        id="wait-dialog-load">
        <div>
            <div id='wait-dialog_text'>Loading data. Please
                wait...</div>
        </div>
    </div>

    <div class="fll-sw-ui-dialog fll-sw-ui-inactive"
        id="wait-dialog-save">
        <div>
            <div id='wait-dialog_text'>Saving data. Please wait...</div>
        </div>
    </div>
</body>
</html>
