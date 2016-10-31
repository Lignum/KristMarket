package me.lignum.kristmarket.commands

import me.lignum.kristmarket.{KristMarket, Position, ShopType, SignShop}
import org.spongepowered.api.block.BlockTypes
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
      onlyOne(integer(Text.of("initialBase"))),
      onlyOne(integer(Text.of("initialDemand"))),
      onlyOne(integer(Text.of("halveningConstant"))),
      onlyOne(enumValue(Text.of("shopType"), classOf[ShopType]))
    )
    .executor(new CreateShop)
    .build()
}

class CreateShop extends CommandExecutor {
  override def execute(src: CommandSource, args: CommandContext): CommandResult = src match {
    case player: Player =>
      val initialBaseOpt = args.getOne[Int]("initialBase")
      val initialDemandOpt = args.getOne[Int]("initialDemand")
      val halveningConstantOpt = args.getOne[Int]("halveningConstant")
      val shopTypeOpt = args.getOne[ShopType]("shopType")

      if (!initialBaseOpt.isPresent || !initialDemandOpt.isPresent ||
        !halveningConstantOpt.isPresent || !shopTypeOpt.isPresent) {
        src.sendMessage(
          Text.builder("Invalid arguments!")
            .color(TextColors.RED)
            .build()
        )

        return CommandResult.success()
      }

      val initialBase = initialBaseOpt.get
      val initialDemand = initialDemandOpt.get
      val halveningConstant = halveningConstantOpt.get
      val shopType = shopTypeOpt.get

      val itemStackOpt = player.getItemInHand

      if (itemStackOpt.isPresent) {
        val itemStack = itemStackOpt.get
        val ray: BlockRay[World] = BlockRay.from(player)
          .filter(BlockRay.continueAfterFilter[World](BlockRay.onlyAirFilter(), 1))
          .build()

        val hitOpt = ray.end()

        if (hitOpt.isPresent) {
          val hit = hitOpt.get
          val location = new Position(hit.getBlockX, hit.getBlockY, hit.getBlockZ)

          val block = player.getWorld.getBlock(hit.getBlockPosition)

          if (block.getType == BlockTypes.WALL_SIGN || block.getType == BlockTypes.STANDING_SIGN) {
            val shop = new SignShop(location, itemStack, initialBase, initialDemand, halveningConstant, shopType)
            KristMarket.get.database.addSignShop(shop) match {
              case Some(s) =>
                val action = if (s.shopType == ShopType.BUY) "buying" else "selling"
                val itemName = s.item.getItem.getName

                src.sendMessage(
                  Text.builder(
                    "There's already a sign shop " + action + " " + itemName + " here!"
                  )
                  .color(TextColors.RED)
                  .build()
                )

              case None =>
                KristMarket.get.database.save()

                src.sendMessage(
                  Text.builder("Successfully created your sign shop!")
                    .color(TextColors.GREEN)
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
