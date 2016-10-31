package me.lignum.kristmarket

import java.io.File
import java.nio.file.Path

import com.google.inject.Inject
import me.lignum.kristmarket.commands.CreateShop
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.plugin.Plugin

@Plugin(id = "kristmarket", name = "KristMarket", version = "1.0")
class KristMarket {
  var logger: Logger = _

  @Inject
  @ConfigDir(sharedRoot = true)
  var configDir: Path = _

  var dbFile: File = _
  var configFile: File = _

  var config: Configuration = _
  var database: Database = _

  KristMarket.instance = this

  @Inject
  def setLogger(l: Logger) = logger = l

  @Listener
  def onInit(event: GameInitializationEvent): Unit = {
    configFile = configDir.resolve("kristmarket.conf").toFile
    config = new Configuration(configFile)

    dbFile = configDir.resolve("kristmarket.db").toFile
    database = new Database(dbFile)

    Sponge.getCommandManager.register(this, CreateShop.spec, "createshop")
  }
}

object KristMarket {
  private var instance: KristMarket = _

  def get = instance
}