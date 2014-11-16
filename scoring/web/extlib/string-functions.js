/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

String.repeat = function(chr, count) {
	var str = "";
	for (var x = 0; x < count; x++) {
		str += chr;
	}

	return str;
};

String.prototype.padL = function(width, pad) {
	if (!width || width < 1)
		return this;

	if (!pad)
		pad = " ";
	var length = width - this.length;
	if (length < 1)
		return this.substr(0, width);

	return (String.repeat(pad, length) + this).substr(0, width);
};
