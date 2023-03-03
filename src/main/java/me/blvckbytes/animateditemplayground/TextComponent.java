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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

@Getter
public class TextComponent implements IComponent {

  private static final Map<ChatColor, Color> vanillaColors;

  static {
    vanillaColors = generateVanillaColors();
  }

  private final @Nullable String text;
  private final boolean[] formatting;
  private final List<IComponent> siblings;

  // Click event
  private @Nullable ClickAction clickAction;
  private @Nullable String clickValue;

  // Hover event
  private @Nullable HoverAction hoverAction;
  private @Nullable IComponent hoverValue;

  // Custom color as well as it's chat-color approximation (only for hex values)
  private @Nullable String color, approximatedColor;

  /**
   * Create a new text component from plain text without any pre-processing
   * @param text Component's text value
   */
  public TextComponent(@Nullable String text) {
    this(text, null);
  }

  /**
   * Create a new text component from plain text without any pre-processing
   * but with a color property value
   * @param text Component's text value
   * @param color Color of the component, optional
   */
  private TextComponent(@Nullable String text, @Nullable String color) {
    this.formatting = new boolean[TextFormatting.values.length];
    this.siblings = new ArrayList<>();
    this.text = text;
    this.color = color;
    this.approximatedColor = translateColor(color);
  }

  /**
   * Create a new text component from plain text without any pre-processing
   * but with a color property value as well as formatting flags
   * @param text Component's text value
   * @param color Color of the component, optional
   * @param formatting Formatting modes
   */
  private TextComponent(@Nullable String text, @Nullable String color, boolean[] formatting) {
    this(text, color);

    // Copy into the local array to avoid mutability
    System.arraycopy(formatting, 0, this.formatting, 0, Math.min(this.formatting.length, formatting.length));
  }

  /////////////////////////////// Miscellanoeus ///////////////////////////////

  /**
   * Toggle a formatting for this component and all of it's children
   * @param formatting Formatting to toggle
   * @param state New state
   */
  public void toggleFormatting(TextFormatting formatting, boolean state) {
    this.formatting[formatting.ordinal()] = state;
  }

  /**
   * Add another sibling component which will inherit it's parent's properties
   * @param component Component to add
   */
  public void addSibling(IComponent component) {
    this.siblings.add(component);
  }

  /**
   * Set a new color value
   * @param color Color to set
   */
  public void setColor(String color) {
    this.color = color;
    this.approximatedColor = translateColor(color);
  }

  ///////////////////////////////// Clicking //////////////////////////////////

  /**
   * Set what happens when the message is being clicked within the chat
   * @param action Action to be executed
   * @param value Action value
   */
  public void setClick(ClickAction action, String value) {
    this.clickAction = action;
    this.clickValue = value;
  }

  /**
   * Clear the hover event
   */
  public void clearClick() {
    this.clickAction = null;
    this.clickValue = null;
  }

  ///////////////////////////////// Hovering //////////////////////////////////

  /**
   * Set what happens when the message is being hovered within the chat
   * @param action Action to be executed
   * @param value Action value
   */
  public TextComponent setHover(HoverAction action, String value) {
    this.hoverAction = action;

    // Create a new plain text component
    TextComponent comp = new TextComponent(value);
    this.hoverValue = comp;

    return comp;
  }

  /**
   * Set what happens when the message is being hovered within the chat
   * @param action Action to be executed
   * @param value Action value
   */
  public void setHover(HoverAction action, TextComponent value) {
    this.hoverAction = action;
    this.hoverValue = value;
  }

  /**
   * Clear the hover event
   */
  public void clearHover() {
    this.hoverAction = null;
    this.hoverValue = null;
  }

  ///////////////////////////////// Generation /////////////////////////////////

  @Override
  public JsonObject toJson(boolean approximateColors) {
    JsonObject res = new JsonObject();

    // Set text
    res.addProperty("text", this.text == null ? "" : this.text);

    // Apply HEX color
    if (!approximateColors && this.color != null)
      res.addProperty("color", this.color);

    // Apply approximated color
    if (approximateColors && this.approximatedColor != null)
      res.addProperty("color", this.approximatedColor);

    // Apply hovering
    if (this.hoverAction != null && this.hoverValue != null) {
      JsonObject action = new JsonObject();
      action.addProperty("action", this.hoverAction.name().toLowerCase());
      action.add("value", this.hoverValue.toJson(approximateColors));
      res.add("hoverEvent", action);
    }

    // Apply clicking
    if (this.clickAction != null && this.clickValue != null) {
      JsonObject action = new JsonObject();
      action.addProperty("action", this.clickAction.name().toLowerCase());
      action.addProperty("value", this.clickValue);
      res.add("clickEvent", action);
    }

    // Apply formatting flags
    for (int i = 0; i < formatting.length; i++) {
      // Disabled formatting, don't append
      if (!formatting[i])
        continue;

      TextFormatting fmt = TextFormatting.values[i];
      res.addProperty(fmt.name().toLowerCase(), true);
    }

    // Append all siblings
    if (siblings.size() > 0) {
      JsonArray extra = new JsonArray();
      siblings.forEach(s -> extra.add(s.toJson(approximateColors)));
      res.add("extra", extra);
    }

    return res;
  }

  @Override
  public String toPlainText() {
    StringBuilder sb = new StringBuilder();

    // Append only approximated colors, if available
    if (this.approximatedColor != null)
      sb.append(this.approximatedColor);

    // Append all active text formattings, one after the other
    for (int i = 0; i < this.formatting.length; i++) {
      if (this.formatting[i])
        sb.append("§").append(TextFormatting.values[i].getMarker());
    }

    // Append the text itself, if available
    if (this.text != null)
      sb.append(this.text);

    // Append the contents of all siblings
    for (IComponent sibling : siblings)
      sb.append(sibling.toPlainText());

    return sb.toString();
  }

  @Override
  public String toString() {
    return toJson(false).toString();
  }

  /////////////////////////////////// Parsing //////////////////////////////////

  /**
   * State content wrapper utility used while parsing
   */
  @AllArgsConstructor
  private static class ChildContentState {
    StringBuilder value;
    String color;
    List<GradientPoint> gradient;
    boolean[] fmts;

    private static ChildContentState makeDefault() {
      return new ChildContentState(
        new StringBuilder(), null, null,
        new boolean[TextFormatting.values.length]
      );
    }
  }

  /**
   * Parses a new TextComponent from a string of text by creating new
   * sub-components to express hex-color notations as required. Vanilla
   * notation will be kept in one component as much as possible.
   * @param text Text to parse
   * @param gradientGenerator Gradient generator ref for generating gradients from gradient notation, optional
   * @return Parsed component
   */
  public static TextComponent parseFromText(String text, @Nullable GradientGenerator gradientGenerator) {

    // Quick exit: Does not contain any hex colors
    if (!(text.contains("§#") || text.contains("§<")))
      return new TextComponent(text);

    // Head component, when splitting, a new sibling is appended to it
    TextComponent head = new TextComponent(null);
    ChildContentState state = ChildContentState.makeDefault();

    // Iterate text char by char
    char[] chars = text.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];

      // Not a special notation (or the last char), keep on collecting
      if (c != '§' || i == chars.length - 1) {
        state.value.append(c);
        continue;
      }

      int charsLeft = chars.length - 1 - i;
      char n = chars[i + 1];

      // HEX color notation, supporting only #RRGGBB
      // 6 + 1 as the next char (n) is the #
      if (n == '#' && charsLeft >= (6 + 1)) {
        // Hex-characters: [0-9A-Fa-f]

        StringBuilder hex = new StringBuilder("#");

        // Try to match up to 6 and skip early if a char is invalid
        for (int j = 0; j < 6; j++) {
          char jc = chars[i + 2 + j];

          // Cannot possibly be a valid notation
          if (!(
            (jc >= '0' && jc <= '9') || // Numbers
            (jc >= 'A' && jc <= 'F') || // Uppercase A-F
            (jc >= 'a' && jc <= 'f')    // Lowerface a-f
          )) {
            hex = null;
            break;
          }

          hex.append(jc);
        }

        // Not a valid hex-color, leave § unappended and continue
        if (hex == null)
          continue;

        pushAndReset(state, head, gradientGenerator, true);
        state.color = hex.toString();

        // Skip #RRGGBB
        i += 7;
        continue;
      }

      // Could be a gradient notation
      if (n == '<' && gradientGenerator != null) {
        // Find the next closing bracket
        int closeInd = text.indexOf('>', i + 1);

        // Bracket available
        if (closeInd > 0) {
          // Grab the possible notation, including brackets, and try to parse it
          List<GradientPoint> gradient = gradientGenerator.parseGradientNotation(
            text.substring(i + 1, closeInd + 1)
          ).orElse(null);

          // Was a gradient notation, push, store gradient and jump ahead
          if (gradient != null) {
            pushAndReset(state, head, gradientGenerator, true);
            state.gradient = gradient;
            i = closeInd;
            continue;
          }
        }
      }

      TextFormatting fmt = TextFormatting.getByChar(n);

      // Check if it's a text formatting sequence while caching a color
      if (fmt != null && (state.color != null || state.gradient != null)) {

        // Push with current formatting and leave color in buffer
        pushAndReset(state, head, gradientGenerator, false);

        // Update formattings
        state.fmts[fmt.ordinal()] = true;

        // Skip this formatting character within the message
        i++;
        continue;
      }

      if (
        // Vanilla color change occurred
        ((n >= '0' && n <= '9') || (n >= 'a' && n <= 'f') || n == 'r') &&
        // And there is a color stashed to be applied on sb's content
        (state.color != null || state.gradient != null)
      )
        pushAndReset(state, head, gradientGenerator, true);

      // Leave special sequence as is
      state.value.append(c);
    }

    // Add remainder
    if (state.value.length() > 0)
      pushAndReset(state, head, gradientGenerator, false);

    return head;
  }

  /**
   * Push a new sibling component to the head using the current value and properties and
   * then reset the value builder as well as the state's properties, if requested
   * @param state Child content state wrapper
   * @param head Head component ref
   * @param gradientGenerator Gradient generator ref for generating gradients from gradient notation, optional
   * @param resetProperties Whether to reset all properties as well
   */
  private static void pushAndReset(
    ChildContentState state,
    TextComponent head,
    @Nullable GradientGenerator gradientGenerator,
    boolean resetProperties
  ) {

    // Don't push empty components
    if (state.value.length() > 0) {
      // Add a gradient component if a gradient is available
      if (state.gradient != null && gradientGenerator != null) {
        TextComponent gradientComp = gradientGenerator.gradientize(state.value.toString(), state.gradient);

        // Also apply formatting flags
        System.arraycopy(state.fmts, 0, gradientComp.formatting, 0, state.fmts.length);

        head.siblings.add(gradientComp);
      }

      // Add a colored component
      else
        head.siblings.add(new TextComponent(state.value.toString(), state.color, state.fmts));

      state.value.setLength(0);
    }

    // Reset the child content state
    if (resetProperties) {
      Arrays.fill(state.fmts, false);
      state.color = null;
      state.gradient = null;
    }
  }

  ///////////////////////////// Color Approximation ////////////////////////////

  /**
   * Generates a map which corresponds vanilla chat colors
   * to the RGB version the client renders (very close)
   * <a href="https://htmlcolorcodes.com/minecraft-color-codes/">Source</a>
   */
  private static Map<ChatColor, Color> generateVanillaColors() {
    Map<ChatColor, Color> res = new LinkedHashMap<>();

    res.put(ChatColor.BLACK, new Color(0x00, 0x00, 0x00));
    res.put(ChatColor.DARK_BLUE, new Color(0x00, 0x00, 0xAA));
    res.put(ChatColor.DARK_GREEN, new Color(0x00, 0xAA, 0x00));
    res.put(ChatColor.DARK_AQUA, new Color(0x00, 0xAA, 0xAA));
    res.put(ChatColor.DARK_RED, new Color(0xAA, 0x00, 0x00));
    res.put(ChatColor.DARK_PURPLE, new Color(0xAA, 0x00, 0xAA));
    res.put(ChatColor.GOLD, new Color(0xFF, 0xAA, 0x00));
    res.put(ChatColor.GRAY, new Color(0xAA, 0xAA, 0xAA));
    res.put(ChatColor.DARK_GRAY, new Color(0x55, 0x55, 0x55));
    res.put(ChatColor.BLUE, new Color(0x55, 0x55, 0xFF));
    res.put(ChatColor.GREEN, new Color(0x55, 0xFF, 0x55));
    res.put(ChatColor.AQUA, new Color(0x55, 0xFF, 0xFF));
    res.put(ChatColor.RED, new Color(0xFF, 0x55, 0x55));
    res.put(ChatColor.LIGHT_PURPLE, new Color(0xFF, 0x55, 0xFF));
    res.put(ChatColor.YELLOW, new Color(0xFF, 0xFF, 0x55));
    res.put(ChatColor.WHITE, new Color(0xFF, 0xFF, 0xFF));

    return res;
  }

  /**
   * Calculate the absolute (always positive) difference between two colors
   * @param a Color A
   * @param b Color B
   * @return Positive difference
   */
  private static int absColorDifference(Color a, Color b) {
    return (
      Math.abs(a.getRed() - b.getRed()) +
      Math.abs(a.getGreen() - b.getGreen()) +
      Math.abs(a.getBlue() - b.getBlue())
    );
  }

  /**
   * Find the closest chat color match to any given color
   * @param color Target color
   * @return Closest chat color match
   */
  private static ChatColor findClosestMatch(Color color) {
    Map.Entry<ChatColor, Color> closest = null;
    int closestDiff = Integer.MAX_VALUE;

    // Find the color with the smallest delta
    for (Map.Entry<ChatColor, Color> e : vanillaColors.entrySet()) {
      // Initially set to the first value
      if (closest == null) {
        closest = e;
        continue;
      }

      // Update if the diff is smaller than before
      int currDiff = absColorDifference(color, e.getValue());
      if (currDiff < closestDiff) {
        closest = e;
        closestDiff = currDiff;
      }
    }

    // Will never be null, as there are always values hard-coded
    assert closest != null;
    return closest.getKey();
  }

  /**
   * Parses a color from it's hex representation
   * @param input Color to parse
   * @return Parsed color or null if unparsable
   */
  private static @Nullable Color parseColor(String input) {
    if (!input.startsWith("#"))
      return null;

    try {
      return new Color(
        Integer.parseInt(input.substring(1, 3), 16),
        Integer.parseInt(input.substring(3, 5), 16),
        Integer.parseInt(input.substring(5, 7), 16)
      );
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Translate any given color if it's a hex color and approximation mode is enabled
   * @param color Color to translate
   * @return Translated color, if applicable
   */
  private static @Nullable String translateColor(@Nullable String color) {
    // Pass through null values
    if (color == null)
      return null;

    // Not a hex value, cannot translate anything
    Color hex = parseColor(color);
    if (hex == null)
      return color;

    // Respond with the closest matching ChatColor's name
    return findClosestMatch(hex).name().toLowerCase();
  }
}
