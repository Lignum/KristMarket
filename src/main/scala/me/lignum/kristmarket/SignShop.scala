package me.lignum.kristmarket

import java.util.{Calendar, Locale}

import ninja.leaping.configurate.ConfigurationNode
import org.spongepowered.api.Sponge
import org.spongepowered.api.block.tileentity.Sign
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData
import org.spongepowered.api.data.translator.ConfigurateTranslator
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors
import org.spongepowered.api.world.World

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

    val worldOpt = Sponge.getServer.getWorld(node.getNode("world").getString(""))

    if (worldOpt.isPresent) {
      val world = worldOpt.get
      val builder = Sponge.getDataManager.getBuilder(classOf[ItemStack])

      if (builder.isPresent) {
        val item = builder.get.build(itemData)

        if (item.isPresent) {
          return Some(new SignShop(world, location, item.get, initialBase, demand, halveningConstant, shopType))
        }
      }
    }

    None
  }
}

class SignShop(
  val world: World,
  val location: Position, val item: ItemStack, val initialBase: Int,
  val demand: Int, val halveningConstant: Int, val shopType: ShopType
) {
  var price = 0

  updatePrice()

  def setSignText(sign: Sign): Unit = {
    val heading = "[" + (shopType match {
      case ShopType.BUY => "Buy"
      case ShopType.SELL => "Sell"
      case _ => "Fuck idk"
    }) + "]"

    val itemName = item.getTranslation.get(Locale.UK)

    val signDataOpt = sign.getOrCreate(classOf[SignData])

    if (signDataOpt.isPresent) {
      val sd = signDataOpt.get()

      sd.setElement(0, Text.builder(heading).color(TextColors.BLUE).build())
      sd.setElement(1, Text.of(item.getQuantity + "x"))
      sd.setElement(2, Text.builder(itemName).color(TextColors.DARK_GREEN).build())
      sd.setElement(3, Text.of("for " + price + " KST"))

      sign.offer(sd)
    }
  }

  def updatePrice() = {
    val calendar = Calendar.getInstance()
    val minutes = calendar.get(Calendar.MINUTE)
    val time = minutes / 60.0

    price =
      Math.floor((initialBase + 10 * Math.sin(2.0 * Math.PI * time)) * Math.pow(2.0, demand / halveningConstant.toDouble)).toInt
  }

  def writeToConfigNode(node: ConfigurationNode) = {
    node.getNode("location", "x").setValue(location.x)
    node.getNode("location", "y").setValue(location.y)
    node.getNode("location", "z").setValue(location.z)

    val translator = ConfigurateTranslator.instance()
    node.getNode("itemStack").setValue(translator.translateData(item.toContainer))

    node.getNode("world").setValue(world.getUniqueId.toString)

    node.getNode("halveningConstant").setValue(halveningConstant)
    node.getNode("shopType").setValue(shopType.toString)
    node.getNode("initialBase").setValue(initialBase)
    node.getNode("demand").setValue(demand)
  }
}
