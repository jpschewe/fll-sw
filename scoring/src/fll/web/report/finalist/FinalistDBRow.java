/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.util.Calendar;
import java.util.Date;

/**
 * Mirrors javascript class in schedule.js. Variable names need to match the
 * javascript/JSON.
 */
final class FinalistDBRow {
  private String categoryName;

  public String getCategoryName() {
    return categoryName;
  }

  private int hour;

  public int getHour() {
    return hour;
  }

  private int minute;

  public int getMinute() {
    return minute;
  }

  public Date getTime() {
    final Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR, getHour());
    cal.set(Calendar.MINUTE, getMinute());
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    return cal.getTime();
  }

  private int teamNumber;

  public int getTeamNumber() {
    return teamNumber;
  }
}
//
// private static class DateTimeDeserializer implements JsonDeserializer<Date> {
// private static final DateFormat mFormat = new
// SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//
// public Date deserialize(JsonElement json,
// Type typeOfT,
// JsonDeserializationContext context) throws JsonParseException {
// final String dateStr = json.getAsJsonPrimitive().getAsString();
// try {
// return mFormat.parse(dateStr);
// } catch (final ParseException e) {
// LOGGER.error("Unparsable date: '"
// + dateStr + "'");
// return null;
// }
// }
// }