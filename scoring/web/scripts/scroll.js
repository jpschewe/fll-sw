// autoscroll a page and when the end of page is reached reload the page

var scrollTimer;
var scrollAmount = 2; // scroll by 100 pixels each time
var documentYposition = 0;
var scrollPause = 100; // amount of time, in milliseconds, to pause between
						// scrolls

// http://www.evolt.org/article/document_body_doctype_switching_and_more/17/30655/index.html
function getScrollPosition() {
	if (window.pageYOffset) {
		return window.pageYOffset;
	} else if (document.documentElement && document.documentElement.scrollTop) {
		return document.documentElement.scrollTop;
	} else if (document.body) {
		return document.body.scrollTop;
	}
}

function myScroll() {
	documentYposition += scrollAmount;
	window.scrollBy(0, scrollAmount);
	if (getScrollPosition() + 300 < documentYposition) { // wait 300 pixels
															// until we refresh
		window.clearInterval(scrollTimer);
		window.scroll(0, 0); // scroll back to top and then refresh
		location.href = location.href;
	}
}

function startScrolling() {
	scrollTimer = window.setInterval('myScroll()', scrollPause);
}
