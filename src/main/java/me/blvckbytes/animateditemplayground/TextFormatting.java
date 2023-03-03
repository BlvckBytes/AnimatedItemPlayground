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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum TextFormatting {

  BOLD('l'),
  ITALIC('o'),
  UNDERLINED('n'),
  STRIKETHROUGH('m'),
  OBFUSCATED('k'),
  ;

  private final char marker;

  public static final TextFormatting[] values = values();

  // Mapping marking characters to enum constants for quick access
  private static final Map<Character, TextFormatting> lut;

  static {
    // Initialize the lookup table on all available values
    lut = new HashMap<>();
    for (TextFormatting fmt : values)
      lut.put(fmt.getMarker(), fmt);
  }

  /**
   * Find a text formatting constant by it's representitive marking character
   * @param c Marking character to search for
   * @return Text formatting constant or null if the char is unknown
   */
  public static @Nullable TextFormatting getByChar(char c) {
    return lut.get(c);
  }
}
