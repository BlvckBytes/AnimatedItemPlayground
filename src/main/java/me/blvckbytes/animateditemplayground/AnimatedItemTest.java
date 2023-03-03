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

import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;
import me.blvckbytes.autowirer.ICleanable;
import me.blvckbytes.autowirer.IInitializable;
import me.blvckbytes.bbreflect.IReflectionHelper;
import me.blvckbytes.bbreflect.RClass;
import me.blvckbytes.bbreflect.handle.ClassHandle;
import me.blvckbytes.bbreflect.handle.FieldHandle;
import me.blvckbytes.bbreflect.handle.MethodHandle;
import me.blvckbytes.bbreflect.handle.predicate.Assignability;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimatedItemTest implements Listener, IInitializable, ICleanable {

  private static final long PERIOD = 1;
  private static final double STEP_SIZE = 0.03, EDGE_STOP = .12;
  private static final Color CENTER = new Color(253, 252, 0), AROUND = new Color(254, 72, 0);
  private static final GradientPoint BEGINNING = new GradientPoint(AROUND, 0.0), END = new GradientPoint(AROUND, 1.0);

  private final FieldHandle F_CRAFT_META_ITEM__NAME_BASE_COMPONENT, F_CRAFT_META_ITEM__NAME_STRING;
  private final MethodHandle M_CHAT_SERIALIZER__FROM_JSON;

  @AllArgsConstructor
  private static class AnimationState {
    List<GradientPoint> colors;
    boolean forwards;
  }

  private final Map<Player, AnimationState> animations;
  private final Plugin plugin;
  private final GradientGenerator gradientGenerator;

  private BukkitTask task;

  public AnimatedItemTest(Plugin plugin, IReflectionHelper reflectionHelper) throws Exception {
    this.plugin = plugin;
    this.gradientGenerator = new GradientGenerator();
    this.animations = new HashMap<>();

    ClassHandle C_CHAT_SERIALIZER  = reflectionHelper.getClass(RClass.CHAT_SERIALIZER);
    ClassHandle C_BASE_COMPONENT   = reflectionHelper.getClass(RClass.I_CHAT_BASE_COMPONENT);
    ClassHandle C_CRAFT_META_ITEM  = reflectionHelper.getClass(RClass.CRAFT_META_ITEM);

    F_CRAFT_META_ITEM__NAME_BASE_COMPONENT = C_CRAFT_META_ITEM.locateField().withType(C_BASE_COMPONENT).optional();
    F_CRAFT_META_ITEM__NAME_STRING         = C_CRAFT_META_ITEM.locateField().withType(String.class).optional();
    M_CHAT_SERIALIZER__FROM_JSON = C_CHAT_SERIALIZER.locateMethod().withParameters(JsonElement.class).withReturnType(C_BASE_COMPONENT, false, Assignability.TYPE_TO_TARGET).withStatic(true).required();
  }

  private void updateAnimation(Player p) {
    AnimationState state = animations.get(p);
    boolean existed = state != null;

    if (!existed) {
      List<GradientPoint> colors = new ArrayList<>();

      colors.add(BEGINNING);
      colors.add(new GradientPoint(CENTER, .5));
      colors.add(END);

      state = new AnimationState(colors, true);
    }

    playAnimationFrame(p, state);
    advanceAnimation(state);

    if (!existed)
      animations.put(p, state);
  }

  private void playAnimationFrame(Player p, AnimationState state) {
    try {
      String text = "FancyItem | " + p.getName();

      TextComponent comp = gradientGenerator.gradientize(text, state.colors);
      comp.toggleFormatting(TextFormatting.BOLD, true);

      JsonElement colorJson = comp.toJson(false);
      ItemStack itemInHand = p.getInventory().getItemInMainHand();

      if (itemInHand.getType().isAir()) {
        itemInHand = new ItemStack(Material.DIAMOND_SWORD);
        p.getInventory().setItemInMainHand(itemInHand);
      }

      ItemMeta meta = itemInHand.getItemMeta();

      if (meta == null)
        throw new IllegalStateException("Could not get the item's meta");

      if (F_CRAFT_META_ITEM__NAME_STRING != null)
        F_CRAFT_META_ITEM__NAME_STRING.set(meta, colorJson.toString());

      if (F_CRAFT_META_ITEM__NAME_BASE_COMPONENT != null)
        F_CRAFT_META_ITEM__NAME_BASE_COMPONENT.set(meta, M_CHAT_SERIALIZER__FROM_JSON.invoke(null, colorJson));

      itemInHand.setItemMeta(meta);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void advanceAnimation(AnimationState state) {
    if (state.colors.size() != 3)
      return;

    GradientPoint target = state.colors.get(1);

    if (state.forwards) {
      if (target.offset + EDGE_STOP >= 1)
        state.forwards = false;
      updateGradient(state);
    }

    else {
      if (target.offset - EDGE_STOP <= 0)
        state.forwards = true;
      updateGradient(state);
    }
  }

  private void updateGradient(AnimationState state) {
    GradientPoint center = state.colors.get(1);
    center.offset = center.offset + STEP_SIZE * (state.forwards ? 1 : -1);
  }

  @Override
  public void cleanup() {
    if (task != null)
      task.cancel();
  }

  @Override
  public void initialize() {
    task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
      for (Player p : Bukkit.getOnlinePlayers())
        updateAnimation(p);
    }, 0L, PERIOD);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    this.animations.remove(event.getPlayer());
  }
}
