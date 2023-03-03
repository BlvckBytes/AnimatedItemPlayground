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

import me.blvckbytes.autowirer.AutoWirer;
import me.blvckbytes.bbreflect.CommandRegisterer;
import me.blvckbytes.bbreflect.IReflectionHelper;
import me.blvckbytes.bbreflect.ReflectionHelperFactory;
import me.blvckbytes.bbreflect.packets.PacketInterceptorRegistry;
import me.blvckbytes.bbreflect.packets.communicator.FakeSlotCommunicator;
import me.blvckbytes.bbreflect.packets.communicator.ItemNameCommunicator;
import me.blvckbytes.bbreflect.packets.communicator.WindowOpenCommunicator;
import me.blvckbytes.bukkitboilerplate.ConsoleSenderLogger;
import me.blvckbytes.bukkitboilerplate.ELogLevel;
import me.blvckbytes.bukkitboilerplate.ILogger;
import me.blvckbytes.bukkitboilerplate.PluginFileHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class AnimatedItemPlayground extends JavaPlugin {

  private AutoWirer wirer;
  private ILogger logger;

  @Override
  public void onEnable() {
    long beginStamp = System.nanoTime();

    logger = new ConsoleSenderLogger(this);

    wirer = new AutoWirer()
      .addExistingSingleton(this)
      .addExistingSingleton(logger)
      .addSingleton(IReflectionHelper.class, dependencies -> {
        IReflectionHelper helper = new ReflectionHelperFactory(this).makeHelper();
        logger.log(ELogLevel.INFO, "Detected server version " + helper.getVersion());
        return helper;
      }, IReflectionHelper::cleanupInterception)
      .addSingleton(CommandRegisterer.class)
      .addSingleton(PluginFileHandler.class)
      .addSingleton(AnimatedItemTest.class)
      .addSingleton(FakeSlotCommunicator.class)
      .addSingleton(PacketInterceptorRegistry.class)
      .addSingleton(ItemNameCommunicator.class)
      .addSingleton(WindowOpenCommunicator.class)
      .addInstantiationListener(Listener.class, (listener, dependencies) -> {
        Bukkit.getPluginManager().registerEvents(listener, this);
      })
      .addInstantiationListener(Command.class, (command, dependencies) -> {
        ((CommandRegisterer) dependencies[0]).register(command);
      }, CommandRegisterer.class)
      .onException(e -> {
        this.logger.log(ELogLevel.ERROR, "An error occurred while setting up the plugin:");
        this.logger.logError(e);
        Bukkit.getServer().getPluginManager().disablePlugin(this);
      })
      .wire(wirer -> {
        this.logger.log(ELogLevel.INFO, "Successfully loaded " + wirer.getInstancesCount() + " classes (" + ((System.nanoTime() - beginStamp) / 1000 / 1000) + "ms)");
      });
  }

  @Override
  public void onDisable() {
    try {
      if (wirer != null)
        wirer.cleanup();
    } catch (Exception e) {
      this.logger.logError(e);
    }
  }
}
