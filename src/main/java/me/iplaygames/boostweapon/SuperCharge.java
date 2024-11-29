package me.iplaygames.boostweapon;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;

public class SuperCharge implements Listener, CommandExecutor {

    private final JavaPlugin plugin;

    public SuperCharge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.isOp()) {
                // Create a new ItemStack for the custom Nether Star
                ItemStack supercharge = new ItemStack(Material.NETHER_STAR);
                ItemMeta meta = supercharge.getItemMeta();
                meta.setDisplayName(ChatColor.GOLD + "Supercharge");
                meta.setLore(Collections.singletonList(ChatColor.AQUA + "Use this to gain special abilities!"));
                supercharge.setItemMeta(meta);
                // Give the player the custom Nether Star
                player.getInventory().addItem(supercharge);
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "You must be an operator to use this command!");
                return false;
            }
        } else {
            sender.sendMessage("You must be a player to use this command.");
            return false;
        }
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.NETHER_STAR) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.GOLD + "Supercharge") &&
                    meta.hasLore() && meta.getLore().contains(ChatColor.AQUA + "Use this to gain special abilities!")) {
                // Apply the glowing effect for 2 minutes
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 2 * 60 * 20, 1));

                // Apply Slowness 4 effect for 2 minutes
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 2 * 60 * 20, 5));

                // Apply Strength 3 effect for 2 minutes
                player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 2 * 60 * 20, 2));

                // Send a bold purple message in chat
                Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + player.getName() + " has supercharged!");

                // Remove one custom Nether Star from the player's inventory
                ItemStack toRemove = new ItemStack(Material.NETHER_STAR);
                ItemMeta toRemoveMeta = toRemove.getItemMeta();
                toRemoveMeta.setDisplayName(ChatColor.GOLD + "Supercharge");
                toRemoveMeta.setLore(Arrays.asList(ChatColor.AQUA + "Use this to gain special abilities!"));
                toRemove.setItemMeta(toRemoveMeta);
                toRemove.setAmount(1);

                player.getInventory().removeItem(toRemove);
            }
        }
    }
}
