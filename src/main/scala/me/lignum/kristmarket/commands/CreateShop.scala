package me.lignum.kristmarket.commands

import me.lignum.kristmarket.{KristMarket, ShopType, SignShop}
import org.spongepowered.api.block.BlockTypes
import org.spongepowered.api.block.tileentity.{Sign, TileEntityTypes}
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.GenericArguments._
import org.spongepowered.api.command.spec.{CommandExecutor, CommandSpec}
import org.spongepowered.api.command.{CommandResult, CommandSource}
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.text.Text
import org.spongepowered.api.text.format.TextColors
import org.spongepowered.api.util.blockray.BlockRay
import org.spongepowered.api.world.World

object CreateShop {
  val spec = CommandSpec.builder()
    .description(Text.of("Creates a sign shop."))
    .permission("kristmarket.command.createshop")
    .arguments(
      onlyOne(enumValue(Text.of("shopType"), classOf[ShopType]))
    )
    .executor(new CreateShop)
    .build()
}

class CreateShop extends CommandExecutor {
  override def execute(src: CommandSource, args: CommandContext): CommandResult = src match {
    case player: Player =>
      val shopTypeOpt = args.getOne[ShopType]("shopType")

      if (!shopTypeOpt.isPresent) {
        src.sendMessage(
          Text.builder("Invalid arguments!")
            .color(TextColors.RED)
            .build()
        )

        return CommandResult.success()
      }

      val shopType = shopTypeOpt.get

      val itemStackOpt = player.getItemInHand

      if (itemStackOpt.isPresent) {
        val itemStack = itemStackOpt.get
        val quantity = itemStack.getQuantity
        itemStack.setQuantity(1)

        KristMarket.get.database.getShopItem(itemStack) match {
          case Some(is) =>
          case None =>
            src.sendMessage(Text.of(TextColors.RED, "Please register this item with /setshopitem!"))
            return CommandResult.success()
        }

        val ray: BlockRay[World] = BlockRay.from(player)
          .filter(BlockRay.continueAfterFilter[World](BlockRay.onlyAirFilter(), 1))
          .build()

        val hitOpt = ray.end()

        if (hitOpt.isPresent) {
          val hit = hitOpt.get
          val location = hit.getLocation

          val block = player.getWorld.getBlock(hit.getBlockPosition)

          if (block.getType == BlockTypes.WALL_SIGN || block.getType == BlockTypes.STANDING_SIGN) {
            val tentOpt = player.getWorld.getTileEntity(hit.getBlockPosition)

            if (tentOpt.isPresent) {
              val tent = tentOpt.get

              if (tent.getType == TileEntityTypes.SIGN) {
                val sign = tent.asInstanceOf[Sign]
                val shop = new SignShop(location, itemStack, quantity, shopType == ShopType.BUY)

                KristMarket.get.database.addSignShop(shop) match {
                  case Some(s) =>
                    val action = if (s.isBuyShop) "selling" else "buying"
                    val itemName = s.item.getItem.getName

                    src.sendMessage(
                      Text.builder(
                        "There's already a sign shop " + action + " " + itemName + " here!"
                      )
                      .color(TextColors.RED)
                      .build()
                    )

                  case None =>
                    shop.setSignText(sign)
                    KristMarket.get.database.save()

                    src.sendMessage(
                      Text.builder("Successfully created your sign shop!")
                        .color(TextColors.GREEN)
                        .build()
                    )
                }
              } else {
                src.sendMessage(
                  Text.builder("This sign has no sign tile entity!")
                    .color(TextColors.RED)
                    .build()
                )
              }
            } else {
              src.sendMessage(
                Text.builder("This sign has no tile entity!")
                  .color(TextColors.RED)
                  .build()
              )
            }
          } else {
            src.sendMessage(
              Text.builder("You need to be looking at a sign to create a sign shop!")
                .color(TextColors.RED)
                .build()
            )
          }
        } else {
          src.sendMessage(
            Text.builder("You need to be looking at a sign to create a sign shop!")
              .color(TextColors.RED)
              .build()
          )
        }
      } else {
        src.sendMessage(
          Text.builder("You're not holding an item in your hand!")
            .color(TextColors.RED)
            .build()
        )
      }

      CommandResult.success()

    case _ =>
      src.sendMessage(
        Text.builder("This command can only be used by players!")
          .color(TextColors.RED)
          .build()
      )

      CommandResult.success()
  }
}
