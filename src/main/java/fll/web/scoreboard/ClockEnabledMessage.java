/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.web.scoreboard;

/**
 * Let the scoreboard know if the clock should be enabled.
 */
final class ClockEnabledMessage extends Message {

  /**
   * @param clockEnabled {@link #isClockEnabled()}
   */
  ClockEnabledMessage(final boolean clockEnabled) {
    super(Message.MessageType.CLOCK_ENABLED);
    this.clockEnabled = clockEnabled;
  }

  private boolean clockEnabled;

  /**
   * @return is the clock enabled
   */
  public boolean isClockEnabled() {
    return clockEnabled;
  }

}
