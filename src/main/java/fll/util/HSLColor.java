/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.awt.Color;

/**
 * The HSLColor class provides methods to manipulate HSL (Hue, Saturation
 * Luminance) values to create a corresponding Color object using the RGB
 * ColorSpace.
 * The HUE is the color, the Saturation is the purity of the color (with
 * respect to grey) and Luminance is the brightness of the color (with respect
 * to black and white)
 * The Hue is specified as an angel between 0 - 360 degrees where red is 0,
 * green is 120 and blue is 240. In between you have the colors of the rainbow.
 * Saturation is specified as a percentage between 0 - 100 where 100 is fully
 * saturated and 0 approaches gray. Luminance is specified as a percentage
 * between 0 - 100 where 0 is black and 100 is white.
 * In particular the HSL color space makes it easier change the Tone or Shade
 * of a color by adjusting the luminance value.
 * Based on code from https://tips4java.wordpress.com/2009/07/05/hsl-color/
 */
public class HSLColor {

  private final Color rgb;

  private final float[] hsl;

  private final float alpha;

  /**
   * Create a HSLColor object using an RGB Color object.
   *
   * @param rgb the RGB Color object
   */
  public HSLColor(final Color rgb) {
    this.rgb = rgb;
    hsl = fromRGB(rgb);
    alpha = rgb.getAlpha()
        / 255.0f;
  }

  /**
   * Create a HSLColor object using individual HSL values and a default
   * alpha value of 1.0.
   *
   * @param h is the Hue value in degrees between 0 - 360
   * @param s is the Saturation percentage between 0 - 100
   * @param l is the Lumanance percentage between 0 - 100
   */
  public HSLColor(final float h,
                  final float s,
                  final float l) {
    this(h, s, l, 1.0f);
  }

  /**
   * Create a HSLColor object using individual HSL values.
   *
   * @param h the Hue value in degrees between 0 - 360
   * @param s the Saturation percentage between 0 - 100
   * @param l the Luminance percentage between 0 - 100
   * @param alpha the alpha value between 0 - 1
   */
  public HSLColor(final float h,
                  final float s,
                  final float l,
                  final float alpha) {
    if (h < 0
        || h > 360) {
      throw new IllegalArgumentException("hue must be between 0 and 360");
    }
    if (s < 0
        || s > 100) {
      throw new IllegalArgumentException("saturation must be between 0 and 100");
    }
    if (l < 0
        || l > 100) {
      throw new IllegalArgumentException("luminance must be between 0 and 100");
    }
    if (alpha < 0
        || alpha > 1) {
      throw new IllegalArgumentException("Alpha must be between 0 and 1");
    }

    hsl = new float[] { h, s, l };
    this.alpha = alpha;
    rgb = toRGB(hsl, alpha);
  }

  /**
   * Create a RGB Color object based on this HSLColor with a different
   * Hue value. The degrees specified is an absolute value.
   *
   * @param degrees - the Hue value between 0 - 360
   * @return the RGB Color object
   */
  public Color adjustHue(final float degrees) {
    if (degrees < 0
        || degrees > 360) {
      throw new IllegalArgumentException("degrees must be between 0 and 360");
    }
    return toRGB(degrees, hsl[1], hsl[2], alpha);
  }

  /**
   * Create a RGB Color object based on this HSLColor with a different
   * Luminance value. The percent specified is an absolute value.
   *
   * @param percent - the Luminance value between 0 - 100
   * @return the RGB Color object
   */
  public Color adjustLuminance(final float percent) {
    if (percent < 0
        || percent > 100) {
      throw new IllegalArgumentException("percent must be between 0 and 100");
    }
    return toRGB(hsl[0], hsl[1], percent, alpha);
  }

  /**
   * Create a RGB Color object based on this HSLColor with a different
   * Saturation value. The percent specified is an absolute value.
   *
   * @param percent - the Saturation value between 0 - 100
   * @return the RGB Color object
   */
  public Color adjustSaturation(final float percent) {
    if (percent < 0
        || percent > 100) {
      throw new IllegalArgumentException("percent must be between 0 and 100");
    }
    return toRGB(hsl[0], percent, hsl[2], alpha);
  }

  /**
   * Create a RGB Color object based on this HSLColor with a different
   * Shade. Changing the shade will return a darker color. The percent
   * specified is a relative value.
   *
   * @param percent - the value between 0 - 100
   * @return the RGB Color object
   */
  public Color adjustShade(final float percent) {
    if (percent < 0
        || percent > 100) {
      throw new IllegalArgumentException("percent must be between 0 and 100");
    }

    final float multiplier = (100.0f
        - percent)
        / 100.0f;
    final float l = Math.max(0.0f, hsl[2]
        * multiplier);

    return toRGB(hsl[0], hsl[1], l, alpha);
  }

  /**
   * Create a RGB Color object based on this HSLColor with a different
   * Tone. Changing the tone will return a lighter color. The percent
   * specified is a relative value.
   *
   * @param percent - the value between 0 - 100
   * @return the RGB Color object
   */
  public Color adjustTone(final float percent) {
    if (percent < 0
        || percent > 100) {
      throw new IllegalArgumentException("percent must be between 0 and 100");
    }

    final float multiplier = (100.0f
        + percent)
        / 100.0f;
    final float l = Math.min(100.0f, hsl[2]
        * multiplier);

    return toRGB(hsl[0], hsl[1], l, alpha);
  }

  /**
   * Get the Alpha value.
   *
   * @return the Alpha value.
   */
  public float getAlpha() {
    return alpha;
  }

  /**
   * Create a RGB Color object that is the complementary color of this
   * HSLColor. This is a convenience method. The complementary color is
   * determined by adding 180 degrees to the Hue value.
   * 
   * @return the RGB Color object
   */
  public Color getComplementary() {
    final float hue = (hsl[0]
        + 180.0f)
        % 360.0f;
    return toRGB(hue, hsl[1], hsl[2]);
  }

  /**
   * Get the Hue value.
   *
   * @return the Hue value.
   */
  public float getHue() {
    return hsl[0];
  }

  /**
   * Get the Luminance value.
   *
   * @return the Luminance value.
   */
  public float getLuminance() {
    return hsl[2];
  }

  /**
   * Get the RGB Color object represented by this HDLColor.
   *
   * @return the RGB Color object.
   */
  public Color getRGB() {
    return rgb;
  }

  /**
   * Get the Saturation value.
   *
   * @return the Saturation value.
   */
  public float getSaturation() {
    return hsl[1];
  }

  @Override
  public String toString() {
    String toString = "HSLColor[h="
        + hsl[0]
        + ",s="
        + hsl[1]
        + ",l="
        + hsl[2]
        + ",alpha="
        + alpha
        + "]";

    return toString;
  }

  /**
   * Convert a RGB Color to it corresponding HSL values.
   *
   * @param color the color to convert
   * @return an array containing the 3 HSL values.
   */
  private static float[] fromRGB(final Color color) {
    // Get RGB values in the range 0 - 1

    final float[] rgb = color.getRGBColorComponents(new float[3]);
    final float r = rgb[0];
    final float g = rgb[1];
    final float b = rgb[2];

    // Minimum and Maximum RGB values are used in the HSL calculations

    final float min = Math.min(r, Math.min(g, b));
    final float max = Math.max(r, Math.max(g, b));

    // Calculate the Hue

    float h = 0;

    if (max == min) {
      h = 0;
    } else if (max == r) {
      h = ((60
          * (g
              - b)
          / (max
              - min))
          + 360)
          % 360;
    } else if (max == g) {
      h = (60
          * (b
              - r)
          / (max
              - min))
          + 120;
    } else if (max == b) {
      h = (60
          * (r
              - g)
          / (max
              - min))
          + 240;
    }

    // Calculate the Luminance

    final float l = (max
        + min)
        / 2;

    // Calculate the Saturation

    final float s;

    if (max == min) {
      s = 0;
    } else if (l <= .5f) {
      s = (max
          - min)
          / (max
              + min);
    } else {
      s = (max
          - min)
          / (2
              - max
              - min);
    }

    return new float[] { h, s
        * 100, l
            * 100 };
  }

  /**
   * Convert HSL values to a RGB Color.
   * H (Hue) is specified as degrees in the range 0 - 360.
   * S (Saturation) is specified as a percentage in the range 1 - 100.
   * L (Lumanance) is specified as a percentage in the range 1 - 100.
   *
   * @param hsl an array containing the 3 HSL values
   * @param alpha the alpha value between 0 - 1
   * @returns the RGB Color object
   */
  private static Color toRGB(float[] hsl,
                             float alpha) {
    return toRGB(hsl[0], hsl[1], hsl[2], alpha);
  }

  /**
   * Convert HSL values to a RGB Color with a default alpha value of 1.
   *
   * @param h Hue is specified as degrees in the range 0 - 360.
   * @param s Saturation is specified as a percentage in the range 0 - 100.
   * @param l Lumanance is specified as a percentage in the range 0 - 100.
   * @return the RGB Color object
   */
  public static Color toRGB(float h,
                            float s,
                            float l) {
    return toRGB(h, s, l, 1.0f);
  }

  /**
   * Convert HSL values to a RGB Color.
   *
   * @param h Hue is specified as degrees in the range 0 - 360.
   * @param s Saturation is specified as a percentage in the range 0 - 100.
   * @param l Lumanance is specified as a percentage in the range 0 - 100.
   * @param alpha the alpha value between 0 - 1
   * @return the RGB Color object
   */
  public static Color toRGB(float h,
                            float s,
                            float l,
                            final float alpha) {
    if (h < 0
        || h > 360) {
      throw new IllegalArgumentException("hue must be between 0 and 360");
    }
    if (s < 0
        || s > 100) {
      throw new IllegalArgumentException("saturation must be between 0 and 100");
    }
    if (l < 0
        || l > 100) {
      throw new IllegalArgumentException("luminance must be between 0 and 100");
    }
    if (alpha < 0
        || alpha > 1) {
      throw new IllegalArgumentException("Alpha must be between 0 and 1");
    }

    // Formula needs all values between 0 - 1.

    h = h
        % 360.0f;
    h /= 360f;
    s /= 100f;
    l /= 100f;

    float q = 0;

    if (l < 0.5) {
      q = l
          * (1
              + s);
    } else {
      q = (l
          + s)
          - (s
              * l);
    }

    final float p = 2
        * l
        - q;

    final float r = Math.min(Math.max(0, hueToRGB(p, q, h
        + (1.0f
            / 3.0f))), 1f);
    final float g = Math.min(Math.max(0, hueToRGB(p, q, h)), 1f);
    final float b = Math.min(Math.max(0, hueToRGB(p, q, h
        - (1.0f
            / 3.0f))), 1f);

    return new Color(r, g, b, alpha);
  }

  private static float hueToRGB(float p,
                                float q,
                                float h) {
    if (h < 0)
      h += 1;

    if (h > 1)
      h -= 1;

    if (6
        * h < 1) {
      return p
          + ((q
              - p)
              * 6
              * h);
    }

    if (2
        * h < 1) {
      return q;
    }

    if (3
        * h < 2) {
      return p
          + ((q
              - p)
              * 6
              * ((2.0f
                  / 3.0f)
                  - h));
    }

    return p;
  }
}