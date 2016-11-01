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
    })
  }
}
