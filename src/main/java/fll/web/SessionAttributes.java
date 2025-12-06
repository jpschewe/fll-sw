/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.util.FLLRuntimeException;
import jakarta.servlet.http.HttpSession;

/**
 * Keys for session variables. Each key has an associated accessor function as
 * well that helps with type safety.
 *
 * @author jpschewe
 */
public final class SessionAttributes {

  private SessionAttributes() {
    // no instances
  }

  /**
   * A {@link String} that is a message to display. This is set in many pages
   * and servlets to pass a message onto the next page to display to the user.
   */
  public static final String MESSAGE = "message";

  /**
   * @param session where to get the information
   * @return the message to display to the user
   */
  public static @Nullable String getMessage(final HttpSession session) {
    return getAttribute(session, MESSAGE, String.class);
  }

  /**
   * Append some text to the message in the session.
   *
   * @param session where to get/store the information
   * @param msg the text to append to the message
   */
  public static void appendToMessage(final HttpSession session,
                                     final String msg) {
    final String prevMessage = getMessage(session);
    final StringBuilder builder = new StringBuilder();
    if (null != prevMessage) {
      builder.append(prevMessage);
    }
    builder.append(msg);
    session.setAttribute(MESSAGE, builder.toString());
  }

  /**
   * Key in the session used to store the URL to redirect to after the current
   * operation completes.
   * The type is a {@link String}.
   */
  public static final String REDIRECT_URL = "redirect_url";

  /**
   * @param session where to get the information
   * @return the URL to send the user to after the current operation completes
   */
  public static @Nullable String getRedirectURL(final HttpSession session) {
    return getAttribute(session, REDIRECT_URL, String.class);
  }

  /**
   * Clear the redirect URL.
   * 
   * @param session what to modify
   */
  public static void clearRedirectURL(final HttpSession session) {
    session.removeAttribute(REDIRECT_URL);
  }

  /**
   * Key in the session used to store the display name.
   *
   * @see fll.web.display.DisplayInfo
   */
  public static final String DISPLAY_NAME = "displayName";

  /**
   * Get the name for the current display.
   *
   * @param session where to get the information from
   * @return may be null
   */
  public static @Nullable String getDisplayName(final HttpSession session) {
    return getAttribute(session, DISPLAY_NAME, String.class);
  }

  /**
   * Get session attribute and send appropriate error if type is wrong. Note
   * that null is always valid.
   *
   * @param session where to get the attribute
   * @param attribute the attribute to get
   * @param clazz the expected type
   * @param <T> the expected type
   * @return the attribute value
   */
  public static <T> @Nullable T getAttribute(final HttpSession session,
                                             final String attribute,
                                             final Class<T> clazz) {
    final Object o = session.getAttribute(attribute);
    if (o == null
        || clazz.isInstance(o)) {
      return clazz.cast(o);
    } else {
      throw new ClassCastException(String.format("Expecting session attribute '%s' to be of type '%s', but was of type '%s'",
                                                 attribute, clazz, o.getClass()));
    }
  }

  /**
   * Get a session attribute and throw a {@link NullPointerException} if it's
   * null.
   *
   * @param session where to get the attribute from
   * @param attribute the name of the attribute to retrieve
   * @param <T> the type of value stored in the attribute
   * @param clazz the type of value stored in the attribute
   * @return the attribute value
   * @see #getAttribute(HttpSession, String, Class)
   */
  public static <T> T getNonNullAttribute(final HttpSession session,
                                          final String attribute,
                                          final Class<T> clazz) {
    final T retval = getAttribute(session, attribute, clazz);
    if (null == retval) {
      throw new NullPointerException(String.format("Session attribute %s is null when it's not expected to be",
                                                   attribute));
    }
    return retval;
  }

  /**
   * Stores the current login information. This is an instance of
   * {@link AuthenticationContext}.
   */
  public static final String AUTHENTICATION = "authentication";

  /**
   * Get the authentication information. If there is no authentication information
   * {@link AuthenticationContext#notLoggedIn()} is used.
   * 
   * @param session used to get the variable
   * @return the authentication information
   */
  public static AuthenticationContext getAuthentication(final HttpSession session) {
    final AuthenticationContext auth = getAttribute(session, AUTHENTICATION, AuthenticationContext.class);
    if (null == auth) {
      final AuthenticationContext newAuth = AuthenticationContext.notLoggedIn();
      session.setAttribute(AUTHENTICATION, newAuth);
      return newAuth;
    } else {
      return auth;
    }
  }

  /**
   * The key in the session to get form parameters stored by login and should be
   * passed to the next page.
   */
  public static final String STORED_PARAMETERS = "stored_parameters";

  /**
   * Key to check in request parameters to find a workflow ID.
   * 
   * @see #getWorkflowAttribute(HttpSession, String, String, Class)
   */
  public static final String WORKFLOW_ID = "workflow_id";

  /**
   * Get a workflow session attribute and send appropriate error if type is wrong.
   * Note that null is always valid.
   *
   * @param session where to find the workflow session
   * @param workflowId the identifier for the workflow
   * @param attribute the attribute to get
   * @param clazz the expected type
   * @param <T> the expected type
   * @return the attribute value or null if the attribute or workflow session
   *         doesn't exist
   */
  public static <T> @Nullable T getWorkflowAttribute(final HttpSession session,
                                                     final String workflowId,
                                                     final String attribute,
                                                     final Class<T> clazz) {
    final @Nullable WorkflowSession workflowSession = getAttribute(session, getWorkflowSessionKey(workflowId),
                                                                   WorkflowSession.class);
    if (null == workflowSession) {
      return null;
    }

    return workflowSession.getAttribute(attribute, clazz);
  }

  /**
   * Get a session attribute and throw a {@link NullPointerException} if it's
   * null.
   *
   * @param session where to get the attribute from
   * @param workflowId the identifier for the workflow
   * @param attribute the name of the attribute to retrieve
   * @param <T> the type of value stored in the attribute
   * @param clazz the type of value stored in the attribute
   * @return the attribute value
   * @see #getWorkflowAttribute(HttpSession, String, String, Class)
   */
  public static <T> T getNonNullWorkflowAttribute(final HttpSession session,
                                                  final String workflowId,
                                                  final String attribute,
                                                  final Class<T> clazz) {
    final T retval = getWorkflowAttribute(session, workflowId, attribute, clazz);
    if (null == retval) {
      throw new NullPointerException(String.format("Workflow session attribute %s is null in %s when it's not expected to be",
                                                   attribute, workflowId));
    }
    return retval;
  }

  /**
   * Set a workflow session attribute.
   * 
   * @param session where the workflow sessions are stored
   * @param workflowId the identifier for the workflow
   * @param attribute the attribute key
   * @param value the value to set
   * @see #getWorkflowAttribute(HttpSession, String, String, Class)
   */
  public static void setWorkflowAttribute(final HttpSession session,
                                          final String workflowId,
                                          final String attribute,
                                          final Object value) {
    final @Nullable WorkflowSession workflowSession = getAttribute(session, getWorkflowSessionKey(workflowId),
                                                                   WorkflowSession.class);
    if (null == workflowSession) {
      throw new FLLRuntimeException(String.format("Workflow with id %s does not exist. Cannot set attribute %s on a non-existent session",
                                                  workflowId, attribute));
    }

    workflowSession.setAttribute(attribute, value);
  }

  /**
   * Create workflow specific session.
   * 
   * @param session the session that the workflow is associated with
   * @return the ID for the workflow session
   * @see #getWorkflowAttribute(HttpSession, String, String, Class)
   * @see #setWorkflowAttribute(HttpSession, String, String, Object)
   */
  public static String createWorkflowSession(final HttpSession session) {
    final WorkflowSession workflowSession = new WorkflowSession();
    session.setAttribute(getWorkflowSessionKey(workflowSession.getId()), workflowSession);
    return workflowSession.getId();
  }

  /**
   * Clean up a workflow session created by
   * {@link #createWorkflowSession(HttpSession)}.
   * 
   * @param session the session
   * @param workflowId the workflow session id
   */
  public static void deleteWorkflowSession(final HttpSession session,
                                           final String workflowId) {
    session.removeAttribute(getWorkflowSessionKey(workflowId));
  }

  private static final String getWorkflowSessionKey(final String workflowId) {
    return ApplicationAttributes.PREFIX
        + workflowId;
  }

  private static final class WorkflowSession implements Serializable {
    private final ConcurrentHashMap<String, Object> data = new ConcurrentHashMap<>();

    private final String id;

    public String getId() {
      return id;
    }

    public WorkflowSession() {
      id = UUID.randomUUID().toString();
    }

    public <T> @Nullable T getAttribute(final String attribute,
                                        final Class<T> clazz) {
      final @Nullable Object o = data.get(attribute);
      if (o == null
          || clazz.isInstance(o)) {
        return clazz.cast(o);
      } else {
        throw new ClassCastException(String.format("Expecting workflow session attribute '%s' to be of type '%s', but was of type '%s'",
                                                   attribute, clazz, o.getClass()));
      }
    }

    public void setAttribute(final String attribute,
                             final Object value) {
      data.put(attribute, value);
    }
  }

}
