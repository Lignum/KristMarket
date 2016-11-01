package me.lignum.kristmarket

import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable
import org.spongepowered.api.item.ItemType
import java.util.Calendar

@ConfigSerializable class ShopItem {
  @Setting var itemType: ItemType = null
  @Setting var initialBase: Int = 0
  @Setting var demand: Int = 0
  @Setting var halveningConstant: Int = 0
  var price: Int = 0

  def this(`type`: ItemType, initialBase: Int, demand: Int, halveningConstant: Int) {
    this()
    this.itemType = `type`
    this.initialBase = initialBase
    this.demand = demand
    this.halveningConstant = halveningConstant
  }

  def updatePrice() {
    val calendar: Calendar = Calendar.getInstance
    val minutes: Int = calendar.get(Calendar.MINUTE)
    val time: Double = minutes / 60.0

    price = Math.floor((initialBase + 10.0 * Math.sin(2.0 * Math.PI * time)) * Math.pow(2.0, demand / halveningConstant.toDouble)).toInt
  }
}