package me.lignum.kristmarket

import java.util.UUID
import java.util.concurrent.TimeUnit

import org.spongepowered.api.Sponge
import org.spongepowered.api.entity.Entity

object RateLimit {
  def addBuy(entity: Entity): Unit = addBuy(entity.getUniqueId)

  def addBuy(uuid: UUID) =
    KristMarket.get.recentBuys.getOrElseUpdate(uuid, (0, System.currentTimeMillis, false)) match {
      case (buys, lastTime, locked) =>
        KristMarket.get.recentBuys(uuid) = (buys + 1, System.currentTimeMillis, locked)
      case _ =>
    }

  def addSale(entity: Entity): Unit = addSale(entity.getUniqueId)

  def addSale(uuid: UUID) =
    KristMarket.get.recentSales.getOrElseUpdate(uuid, (0, System.currentTimeMillis, false)) match {
      case (sales, lastTime, locked) =>
        KristMarket.get.recentSales(uuid) = (sales + 1, System.currentTimeMillis, locked)
      case _ =>
    }

  def shouldAllowBuy(entity: Entity): Boolean = shouldAllowBuy(entity.getUniqueId)

  def shouldAllowBuy(uuid: UUID): Boolean = {
    if (KristMarket.get.config.buyLimit == null) {
      return true
    }

    KristMarket.get.recentBuys.get(uuid) match {
      case Some((buys, lastTime, locked)) =>
        if (locked) {
          return false
        }

        val buyLimit = KristMarket.get.config.buyLimit.amount
        val timeLimit = KristMarket.get.config.buyLimit.time * 1000

        val allow = !(buys > buyLimit && System.currentTimeMillis - lastTime < timeLimit)

        if (!allow) {
          KristMarket.get.recentBuys(uuid) = (buys, lastTime, true)
          
          Sponge.getScheduler.createTaskBuilder()
            .delay(3, TimeUnit.SECONDS)
            .execute(_ => KristMarket.get.recentBuys.remove(uuid))
            .submit(KristMarket.get)
        }

        allow

      case None => true
    }
  }

  def shouldAllowSales(entity: Entity): Boolean = shouldAllowSales(entity.getUniqueId)

  def shouldAllowSales(uuid: UUID): Boolean = {
    if (KristMarket.get.config.sellLimit == null) {
      return true
    }

    KristMarket.get.recentSales.get(uuid) match {
      case Some((sales, lastTime, locked)) =>
        if (locked) {
          return false
        }

        val sellLimit = KristMarket.get.config.sellLimit.amount
        val timeLimit = KristMarket.get.config.sellLimit.time * 1000

        val allow = !(sales > sellLimit && System.currentTimeMillis - lastTime < timeLimit)

        if (!allow) {
          KristMarket.get.recentSales(uuid) = (sales, lastTime, true)

          Sponge.getScheduler.createTaskBuilder()
            .delay(3, TimeUnit.SECONDS)
            .execute(_ => KristMarket.get.recentSales.remove(uuid))
            .submit(KristMarket.get)
        }

        allow

      case None => true
    }
  }
}

class RateLimit(var time: Double, var amount: Int)
