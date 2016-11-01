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

  private def load(): Unit = {
    if (loader.canLoad) {
      rootNode = loader.load()
    } else {
      KristMarket.get.logger.error("Can't load config file!")
    }

    if (rootNode.getNode("updateInterval").isVirtual) {
      // The interval at which prices will be updated in seconds.
      rootNode.getNode("updateInterval").setValue(10)

      if (loader.canSave) {
        loader.save(rootNode)
      } else {
        KristMarket.get.logger.error("Can't save config file!")
      }
    }

    updateInterval = rootNode.getNode("updateInterval").getInt(10)
  }

  load()
}
