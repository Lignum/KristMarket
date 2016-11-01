package me.lignum.kristmarket;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.Human;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.item.inventory.type.GridInventory;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Locale;
import java.util.Optional;

@ConfigSerializable
public class SignShop {
	public enum ActionResult {
		SUCCESS(Text.of(TextColors.GREEN, "Success.")),
		NOT_ENOUGH_FUNDS(Text.of(TextColors.RED, "You don't have enough funds!")),
		ACCOUNT_NOT_FOUND(Text.of(TextColors.RED, "Your account was not found.")),
		TRANSACTION_FAILED(Text.of(TextColors.RED, "Transaction failed!")),
		NO_INVENTORY_SPACE(Text.of(TextColors.RED, "Your inventory is full!")),
		NO_ITEM_IN_HAND(Text.of(TextColors.RED, "You're not holding an item in your hand!")),
		NOT_ENOUGH_ITEMS(Text.of(TextColors.RED, "You're not holding enough of the required item!")),
		WRONG_ITEM(Text.of(TextColors.RED, "You're holding the wrong item!"));

		private final Text message;

		public Text getMessage() {
			return message;
		}

		ActionResult(Text message) {
			this.message = message;
		}
	}

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

		int newPrice = (int)Math.floor(
			(initialBase + 10.0 * Math.sin(2.0 * Math.PI * time)) *
			Math.pow(2.0, demand / (double)halveningConstant)
		);

		price = Math.max(isBuyShop ? 1 : 0, newPrice);
	}

	private static EconomyService getEconomy() {
		return KristMarket$.MODULE$.get().economy();
	}

	private static void saveDatabase() {
		KristMarket$.MODULE$.get().database().save();
	}

	public ActionResult buy(Player player) {
		EconomyService economy = getEconomy();
		Optional<UniqueAccount> accountOpt = economy.getOrCreateAccount(player.getUniqueId());

		if (!accountOpt.isPresent()) {
			return ActionResult.ACCOUNT_NOT_FOUND;
		}

		Account account = accountOpt.get();
		TransactionResult result = account.withdraw(
			economy.getDefaultCurrency(), BigDecimal.valueOf(price), Cause.of(NamedCause.source(player))
		);

		ResultType resultType = result.getResult();

		switch (resultType) {
			case SUCCESS:
				break;

			case ACCOUNT_NO_FUNDS:
				return ActionResult.NOT_ENOUGH_FUNDS;

			default:
				return ActionResult.TRANSACTION_FAILED;
		}

		Inventory inv = player.getInventory().query(Hotbar.class, GridInventory.class);
		InventoryTransactionResult invResult = inv.offer(item.createStack());

		if (invResult.getType() != InventoryTransactionResult.Type.SUCCESS) {
			// Refund
			account.deposit(
				economy.getDefaultCurrency(), BigDecimal.valueOf(price), Cause.of(NamedCause.source(player))
			);

			return ActionResult.NO_INVENTORY_SPACE;
		}

		demand++;

		saveDatabase();
		return ActionResult.SUCCESS;
	}

	public ActionResult sell(Player player) {
		Optional<ItemStack> itemOpt = player.getItemInHand();

		if (!itemOpt.isPresent()) {
			return ActionResult.NO_ITEM_IN_HAND;
		}

		ItemStack stack = itemOpt.get();

		if (stack.getItem() != item.getType()) {
			return ActionResult.WRONG_ITEM;
		}

		if (stack.getQuantity() < item.getCount()) {
			return ActionResult.NOT_ENOUGH_ITEMS;
		}

		EconomyService economy = getEconomy();
		Optional<UniqueAccount> accountOpt = economy.getOrCreateAccount(player.getUniqueId());

		if (!accountOpt.isPresent()) {
			return ActionResult.ACCOUNT_NOT_FOUND;
		}

		Account account = accountOpt.get();
		TransactionResult result = account.deposit(
			economy.getDefaultCurrency(), BigDecimal.valueOf(price), Cause.of(NamedCause.source(player))
		);

		ResultType resultType = result.getResult();

		if (resultType != ResultType.SUCCESS) {
			return ActionResult.TRANSACTION_FAILED;
		}

		int newQuantity = stack.getQuantity() - item.getCount();
		stack.setQuantity(newQuantity);
		player.setItemInHand(stack.getQuantity() <= 0 ? null : stack);

		demand--;
		saveDatabase();
		return ActionResult.SUCCESS;
	}
}
