/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.display;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.util.FLLRuntimeException;
import fll.web.DisplayInfo;
import fll.web.display.Message.MessageType;
import jakarta.websocket.Session;

/**
 * Take care of managing web sockets for the displays.
 */
public final class DisplayHandler {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * UUID for the default display.
   */
  private static final String DEFAULT_DISPLAY_UUID = new UUID(0, 0).toString();

  private final BlockingQueue<WorkItem> workItems = new LinkedBlockingQueue<>();

  private static final SortedMap<String, DisplayData> DISPLAYS = new TreeMap<>();

  private static final Object LOCK = new Object();

  /**
   * Parameter name used to pass the display uuid to the various display pages.
   */
  public static final String DISPLAY_UUID_PARAMETER_NAME = "display_uuid";

  static {
    final DisplayInfo defaultDisplay = new DisplayInfo(DEFAULT_DISPLAY_UUID, DisplayInfo.DEFAULT_DISPLAY_NAME);
    defaultDisplay.setRemotePage(DisplayInfo.WELCOME_REMOTE_PAGE);
    DISPLAYS.put(DEFAULT_DISPLAY_UUID, new DisplayData(defaultDisplay, new DisplaySocket()));
  }

  private DisplayHandler() {
  }

  /**
   * @param uuid from {@link #registerDisplay(String, String, Session)}
   * @return the display information
   * @throws FLLRuntimeException if a display with the specified uuid isn't found
   */
  public static DisplayInfo getDisplay(final String uuid) {
    synchronized (LOCK) {
      if (DISPLAYS.containsKey(uuid)) {
        return DISPLAYS.get(uuid).getInfo();
      } else {
        throw new FLLRuntimeException(String.format("Display with the ID '%s' is not known", uuid));
      }
    }
  }

  /**
   * @return the default display
   */
  public static DisplayInfo getDefaultDisplay() {
    return getDisplay(DEFAULT_DISPLAY_UUID);
  }

  /**
   * @return all {@link DisplayInfo} objects sorted with default first.
   *         Unmodifiable.
   */
  public static List<DisplayInfo> getAllDisplays() {
    synchronized (LOCK) {
      return Collections.unmodifiableList(DISPLAYS.values().stream().map(DisplayData::getInfo)
                                                  .collect(Collectors.toList()));
    }
  }

  /**
   * Register a display or possibly rename an existing display.
   * 
   * @param providedUuid uuid provided by the client, may be null or empty
   * @param name the name of the display
   * @param session the websocket
   */
  public static void registerDisplay(final @Nullable String providedUuid,
                                     final String name,
                                     final Session session) {
    final String uuid;
    if (StringUtils.isBlank(providedUuid)) {
      uuid = UUID.randomUUID().toString();
    } else {
      uuid = providedUuid;
    }

    @Nullable
    DisplayData data;
    synchronized (LOCK) {
      data = DISPLAYS.get(uuid);
      if (null == data) {
        final DisplaySocket socket = new DisplaySocket(session);
        final DisplayInfo info = new DisplayInfo(uuid, name);
        data = new DisplayData(info, socket);
        DISPLAYS.put(uuid, data);
      } else {
        data.info.setName(name);
      }
    }

    send(uuid, data.getSocket(), new AssignUuidMessage(uuid));
    sendCurrentUrl(data);
  }

  /**
   * Send the current URL to all displays.
   */
  public static void sendUpdateUrl() {
    final List<DisplayData> data;
    synchronized (LOCK) {
      data = new LinkedList<>(DISPLAYS.values());
    }
    for (final DisplayData d : data) {
      sendCurrentUrl(d);
    }
  }

  private static void sendCurrentUrl(final DisplayData data) {
    final String url = data.getInfo().getUrl();
    send(data.getInfo().getUuid(), data.getSocket(), new DisplayUrlMessage(url));
  }

  /**
   * Send a message and remove the display if there is an error.
   */
  private static void send(final String uuid,
                           final DisplaySocket socket,
                           final Message message) {
    try {
      socket.sendMessage(message);
    } catch (final IOException e) {
      LOGGER.warn("Error sending message, dropping display with uuid: {}", uuid);
      removeDisplay(uuid);
    }

  }

  /**
   * Note that the display with the specified uuid has been seen now.
   * 
   * @param uuid the uuid of the display
   */
  public static void updateLastSeen(final String uuid) {
    synchronized (LOCK) {
      if (DISPLAYS.containsKey(uuid)) {
        DISPLAYS.get(uuid).getInfo().updateLastSeen();
      } else {
        LOGGER.warn("Got notification to update last seen time for {}, however it's not a known uuid. Ignoring.", uuid);
      }
    }
  }

  private static class WorkItem {
    /* package */ WorkItem(final MessageType type) {
      this.type = type;
    }

    private final MessageType type;

    public final MessageType getType() {
      return type;
    }
  }

  /**
   * Remove a display.
   * 
   * @param uuid uuid that the display was registered with
   * @see #registerDisplay(String, String, Session)
   */
  public static void removeDisplay(final String uuid) {
    synchronized (LOCK) {
      final DisplayData data = DISPLAYS.get(uuid);
      if (null != data) {
        internalRemoveDisplay(uuid, data);
      }
    }
  }

  private static void internalRemoveDisplay(final String uuid,
                                            final DisplayData data) {
    data.getSocket().close();
    DISPLAYS.remove(uuid);
  }

  private static final class DisplayData {
    /* package */ DisplayData(final DisplayInfo info,
                              final DisplaySocket socket) {
      this.info = info;
      this.socket = socket;
    }

    private final DisplayInfo info;

    public DisplayInfo getInfo() {
      return info;
    }

    private final DisplaySocket socket;

    public DisplaySocket getSocket() {
      return socket;
    }
  }

}
