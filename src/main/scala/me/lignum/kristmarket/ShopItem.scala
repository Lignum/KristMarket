package me.lignum.kristmarket

import java.util.Calendar

import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable
import org.spongepowered.api.item.inventory.ItemStack

@ConfigSerializable class ShopItem {
  @Setting var itemType: ItemStack = _
  @Setting var initialBase: Int = 0
  @Setting var demand: Int = 0
  @Setting var halveningConstant: Int = 0

  var price: Int = 0

  def this(`type`: ItemStack, initialBase: Int, demand: Int, halveningConstant: Int) {
    this()
    this.itemType = `type`
    this.initialBase = initialBase
    this.demand = demand
    this.halveningConstant = halveningConstant

    updatePrice()
  }

  def updatePrice() {
    val calendar = Calendar.getInstance
    val minutes = calendar.get(Calendar.MINUTE)
    val time = minutes / 60.0

    price = Math.floor(initialBase.toDouble * Math.pow(2.0, demand / halveningConstant.toDouble)).toInt
  }

  updatePrice()
}