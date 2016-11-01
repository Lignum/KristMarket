package me.lignum.kristmarket

import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit

import com.google.inject.Inject
import me.lignum.kristmarket.commands.{CreateShop, SetShopItem, UpdateShopItem}
import me.lignum.kristmarket.events.BlockListener
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.block.BlockTypes
import org.spongepowered.api.block.tileentity.{Sign, TileEntityTypes}
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.state.{GamePostInitializationEvent, GameStartedServerEvent, GameStoppingServerEvent}
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.service.economy.EconomyService

import scala.collection.mutable.ArrayBuffer

@Plugin(id = "kristmarket", name = "KristMarket", version = "1.0")
class KristMarket {
  var logger: Logger = _

  @Inject
  @ConfigDir(sharedRoot = true)
  var configDir: Path = _

  var dbFile: File = _
  var configFile: File = _

  var config: Configuration = _
  var database: Database = _

  var economy: EconomyService = _

  KristMarket.instance = this

  @Inject
  def setLogger(l: Logger) = logger = l

  def startPriceUpdateSchedule(): Unit = {
    Sponge.getScheduler.createTaskBuilder
      .interval(config.updateInterval, TimeUnit.SECONDS)
      .execute(_ => {
        database.shopItems.foreach(item => item.updatePrice())

        var dbHasChanged = false
        val shopsToRemove = new ArrayBuffer[SignShop]

        database.signShops.foreach(shop => {
          val tentOpt = shop.location.getTileEntity

          if (tentOpt.isPresent) {
            val tent = tentOpt.get
            val block = tent.getBlock

            if (block.getType == BlockTypes.WALL_SIGN || block.getType == BlockTypes.STANDING_SIGN) {
              if (tent.getType == TileEntityTypes.SIGN) {
                val sign = tent.asInstanceOf[Sign]
                shop.setSignText(sign)
              } else {
                shopsToRemove += shop
              }
            } else {
              shopsToRemove += shop
            }
          } else {
            shopsToRemove += shop
          }
        })

        shopsToRemove.foreach(x => {
          database.signShops -= x
          dbHasChanged = true
        })

        if (dbHasChanged) {
          database.save()
        }
      })
      .submit(this)
  }

  @Listener
  def onPostInit(even: GamePostInitializationEvent): Unit = {
    val economyOpt = Sponge.getServiceManager.provide(classOf[EconomyService])

    if (!economyOpt.isPresent) {
      KristMarket.get.logger.error("Couldn't find an economy service!! Please install an economy plugin.")
    } else {
      economy = economyOpt.get
    }
  }

  @Listener
  def onServerStart(event: GameStartedServerEvent): Unit = {
    configFile = configDir.resolve("kristmarket.conf").toFile
    config = new Configuration(configFile)

    dbFile = configDir.resolve("kristmarket.db").toFile
    database = new Database(dbFile)

    Sponge.getCommandManager.register(this, CreateShop.spec, "createshop")
    Sponge.getCommandManager.register(this, SetShopItem.spec, "setshopitem")
    Sponge.getCommandManager.register(this, UpdateShopItem.spec, "updateshopitem")

    Sponge.getEventManager.registerListeners(this, new BlockListener)
    startPriceUpdateSchedule()
  }

  @Listener
  def onServerStop(event: GameStoppingServerEvent): Unit = {
    database.save()
  }
}

object KristMarket {
  private var instance: KristMarket = _

  def get = instance
}