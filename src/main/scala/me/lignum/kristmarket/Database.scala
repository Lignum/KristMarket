package me.lignum.kristmarket

import java.io.File
import java.util.UUID

import com.google.common.reflect.TypeToken
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.api.world.{Location, World}

import scala.collection.mutable.ArrayBuffer

class Database(dbFile: File) {
  val loader = HoconConfigurationLoader.builder()
    .setFile(dbFile)
    .build()

  var rootNode: ConfigurationNode = _

  val signShops = new ArrayBuffer[SignShop]()

  def load(): Unit = {
    if (loader.canLoad) {
      rootNode = loader.load()

      rootNode.getNode("shops").getChildrenMap.forEach((k, v) => {
        if (rootNode.getNode("shops", String.valueOf(k), "location") != null) {
          signShops += v.getValue(TypeToken.of(classOf[SignShop]), new SignShop)
        }
      })
    } else {
      KristMarket.get.logger.error("Can't load KristMarket database!!")
    }
  }

  def save(): Unit = {
    if (loader.canSave) {
      rootNode.getNode("shops").getChildrenMap.keySet().forEach(key => {
        rootNode.getNode("shops").removeChild(key)
      })

      signShops.foreach(shop => {
        rootNode.getNode("shops", UUID.randomUUID()).setValue(TypeToken.of(classOf[SignShop]), shop)
      })

      loader.save(rootNode)
    } else {
      KristMarket.get.logger.error("Can't save KristMarket database!")
    }
  }

  def getSignShopAt(loc: Location[World]) = signShops.find(_.location.equals(loc))

  def addSignShop(shop: SignShop): Option[SignShop] =
    getSignShopAt(shop.location) match {
      case Some(s) => Some(s)
      case None =>
        signShops += shop
        None
    }

  load()
}
