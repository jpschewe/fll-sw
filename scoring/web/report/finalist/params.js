/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function handleCacheEvent(e) {
	console.log("cache event: " + e.type);
	var appCache = window.applicationCache;
	switch (appCache.status) {
	  case appCache.UNCACHED: // UNCACHED == 0
	    console.log('cache state:UNCACHED');
	    break;
	  case appCache.IDLE: // IDLE == 1
	    console.log('cache state:IDLE');
	    $("#cache-ready").show();
	    break;
	  case appCache.CHECKING: // CHECKING == 2
	    console.log('cache state:CHECKING');
	    break;
	  case appCache.DOWNLOADING: // DOWNLOADING == 3
	    console.log('cache state:DOWNLOADING');
	    break;
	  case appCache.UPDATEREADY:  // UPDATEREADY == 4
	    console.log('cache state:UPDATEREADY');
	    $("#cache-ready").show();
	    break;
	  case appCache.OBSOLETE: // OBSOLETE == 5
	    console.log('cache state:OBSOLETE');
	    break;
	  default:
	    return 'cache state:UKNOWN CACHE STATUS';
	    break;
	};
}

function setupAppCache() {
	$("#cache-ready").hide();
	
	var appCache = window.applicationCache;
	if (!appCache) {
		alert("Your browser doesn't support application caching. This app cannot be run offline");
		return;
	}

	// Fired after the first cache of the manifest.
	appCache.addEventListener('cached', handleCacheEvent, false);

	// Checking for an update. Always the first event fired in the sequence.
	appCache.addEventListener('checking', handleCacheEvent, false);

	// An update was found. The browser is fetching resources.
	appCache.addEventListener('downloading', handleCacheEvent, false);

	// The manifest returns 404 or 410, the download failed,
	// or the manifest changed while the download was in progress.
	appCache.addEventListener('error', handleCacheEvent, false);

	// Fired after the first download of the manifest.
	appCache.addEventListener('noupdate', handleCacheEvent, false);

	// Fired if the manifest file returns a 404 or 410.
	// This results in the application cache being deleted.
	appCache.addEventListener('obsolete', handleCacheEvent, false);

	// Fired for each resource listed in the manifest as it is being fetched.
	appCache.addEventListener('progress', handleCacheEvent, false);

	// Fired when the manifest resources have been newly redownloaded.
	appCache.addEventListener('updateready', handleCacheEvent, false);
	
	appCache.addEventListener('error', function(e) {
		console.log("Error loading the appcache manifest");
	}, false);

	if (appCache.status == appCache.UPDATEREADY || appCache.status == appCache.IDLE) {
		console.log("poll: cache ready");
		$("#cache-ready").show();
	}
}

$(document).ready(
		function() {
			setupAppCache();

			$("#divisions").empty();

			$.each($.finalist.getDivisions(), function(i, division) {
				console.log("Division " + division);
				var selected = "";
				if (division == $.finalist.getCurrentDivision()) {
					selected = " selected ";
				}
				var divisionOption = $("<option value='" + i + "'" + selected
						+ ">" + division + "</option>");
				$("#divisions").append(divisionOption);
			});
			$.finalist.setCurrentDivision($.finalist.getDivisionByIndex($(
					"#divisions").val()));

			$("#hour").val($.finalist.getStartHour());
			$("#minute").val($.finalist.getStartMinute());
			$("#duration").val($.finalist.getDuration());

			$("#hour").change(function() {
				var hour = parseInt($(this).val(), 10);
				if (isNaN(hour)) {
					alert("Hour must be an integer");
					$("#hour").val($.finalist.getStartHour());
				} else {
					$.finalist.setStartHour(hour);
				}
			});

			$("#minute").change(function() {
				var minute = parseInt($(this).val(), 10);
				if (isNaN(minute)) {
					alert("Minute must be an integer");
					$("#minute").val($.finalist.getStartMinute());
				} else {
					$.finalist.setStartMinute(minute);
				}
			});

			$("#duration").change(function() {
				var duration = parseInt($(this).val(), 10);
				if (isNaN(duration)) {
					alert("Duration must be an integer");
					$("#duration").val($.finalist.getDuration());
				} else {
					$.finalist.setDuration(duration);
				}
			});

			$("#divisions").change(function() {
				var divIndex = $(this).val();
				var div = $.finalist.getDivisionByIndex(divIndex);
				$.finalist.setCurrentDivision(div);
			});

			$.finalist.displayNavbar();

		}); // end ready function
