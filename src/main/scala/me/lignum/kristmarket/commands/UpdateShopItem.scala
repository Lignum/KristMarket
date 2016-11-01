package me.lignum.kristmarket.commands

import me.lignum.kristmarket.KristMarket
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.GenericArguments._
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors

object UpdateShopItem {
  val spec = CommandSpec.builder()
    .description(Text.of("Similar to /setshopitem. Allows updating of individual properties."))
    .permission("kristmarket.command.updateshopitem")
    .arguments(
      onlyOne(enumValue(Text.of("property"), classOf[UpdateShopProperty])),
      onlyOne(integer(Text.of("value")))
    )
    .executor(new UpdateShopItem)
    .build()
}

class UpdateShopItem extends CommandExecutor {
  override def execute(src: CommandSource, args: CommandContext): CommandResult = src match {
    case player: Player =>
      val propertyOpt = args.getOne[UpdateShopProperty]("property")
      val valueOpt = args.getOne[Int]("value")

      if (!propertyOpt.isPresent || !valueOpt.isPresent) {
        return CommandResult.success()
      }

      val value = valueOpt.get

      val handItemOpt = player.getItemInHand

      if (handItemOpt.isPresent) {
        val handItem = handItemOpt.get

        KristMarket.get.database.getShopItem(handItem.getItem) match {
          case Some(shopItem) =>
            propertyOpt.get match {
              case UpdateShopProperty.BASE                => shopItem.initialBase = value
              case UpdateShopProperty.DEMAND              => shopItem.demand = value
              case UpdateShopProperty.HALVENING_CONSTANT  => shopItem.halveningConstant = value
            }

            KristMarket.get.database.save()
            src.sendMessage(Text.of(TextColors.GREEN, "Update successful."))

          case None =>
            src.sendMessage(Text.of(TextColors.RED, "This item is not registered, use /setshopitem first."))
        }
      } else {
        src.sendMessage(Text.of(TextColors.RED, "You are not holding the item to update in your hand!"))
      }

      CommandResult.success()

    case _ =>
      src.sendMessage(Text.of(TextColors.RED, "This command can only be used by players!"))
      CommandResult.success()
  }
}
