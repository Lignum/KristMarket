package me.lignum.kristmarket

import java.io.File

import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.hocon.HoconConfigurationLoader

class Configuration(configFile: File) {
  private val loader = HoconConfigurationLoader.builder()
    .setFile(configFile)
    .build()

  private var rootNode: ConfigurationNode = _

  var updateInterval: Int = _

  var buyLimit: RateLimit = _
  var sellLimit: RateLimit = _

  private def load(): Unit = {
    if (loader.canLoad) {
      rootNode = loader.load()
    } else {
      KristMarket.get.logger.error("Can't load config file!")
    }

    if (rootNode.getNode("updateInterval").isVirtual) {
      // The interval at which prices will be updated in seconds.
      rootNode.getNode("updateInterval").setValue(10)

      rootNode.getNode("rateLimit", "buy", "enabled").setValue(true)
      rootNode.getNode("rateLimit", "buy", "amount").setValue(5)
      rootNode.getNode("rateLimit", "buy", "time").setValue(1.0)

      rootNode.getNode("rateLimit", "sell", "enabled").setValue(true)
      rootNode.getNode("rateLimit", "sell", "amount").setValue(5)
      rootNode.getNode("rateLimit", "sell", "time").setValue(1.0)

      if (loader.canSave) {
        loader.save(rootNode)
      } else {
        KristMarket.get.logger.error("Can't save config file!")
      }
    }

    updateInterval = rootNode.getNode("updateInterval").getInt(10)

    if (rootNode.getNode("rateLimit", "buy", "enabled").getBoolean(false)) {
      buyLimit = new RateLimit(
        rootNode.getNode("rateLimit", "buy", "time").getDouble(1.0),
        rootNode.getNode("rateLimit", "buy", "amount").getInt(5)
      )
    }

    if (rootNode.getNode("rateLimit", "sell", "enabled").getBoolean(false)) {
      sellLimit = new RateLimit(
        rootNode.getNode("rateLimit", "sell", "time").getDouble(1.0),
        rootNode.getNode("rateLimit", "sell", "amount").getInt(5)
      )
    }
  }

  load()
}
