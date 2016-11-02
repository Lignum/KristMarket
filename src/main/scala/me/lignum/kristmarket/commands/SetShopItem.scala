package me.lignum.kristmarket.commands

import me.lignum.kristmarket.{KristMarket, ShopItem}
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.GenericArguments._
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

object SetShopItem {
  val spec = CommandSpec.builder()
    .description(Text.of("Registers/updates item information for KristMarket shops."))
    .permission("kristmarket.command.setshopitem")
    .arguments(
      onlyOne(integer(Text.of("base"))),
      onlyOne(integer(Text.of("demand"))),
      onlyOne(integer(Text.of("halveningConstant")))
    )
    .executor(new SetShopItem)
    .build()
}

class SetShopItem extends CommandExecutor {
  override def execute(src: CommandSource, args: CommandContext): CommandResult = src match {
    case player: Player =>
      val baseOpt = args.getOne[Int]("base")
      val demandOpt = args.getOne[Int]("demand")
      val halveningConstantOpt = args.getOne[Int]("halveningConstant")

      if (!baseOpt.isPresent || !demandOpt.isPresent || !halveningConstantOpt.isPresent) {
        return CommandResult.success()
      }

      val base = baseOpt.get
      val demand = demandOpt.get
      val halveningConstant = halveningConstantOpt.get

      val handItemOpt = player.getItemInHand

      if (handItemOpt.isPresent) {
        val handItem = handItemOpt.get
        handItem.setQuantity(1)

        KristMarket.get.database.getShopItem(handItem) match {
          case Some(shopItem) =>
            shopItem.initialBase = base
            shopItem.demand = demand
            shopItem.halveningConstant = halveningConstant

            KristMarket.get.database.save()
            src.sendMessage(Text.of(TextColors.GREEN, "Successfully updated the item's shop entry!"))

          case None =>
            val shopItem = new ShopItem(handItem, base, demand, halveningConstant)
            KristMarket.get.database.registerShopItem(shopItem)

            KristMarket.get.database.save()
            src.sendMessage(Text.of(TextColors.GREEN, "Successfully registered a new shop item!"))
        }
      } else {
        src.sendMessage(Text.of(TextColors.RED, "You're not holding the item to register in your hand!"))
      }

      CommandResult.success()

    case _ =>
      src.sendMessage(Text.of(TextColors.RED, "This command can only be used by players!"))
      CommandResult.success()
  }
}
