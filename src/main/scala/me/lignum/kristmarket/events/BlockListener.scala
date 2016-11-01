package me.lignum.kristmarket.events

import java.util.Locale

import me.lignum.kristmarket.KristMarket
import me.lignum.kristmarket.SignShop.ActionResult
import org.spongepowered.api.block.BlockTypes
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.block.ChangeBlockEvent
import org.spongepowered.api.event.block.InteractBlockEvent
import org.spongepowered.api.event.filter.cause.First
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

class BlockListener {
  @Listener
  def onBlockBreak(event: ChangeBlockEvent.Break): Unit = {
    event.getTransactions.forEach(tx => {
      val blockType = tx.getOriginal.getState.getType

      if (blockType == BlockTypes.WALL_SIGN || blockType == BlockTypes.STANDING_SIGN) {
        val loc = tx.getOriginal.getLocation

        if (loc.isPresent) {
          val l = loc.get

          val db = KristMarket.get.database
          var dbChanged = false

          db.signShops
            .filter(_.location.equals(l))
            .foreach(x => {
              db.signShops -= x
              dbChanged = true
            })

          if (dbChanged) {
            db.save()
          }
        }
      }
    })
  }

  @Listener
  def onBlockInteract(event: InteractBlockEvent.Secondary, @First player: Player): Unit = {
    val block = event.getTargetBlock.getState

    if (block.getType == BlockTypes.WALL_SIGN || block.getType == BlockTypes.STANDING_SIGN) {
      val loc = event.getTargetBlock.getLocation

      if (loc.isPresent) {
        val l = loc.get()

        val db = KristMarket.get.database

        db.getSignShopAt(l) match {
          case Some(shop) =>
            val result = if (shop.isBuyShop) {
              shop.buy(player)
            } else {
              shop.sell(player)
            }

            result match {
              case ActionResult.SUCCESS =>
                val verb = if (shop.isBuyShop) "bought" else "sold"
                val itemName = shop.item.getType.getTranslation.get(Locale.UK)

                player.sendMessage(
                  Text.of(
                    TextColors.GREEN, "Successfully ", verb, " ", String.valueOf(shop.item.getCount), " of ",
                    TextColors.YELLOW, itemName, TextColors.GREEN, " for ", String.valueOf(shop.price), " KST."
                  )
                )

              case _ =>
                player.sendMessage(result.getMessage)
            }
          case None =>
        }
      }
    }
  }
}
