package me.lignum.kristmarket

import java.util.Calendar

import ninja.leaping.configurate.ConfigurationNode
import org.spongepowered.api.Sponge
import org.spongepowered.api.data.translator.ConfigurateTranslator
import org.spongepowered.api.item.inventory.ItemStack

object SignShop {
  def apply(node: ConfigurationNode): Option[SignShop] = {
    val location = new Position(
      node.getNode("location", "x").getInt(0),
      node.getNode("location", "y").getInt(0),
      node.getNode("location", "z").getInt(0)
    )

    val halveningConstant = node.getNode("halveningConstant").getInt(0)
    val shopType = ShopType.valueOf(node.getNode("shopType").getString("BUY")) match {
      case x: ShopType => x
      case _ => ShopType.BUY
    }

    val initialBase = node.getNode("initialBase").getInt(0)
    val demand = node.getNode("demand").getInt(0)

    val translator = ConfigurateTranslator.instance()
    val itemData = translator.translateFrom(node.getNode("itemStack"))

    val builder = Sponge.getDataManager.getBuilder(classOf[ItemStack])

    if (builder.isPresent) {
      val item = builder.get.build(itemData)

      if (item.isPresent) {
        Some(new SignShop(location, item.get, initialBase, demand, halveningConstant, shopType))
      } else {
        None
      }
    } else {
      None
    }
  }
}

class SignShop(
  val location: Position, val item: ItemStack, val initialBase: Int,
  val demand: Int, val halveningConstant: Int, val shopType: ShopType) {

  def calculatePrice = {
    val calendar = Calendar.getInstance()
    val minutes = calendar.get(Calendar.MINUTE)
    val time = minutes / 60.0

    Math.floor((initialBase + 10 * Math.sin(2.0 * Math.PI * time)) * Math.pow(2.0, demand / halveningConstant.toDouble)).toInt
  }

  def writeToConfigNode(node: ConfigurationNode) = {
    node.getNode("location", "x").setValue(location.x)
    node.getNode("location", "y").setValue(location.y)
    node.getNode("location", "z").setValue(location.z)

    val translator = ConfigurateTranslator.instance()
    node.getNode("itemStack").setValue(translator.translateData(item.toContainer))

    node.getNode("halveningConstant").setValue(halveningConstant)
    node.getNode("shopType").setValue(shopType.toString)
    node.getNode("initialBase").setValue(initialBase)
    node.getNode("demand").setValue(demand)
  }
}
