package me.lignum.kristmarket.events

import me.lignum.kristmarket.KristMarket
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.block.ChangeBlockEvent

class BlockListener {
  @Listener
  def onBlockBreak(event: ChangeBlockEvent.Break): Unit = {
    event.getTransactions.forEach(tx => {
      val loc = tx.getOriginal.getLocation

      if (loc.isPresent) {
        val l = loc.get

        val db = KristMarket.get.database
        db.signShops
          .filter(_.location.compare(l.getBlockX, l.getBlockY, l.getBlockZ))
          .foreach(x => db.signShops -= x)
      }
    })
  }
}
