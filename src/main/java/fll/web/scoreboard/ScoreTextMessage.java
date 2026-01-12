/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.web.scoreboard;

/**
 * Update the text on the scoreboard
 */
final class ScoreTextMessage extends Message {

  /**
   * @param text {@link #getText()}
   */
  ScoreTextMessage(final String text) {
    super(Message.MessageType.SCORE_TEXT);
    this.text = text;
  }

  private final String text;

  /**
   * @return the text to display
   */
  public String getText() {
    return text;
  }

}
