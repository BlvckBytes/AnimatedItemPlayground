/*
 * MIT License
 *
 * Copyright (c) 2023 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.blvckbytes.animateditemplayground;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class GradientGenerator {

  /**
   * Create a new gradient text from a plain string
   * @param text Plain string to add a gradient to
   * @param colors Colors making up the gradient (have to be sorted by percentage ascending)
   * @return String with applied gradient as a component
   */
  public TextComponent gradientize(
    String text,
    List<GradientPoint> colors
  ) {
    TextComponent res = new TextComponent("");

    // Iterate all characters of the string
    char[] chars = text.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      // How far the loop is into the string
      double percentage = (i + 1D) / chars.length;

      // Create a new component containing only the current character
      TextComponent curr = new TextComponent(String.valueOf(chars[i]));

      // Apply the color at the current point within the gradient
      Color color = getGradientPoint(colors, percentage);
      curr.setColor(String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()));

      res.addSibling(curr);
    }

    return res;
  }

  /**
   * Get a color point on a linear gradient made up of multiple colors at certain points
   * @param colors Colors making up the gradient (have to be sorted by percentage ascending)
   * @param percentage Percentage to pick the color at
   * @return Picked color
   */
  public Color getGradientPoint(List<GradientPoint> colors, double percentage) {
    // No colors present, print all white
    if (colors.size() == 0)
      return Color.WHITE;

    // Only one color present
    if (colors.size() == 1)
      return colors.get(0).color;

    // Quick exit: If the first color has a higher value than 0,
    // the first n percent are that color statically.
    GradientPoint first = colors.get(0);
    if (percentage <= first.offset)
      return first.color;

    // Quick exit: If the last color has a lower value than 1,
    // the last (1 - n) percent are that color statically.
    GradientPoint last = colors.get(colors.size() - 1);
    if (percentage >= last.offset)
      return last.color;

    // Find the two nearest colors around the current percentage point which
    // will make up the smaller in-between-gradient the caller is interested in

    // Start out assuming that A will be the first and B the last color
    GradientPoint a = first, b = last;

    // Only iterate from 1 until n-1, as first and last are already active
    for (int i = 1; i < colors.size() - 1; i++) {
      GradientPoint color = colors.get(i);

      // Set A if the color is below the percentage but higher
      // up than the previous A color
      if (color.offset < percentage && color.offset > a.offset)
        a = color;

      // Set B if the color is above the percentage but lower
      // down than the previous B color
      // It is important to also allow an exact percentage match here, to not
      // make hitting colors impossible when at their exact percentage
      if (color.offset >= percentage && color.offset < b.offset)
        b = color;
    }

    // Relativize the percentage to that smaller gradient section
    // How far into the sub-gradient is that point, from 0 to 1,
    // which is the ratio from the length travelled on the whole gradient
    // to get from A to percentage, divided by the span of A and B.
    percentage = (percentage - a.offset) / (b.offset - a.offset);

    // Linearly interpolate
    double resultRed   = a.color.getRed()   + percentage * (b.color.getRed()   - a.color.getRed());
    double resultGreen = a.color.getGreen() + percentage * (b.color.getGreen() - a.color.getGreen());
    double resultBlue  = a.color.getBlue()  + percentage * (b.color.getBlue()  - a.color.getBlue());

    // Floor to the next nearest integer when converting back into a color
    return new Color(
      (int) Math.floor(resultRed),
      (int) Math.floor(resultGreen),
      (int) Math.floor(resultBlue)
    );
  }

  /**
   * Tries to parse a gradient notation into a usable gradient color point
   * list, sorted by the percentage value ascending
   * @param notation Color notation of format {@code <#RRGGBB:(0-1) * n>} (colors are
   *                 separated by spaces), example: {@code <#FF0000:0 #00FF00:.5 #0000FF:1>}
   * @return Parsed notation on success, empty if the notation was malformed
   */
  public Optional<List<GradientPoint>> parseGradientNotation(String notation) {
    // Not enclosed by angle brackets
    if (!(notation.startsWith("<") && notation.endsWith(">")))
      return Optional.empty();

    List<GradientPoint> res = new ArrayList<>();

    // Split on space to get individual color notations
    String[] colors = notation.substring(1, notation.length() - 1).split(" ");

    for (String color : colors) {
      // Split on colon to get the color data
      String[] data = color.split(":");

      // Malformed color notation
      if (data.length != 2)
        return Optional.empty();

      // Has to start with # to be a valid hex notation
      if (!data[0].startsWith("#"))
        return Optional.empty();

      char[] chars = data[0].toCharArray();

      // Only try to parse if there are enough characters available
      if (chars.length != 6 + 1)
        return Optional.empty();

      // Try to parse the color's R G and B separately
      Color c;
      double percentage;
      try {
        c = new Color(
          Integer.parseInt(chars[1] + "" + chars[2], 16),
          Integer.parseInt(chars[3] + "" + chars[4], 16),
          Integer.parseInt(chars[5] + "" + chars[6], 16)
        );

        percentage = Double.parseDouble(data[1]);
      }

      // Unparsable color or percentage
      catch (Exception ignored) {
        return Optional.empty();
      }

      // Percentage out of range
      if (percentage < 0 || percentage > 1)
        return Optional.empty();

      // Add color point to the list
      res.add(new GradientPoint(c, percentage));
    }

    // Don't accept empty lists
    if (res.size() == 0)
      return Optional.empty();

    // Return the list of colors sorted by their percentage
    res.sort(Comparator.comparingDouble(a -> a.offset));
    return Optional.of(res);
  }
}
