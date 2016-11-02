package me.lignum.kristmarket;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
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
		WRONG_ITEM(Text.of(TextColors.RED, "You're holding the wrong item!")),
		ITEM_HAS_NO_PRICE(Text.of(TextColors.RED, "Item has no price!! Report this to an admin."));

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
	public ItemStack item;

	@Setting
	public int quantity;

	@Setting
	public boolean isBuyShop;

	public SignShop() {

	}

	public SignShop(Location<World> location, ItemStack item, int quantity, boolean isBuyShop) {
		this.location = location;
		this.item = item;
		this.isBuyShop = isBuyShop;
		this.quantity = quantity;
	}

	public int getPrice() {
		Optional<Integer> priceOpt = getDatabase().getItemPrice(item);

		if (!priceOpt.isPresent()) {
			return -1;
		} else {
			int price = priceOpt.get() * quantity;

			if (isBuyShop) {
				return Math.max(Math.max(1, quantity), price);
			} else {
				return Math.max(0, price / 2);
			}
		}
	}

	public void setSignText(Sign sign) {
		String heading = "[" + (isBuyShop ? "Buy" : "Sell") + "]";

		String itemName = item.getTranslation().get(Locale.UK);
		Optional<SignData> signDataOpt = sign.getOrCreate(SignData.class);

		signDataOpt.ifPresent(sd -> {
			sd.setElement(0, Text.builder(heading).color(TextColors.BLUE).build());
			sd.setElement(1, Text.of(quantity + "x"));
			sd.setElement(2, Text.builder(itemName).color(TextColors.DARK_GREEN).build());
			sd.setElement(3, Text.of("for " + getPrice() + " KST"));

			sign.offer(sd);
		});
	}

	private static EconomyService getEconomy() {
		return KristMarket$.MODULE$.get().economy();
	}

	private static Database getDatabase() {
		return KristMarket$.MODULE$.get().database();
	}

	public ActionResult buy(Player player) {
		int price = getPrice();

		if (price < 0) {
			return ActionResult.ITEM_HAS_NO_PRICE;
		}

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

		ItemStack stackToGive = item.copy();
		stackToGive.setQuantity(quantity);
		InventoryTransactionResult invResult = inv.offer(stackToGive);

		if (invResult.getType() != InventoryTransactionResult.Type.SUCCESS) {
			// Refund
			account.deposit(
				economy.getDefaultCurrency(), BigDecimal.valueOf(price), Cause.of(NamedCause.source(player))
			);

			return ActionResult.NO_INVENTORY_SPACE;
		}

		Optional<? extends ShopItem> shopItem = getDatabase().getShopItemOpt(item);
		shopItem.ifPresent(it -> it.demand_$eq(it.demand() + quantity));

		getDatabase().save();
		return ActionResult.SUCCESS;
	}

	public ActionResult sell(Player player) {
		int price = getPrice();

		if (price < 0) {
			return ActionResult.ITEM_HAS_NO_PRICE;
		}

		Optional<ItemStack> itemOpt = player.getItemInHand();

		if (!itemOpt.isPresent()) {
			return ActionResult.NO_ITEM_IN_HAND;
		}

		ItemStack stack = itemOpt.get();

		ItemStack compareStack = item.copy();
		compareStack.setQuantity(stack.getQuantity());

		if (!stack.equalTo(compareStack)) {
			return ActionResult.WRONG_ITEM;
		}

		if (stack.getQuantity() < quantity) {
			return ActionResult.NOT_ENOUGH_ITEMS;
		}

		EconomyService economy = getEconomy();
		Optional<UniqueAccount> accountOpt = economy.getOrCreateAccount(player.getUniqueId());

		if (!accountOpt.isPresent()) {
			return ActionResult.ACCOUNT_NOT_FOUND;
		}

		Account account = accountOpt.get();
		TransactionResult result = account.deposit(
			economy.getDefaultCurrency(), BigDecimal.valueOf(getPrice()), Cause.of(NamedCause.source(player))
		);

		ResultType resultType = result.getResult();

		if (resultType != ResultType.SUCCESS) {
			return ActionResult.TRANSACTION_FAILED;
		}

		int newQuantity = stack.getQuantity() - quantity;
		stack.setQuantity(newQuantity);
		player.setItemInHand(stack.getQuantity() <= 0 ? null : stack);

		Optional<? extends ShopItem> shopItem = getDatabase().getShopItemOpt(item);
		shopItem.ifPresent(it -> it.demand_$eq(it.demand() - quantity));

		getDatabase().save();
		return ActionResult.SUCCESS;
	}
}
