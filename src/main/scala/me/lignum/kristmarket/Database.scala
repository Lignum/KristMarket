package me.lignum.kristmarket

import java.io.File

import scala.collection.mutable.ArrayBuffer

class Database(dbFile: File) {
  if (!dbFile.exists()) {
    try {
      dbFile.createNewFile()
    } catch {
      case e: Throwable => KristMarket.get.logger.error("Failed to create database!", e)
    }
  }

  val signShops = new ArrayBuffer[SignShop]()

  def save(): Unit = {

  }

  def getSignShopAt(x: Int = 0, y: Int = 0, z: Int = 0) = signShops.find(_.location.compare(x, y, z))

  def addSignShop(shop: SignShop): Option[SignShop] =
    getSignShopAt(shop.location.x, shop.location.y, shop.location.z) match {
      case Some(s) => Some(s)
      case None =>
        signShops += shop
        None
    }
}
