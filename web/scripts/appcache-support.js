// Check if the application cache is supported over this connection or if SSL support needs to be enabled

function appCacheStatusToString(status) {
  if (status == window.applicationCache.UPDATEREADY) {
    return "UPDATEREADY";
  } else if (status == window.applicationCache.UNCACHED) {
    return "UNCACHED";
  } else if (status == window.applicationCache.IDLE) {
    return "IDLE";
  } else if (status == window.applicationCache.CHECKING) {
    return "CHECKING";
  } else if (status == window.applicationCache.DOWNLOADING) {
    return "DOWNLOADING";
  } else if (status == window.applicationCache.OBSOLETE) {
    return "OBSOLETE";
  } else {
    return "Unknown - " + status;
  }
}

function appCacheLog(str) {
  if (typeof (console) != 'undefined') {
    console.log(str);
  }
}

var appCacheAlertFired = false;

function handleCacheEvent(e) {
  appCacheLog("Got cache event: " + e + " status: "
      + appCacheStatusToString(window.applicationCache.status));

  if (window.applicationCache.status == window.applicationCache.UNCACHED) {
    if (!appCacheAlertFired) {
      appCacheAlertFired = true;
      alert("After closing this dialog go back and follow the instructions for connecting via SSL if you want to use this application offline.");
    }
  }
}

function handleCacheError(e) {
  appCacheLog('Error: Cache failed to update!: ' + e + " status: "
      + appCacheStatusToString(window.applicationCache.status));
};

var appCache = window.applicationCache;

// Fired after the first cache of the manifest.
appCache.addEventListener('cached', handleCacheEvent, false);

// Checking for an update. Always the first event fired in the sequence.
appCache.addEventListener('checking', handleCacheEvent, false);

// An update was found. The browser is fetching resources.
appCache.addEventListener('downloading', handleCacheEvent, false);

// The manifest returns 404 or 410, the download failed,
// or the manifest changed while the download was in progress.
appCache.addEventListener('error', handleCacheError, false);

// Fired after the first download of the manifest.
appCache.addEventListener('noupdate', handleCacheEvent, false);

// Fired if the manifest file returns a 404 or 410.
// This results in the application cache being deleted.
appCache.addEventListener('obsolete', handleCacheEvent, false);

// Fired for each resource listed in the manifest as it is being fetched.
appCache.addEventListener('progress', handleCacheEvent, false);

// Fired when the manifest resources have been newly redownloaded.
appCache.addEventListener('updateready', handleCacheEvent, false);
