package me.lignum.kristmarket;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.json.JSONObject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Calendar;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

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

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();

		JSONObject loc = new JSONObject();
		loc.put("world", location.getExtent().getUniqueId().toString());
		loc.put("x", location.getBlockX());
		loc.put("y", location.getBlockY());
		loc.put("z", location.getBlockZ());
		json.put("location", loc);

		JSONObject it = new JSONObject();
		it.put("name", item.getType().getName());
		it.put("count", item.getCount());
		json.put("item", it);

		json.put("initialBase", initialBase);
		json.put("demand", demand);
		json.put("isBuyShop", isBuyShop);
		json.put("halveningConstant", halveningConstant);

		return json;
	}

	public static SignShop fromJSON(JSONObject json) {
		JSONObject loc = json.optJSONObject("location");

		if (loc == null) {
			KristMarket$.MODULE$.get().logger().error("loc == null");
			return null;
		}

		String worldUUID = loc.optString("world", null);

		if (worldUUID == null) {
			KristMarket$.MODULE$.get().logger().error("worldUUID == null");
			return null;
		}

		int x = loc.optInt("x", 0);
		int y = loc.optInt("y", 0);
		int z = loc.optInt("z", 0);

		JSONObject it = json.optJSONObject("item");
		String name = it.optString("name", null);

		if (name == null) {
			return null;
		}

		int count = it.optInt("count", 1);

		Optional<ItemType> itemType = Sponge.getRegistry().getType(ItemType.class, name);

		if (!itemType.isPresent()) {
			return null;
		}

		ItemStackSnapshot snapshot = ItemStack.builder()
			.itemType(itemType.get())
			.quantity(count)
			.build()
			.createSnapshot();

		Optional<World> world = Sponge.getServer().getWorld(UUID.fromString(worldUUID));

		if (!world.isPresent()) {
			KristMarket$.MODULE$.get().logger().error("!world.isPresent()");
			return null;
		}

		SignShop shop = new SignShop();
		shop.location = new Location<World>(world.get(), x, y, z);
		shop.initialBase = json.optInt("initialBase", 0);
		shop.demand = json.optInt("demand", 0);
		shop.isBuyShop = json.optBoolean("isBuyShop", true);
		shop.halveningConstant = json.optInt("halveningConstant", 0);
		shop.item = snapshot;
		return shop;
	}
}
