package me.lignum.kristmarket;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.Human;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.item.inventory.type.GridInventory;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Calendar;
import java.util.Locale;
import java.util.Optional;

@ConfigSerializable
public class SignShop {
	@Setting
	public Location<World> location;

	@Setting
	public ItemStackSnapshot item;

	@Setting
	public int initialBase;

	@Setting
	public int demand;

	@Setting
	public int halveningConstant;

	@Setting
	public boolean isBuyShop;

	public int price;

	public SignShop() {

	}

	public SignShop(Location<World> location, ItemStackSnapshot item, int initialBase, int demand, int halveningConstant, boolean isBuyShop) {
		this.location = location;
		this.item = item;
		this.initialBase = initialBase;
		this.demand = demand;
		this.halveningConstant = halveningConstant;
		this.isBuyShop = isBuyShop;
	}

	public void setSignText(Sign sign) {
		String heading = "[" + (isBuyShop ? "Buy" : "Sell") + "]";

		String itemName = item.getType().getTranslation().get(Locale.UK);
		Optional<SignData> signDataOpt = sign.getOrCreate(SignData.class);

		signDataOpt.ifPresent(sd -> {
			sd.setElement(0, Text.builder(heading).color(TextColors.BLUE).build());
			sd.setElement(1, Text.of(item.getCount() + "x"));
			sd.setElement(2, Text.builder(itemName).color(TextColors.DARK_GREEN).build());
			sd.setElement(3, Text.of("for " + price + " KST"));

			sign.offer(sd);
		});
	}

	public void updatePrice() {
		Calendar calendar = Calendar.getInstance();
		int minutes = calendar.get(Calendar.MINUTE);
		double time = minutes / 60.0;

		price = (int)Math.floor(
					(initialBase + 10.0 * Math.sin(2.0 * Math.PI * time)) *
					Math.pow(2.0, demand / (double)halveningConstant)
				);
	}

	public boolean buy(Player player) {
		Inventory inv = player.getInventory().query(Hotbar.class, GridInventory.class);
		InventoryTransactionResult result = inv.offer(item.createStack());
		return result.getType() == InventoryTransactionResult.Type.SUCCESS;
	}


}
