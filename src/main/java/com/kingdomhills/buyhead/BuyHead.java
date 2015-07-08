package com.kingdomhills.buyhead;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BuyHead extends JavaPlugin {
	
	String permission = "buyhead.buy";

	Economy econ;
	
	double price;
	String msgbuy;
	String msgnotexists;
	String msgnomoney;
	String msghelp;
	String msgnoperm;
	String msginvfull;
	String msgmojangerr;
	String customname;
	String customlore;
	
	@Override
	public void onEnable() {
		if (!setupEconomy()) {
			getLogger().info("Vault not found. Disabling.");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		saveDefaultConfig();
		price = getConfig().getDouble("price");
		msgbuy = getConfig().getString("msg-buy");
		msgnotexists = getConfig().getString("msg-notexists");
		msgnomoney = getConfig().getString("msg-nomoney");
		msghelp = getConfig().getString("msg-help");
		msgnoperm = getConfig().getString("msg-noperm");
		msginvfull = getConfig().getString("msg-invfull");
		msgmojangerr = getConfig().getString("msg-mojangerr");
		customname = getConfig().getString("custom-name");
		customlore = getConfig().getString("custom-lore");
	}
	
	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String cmdalias, String[] args) {
		if (!(sender instanceof Player)) sender.sendMessage("This command must be runned as a player");
		else {
			final Player player = (Player) sender;
			if (player.hasPermission(permission)) {
				if (args.length > 0) {
					final String skullowner = args[0];
					int tempamount = 1;
					if (args.length > 1) {
						int tryamount = Integer.parseInt(args[1]);
						if (tryamount > 0) tempamount = tryamount;
					}
					final int amount = tempamount;
					final double totprice = amount * price;
					if (!econ.has(player, totprice)) sendMsgPlaceholder(player, msgnomoney.replace("%unitprice%", econ.currencyNamePlural() + price));
					else if (player.getInventory().firstEmpty()==-1) sendMsgPlaceholder(player, msginvfull);
					else {
						new Thread(new Runnable() {public void run() {
							String realname = getRealPlayernameFromNameMojang(skullowner);
							if (realname == null) {
								sendMsgPlaceholder(player, msgnotexists.replace("%skullowner%", skullowner));
							}
							else if (realname.equals("%IOException%")) {
								sendMsgPlaceholder(player, msgmojangerr);
							}
							else {
								econ.withdrawPlayer(player, totprice);
								sendMsgPlaceholder(player, msgbuy.replace("%amount%", String.valueOf(amount)).replace("%skullowner%", realname).replace("%price%", econ.currencyNamePlural() + price));
	
								DateFormat dd = new SimpleDateFormat("dd/MM/yyyy");
								String reportDate = dd.format(Calendar.getInstance().getTime());
								DateFormat dh = new SimpleDateFormat("HH:mm:ss");
								String reportHour = dh.format(Calendar.getInstance().getTime());
								
								String name = customname.replace("%owner%", realname);
								String lore = customlore.replace("%date%", reportDate).replace("%time%", reportHour).replace("%buyer%", player.getName());
								
								player.getInventory().addItem(getHead(realname, amount, name, lore));
							}
						}}).start();
					}
				}
				else sendMsgPlaceholder(player, msghelp.replace("%cmd%", "/" + cmdalias).replace("%unitprice%", econ.currencyNamePlural() + price));
			}
			else sendMsgPlaceholder(player, msgnoperm.replace("%permnode%", permission));
		}
		return true;
	}
	
	private void sendMsgPlaceholder(Player player, String msg) {
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
	}
	
	public String getRealPlayernameFromNameMojang(String playername) {
		StringBuffer httpresponse = new StringBuffer();
		String response = "null";
		try {
			URL url;
			url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playername);
			HttpURLConnection http = (HttpURLConnection) url.openConnection();
			http.setDoOutput(true);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) httpresponse.append(inputLine);
			in.close();
			response = httpresponse.toString();

			if (Array.getLength(response.toString().split("\"")) > 7) response = httpresponse.toString().split("\"")[7];
			else response = null;
		}
		catch (IOException e) {
			response = "%IOException%";
		}
		return response;
	}
	
	static public ItemStack getHead(String playername, int amount, String customname, String lore) {
		ItemStack item;
		item = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
		item.setAmount(amount);
		SkullMeta meta = (SkullMeta) item.getItemMeta();
		if (customname != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', customname));
		if (lore != null) {
			List<String> lorelist = new ArrayList<String>();
			lorelist.add(ChatColor.translateAlternateColorCodes('&', lore));
			meta.setLore(lorelist);
		}
		meta.setOwner(playername);
		item.setItemMeta(meta);
		return item;
	}
}