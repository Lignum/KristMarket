package me.lignum.kristmarket

import java.io.{File, PrintWriter}
import java.util.Scanner

import org.json.{JSONArray, JSONObject}
import org.spongepowered.api.world.{Location, World}

import scala.collection.mutable.ArrayBuffer

class Database(dbFile: File) {
  if (!dbFile.exists()) {
    dbFile.createNewFile()

    val json = new JSONObject
    json.put("shops", new JSONArray)

    val pw = new PrintWriter(dbFile)
    pw.println(json.toString(4))
    pw.close()
  }

  val signShops = new ArrayBuffer[SignShop]()

  def load(): Unit = {
    val scanner = new Scanner(dbFile)
    var contents = ""

    while (scanner.hasNextLine) {
      contents += scanner.nextLine()
    }

    scanner.close()

    val json = new JSONObject(contents)
    val shopsArray = json.optJSONArray("shops")

    shopsArray match {
      case shops: JSONArray =>
        for (i <- 0 until shops.length) {
          val shop = shops.optJSONObject(i)

          if (shop != null) {
            signShops += SignShop.fromJSON(shop)
          }
        }

      case _ =>
    }
  }

  def save(): Unit = {
    val json = new JSONObject
    val shopsArray = new JSONArray

    signShops.foreach(shop => shopsArray.put(shop.toJSON))

    json.put("shops", shopsArray)

    val pw = new PrintWriter(dbFile)
    pw.println(json.toString(4))
    pw.close()
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
