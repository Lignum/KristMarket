package me.lignum.kristmarket

import java.io.File
import java.util.{Optional, UUID}

import com.google.common.reflect.TypeToken
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.world.{Location, World}

import scala.collection.mutable.ArrayBuffer

class Database(dbFile: File) {
  val loader = HoconConfigurationLoader.builder()
    .setFile(dbFile)
    .build()

  var rootNode: ConfigurationNode = _

  val signShops = new ArrayBuffer[SignShop]()
  val shopItems = new ArrayBuffer[ShopItem]()

  def load(): Unit = {
    if (loader.canLoad) {
      rootNode = loader.load()

      rootNode.getNode("shops").getChildrenMap.forEach((k, v) => {
        if (rootNode.getNode("shops", String.valueOf(k), "location") != null) {
          signShops += v.getValue(TypeToken.of(classOf[SignShop]), new SignShop)
        }
      })

      rootNode.getNode("items").getChildrenMap.forEach((k, v) => {
        if (rootNode.getNode("items", String.valueOf(k), "type") != null) {
          shopItems += v.getValue(TypeToken.of(classOf[ShopItem]), new ShopItem)
        }
      })

      rootNode.getNode()
    } else {
      KristMarket.get.logger.error("Can't load KristMarket database!!")
    }
  }

  private def removeChildren(node: String): Unit =
    rootNode.getNode(node).getChildrenMap.keySet().forEach(key => {
      rootNode.getNode(node).removeChild(key)
    })

  def save(): Unit = {
    if (loader.canSave) {
      removeChildren("shops")

      signShops.foreach(shop => {
        rootNode.getNode("shops", UUID.randomUUID()).setValue(TypeToken.of(classOf[SignShop]), shop)
      })

      removeChildren("items")

      shopItems.foreach(item => {
        rootNode.getNode("items", UUID.randomUUID()).setValue(TypeToken.of(classOf[ShopItem]), item)
      })

      loader.save(rootNode)
    } else {
      KristMarket.get.logger.error("Can't save KristMarket database!")
    }
  }

  def getItemPrice(itemType: ItemStack): Optional[Integer] = getShopItem(itemType) match {
    case Some(it) => Optional.of(it.price)
    case None => Optional.empty()
  }

  def getShopItem(itemType: ItemStack) = shopItems.find(_.itemType.equalTo(itemType))

  def getShopItemOpt(itemType: ItemStack) = getShopItem(itemType) match {
    case Some(it) => Optional.of(it)
    case None => Optional.empty()
  }

  def registerShopItem(item: ShopItem): Option[ShopItem] = getShopItem(item.itemType) match {
    case Some(i) => Some(i)
    case None =>
      shopItems += item
      None
  }

  def getSignShopAt(loc: Location[World]) = signShops.find(_.location.equals(loc))

  def addSignShop(shop: SignShop): Option[SignShop] = getSignShopAt(shop.location) match {
    case Some(s) => Some(s)
    case None =>
      signShops += shop
      None
  }

  load()
}
