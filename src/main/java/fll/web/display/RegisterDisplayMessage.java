package fll.web.display;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Initial message sent from the client to the server to register the display.
 */
/* package */ final class RegisterDisplayMessage extends UuidMessage {

  /**
   * @param uuid {@link #getUuid()}
   * @param name {@link #getName()}
   */
  RegisterDisplayMessage(@JsonProperty("uuid") final String uuid,
                         @JsonProperty("name") final String name) {
    super(Message.MessageType.REGISTER_DISPLAY, uuid);
    this.name = name;
  }

  private final String name;

  /**
   * @return name of the display
   */
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return String.format("class: %s uuid: %s name: %s", getClass().getSimpleName(), getUuid(), getName());
  }

}
