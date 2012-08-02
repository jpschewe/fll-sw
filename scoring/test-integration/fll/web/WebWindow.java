/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

/**
 * Creates and Handles a New window. Based on code from
 * http://blog.nsissoft.com/
 * 2012/01/10/creating-new-window-in-selenium-2-webdriver-java/
 */
public class WebWindow {

  private final WebDriver driver;

  private String handle;

  private final String name;

  private String parentHandle;

  private static int instanceCount = 0;

  /**
   * Creates a new window for given web driver
   * 
   * @param parent WebDriver instance
   * @param url Initial url to load
   * @return new WebWindow
   */
  public WebWindow(final WebDriver parent,
                   final String url) {

    this.driver = parent;
    parentHandle = parent.getWindowHandle();
    name = createUniqueName();
    handle = createWindow(url);
    // Switch to that window and load the url to wait
    switchToWindow().get(url);
  }

  public String getWindowHandle() {
    return handle;
  }

  public String getParentHandle() {
    return parentHandle;
  }

  public void close() {
    switchToWindow().close();
    handle = "";
    // Switch back to the parent window
    driver.switchTo().window(parentHandle);
  }

  private static String createUniqueName() {
    return "a_Web_Window_"
        + instanceCount++;
  }

  public WebDriver switchToWindow() {
    checkForClosed();
    return driver.switchTo().window(handle);
  }

  public WebDriver switchToParent() {
    checkForClosed();
    return driver.switchTo().window(parentHandle);
  }

  private String createWindow(String url) {

    // Record old handles
    final Set<String> oldHandles = driver.getWindowHandles();
    parentHandle = driver.getWindowHandle();

    // Inject an anchor element
    if (driver instanceof JavascriptExecutor) {
      ((JavascriptExecutor) driver).executeScript(injectAnchorTag(name, url));
    } else {
      throw new RuntimeException("Must have a JavascriptExecutor instance. Find a more capable WebDriver");
    }

    // Click on the anchor element
    driver.findElement(By.id(name)).click();

    handle = getNewHandle(oldHandles);

    return handle;
  }

  private String getNewHandle(final Set<String> oldHandles) {

    final Set<String> newHandles = driver.getWindowHandles();
    newHandles.removeAll(oldHandles);

    // Find the new window
    for (final String handle : newHandles) {
      return handle;
    }

    return null;
  }

  private void checkForClosed() {
    if (handle == null
        || handle.equals(""))
      throw new WebDriverException("Web Window closed or not initialized handle: " + handle);
  }

  private String injectAnchorTag(final String id,
                                 final String url) {
    return String.format("var anchorTag = document.createElement('a'); "
        + "anchorTag.appendChild(document.createTextNode('nwh'));" + "anchorTag.setAttribute('id', '%s');"
        + "anchorTag.setAttribute('href', '%s');" + "anchorTag.setAttribute('target', '_blank');"
        + "anchorTag.setAttribute('style', 'display:block;');"
        + "document.getElementsByTagName('body')[0].appendChild(anchorTag);", id, url);
  }

}
