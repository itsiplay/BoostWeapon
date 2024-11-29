package me.iplaygames.boostweapon;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.boss.BarColor;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.boss.BossBar;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.*;

public final class SonicWeapon extends JavaPlugin implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long cooldown = 10000; // Cooldown time in milliseconds
    private final Trail trail = new Trail(Particle.REDSTONE);
    private HashMap<UUID, Long> lastUseMap = new HashMap<>(); // Add this line

    private final Map<UUID, Long> harpoonCooldowns = new HashMap<>();
    private final int COOLDOWN_TIME = 5000; // Cooldown time in milliseconds (e.g., 5000ms = 5 seconds)


    @Override
    public void onEnable() {
        // Register your command here
        this.getCommand("sonicblast").setExecutor(new SonicBlastCommandExecutor(this));
        getServer().getPluginManager().registerEvents(new AnvilInteractionHandler(), this);
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        // Add a task to update the trail every tick
        new BukkitRunnable() {
            @Override
            public void run() {
                trail.tick();
            }
        }.runTaskTimer(this, 0, 1);

        // Initialize SuperCharge with the plugin instance
        SuperCharge superCharge = new SuperCharge(this); // Pass 'this' here
        getCommand("supercharge").setExecutor(superCharge);
        getServer().getPluginManager().registerEvents(superCharge, this);
        registerCraftingRecipe(); // For supercharge
        registerBroadswordCraftingRecipe();
        registerSonicBowCraftingRecipe();
        registerHarpoonLauncherCraftingRecipe();
        // Register the new command executor
        this.getCommand("boostcredits").setExecutor(new BoostCreditsCommandExecutor());
        this.getCommand("givebroadsword").setExecutor(this);
        this.getCommand("giveharpoongun").setExecutor(this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        PlayerInventory inventory = event.getPlayer().getInventory();
        fixBroadswordsInInventory(inventory);
    }

    private void fixBroadswordsInInventory(PlayerInventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (isBroadsword(meta)) {
                    removeEnchantments(item);
                }
            }
        }
    }

    private boolean isBroadsword(ItemMeta meta) {
        return meta.hasDisplayName() &&
                meta.getDisplayName().equals(ChatColor.DARK_PURPLE + "Broadsword of Power") &&
                meta.getLore() != null &&
                meta.getLore().contains(ChatColor.GRAY + "Wield the power of the warrior.");
    }

    private void removeEnchantments(ItemStack item) {
        item.getEnchantments().keySet().forEach(item::removeEnchantment);
    }

    private class AnvilInteractionHandler implements Listener {

        @EventHandler
        public void onPrepareAnvil(PrepareAnvilEvent event) {
            ItemStack resultItem = event.getResult();
            ItemStack firstItem = event.getInventory().getItem(0);

            if (firstItem != null && firstItem.hasItemMeta()) {
                ItemMeta meta = firstItem.getItemMeta();

                if (isSonicBow(meta) || isBroadsword(meta) || isHarpoonLauncher(meta)) {
                    event.setResult(null);
                    event.getInventory().setRepairCost(0);
                }
            }
        }

        private boolean isSonicBow(ItemMeta meta) {
            return meta.hasDisplayName() &&
                    meta.getDisplayName().equals(ChatColor.AQUA + "Sonic Bow") &&
                    meta.getLore() != null &&
                    meta.getLore().contains(ChatColor.GRAY + "Unleash the power of sound with this mystical bow!");
        }

        private boolean isBroadsword(ItemMeta meta) {
            return meta.hasDisplayName() &&
                    meta.getDisplayName().equals(ChatColor.DARK_PURPLE + "Broadsword of Power") &&
                    meta.getLore() != null &&
                    meta.getLore().contains(ChatColor.GRAY + "Wield the power of the warrior.");
        }

        private boolean isHarpoonLauncher(ItemMeta meta) {
            return meta.hasDisplayName() &&
                    meta.getDisplayName().equals(ChatColor.AQUA + "Harpoon Launcher") &&
                    meta.getLore() != null &&
                    meta.getLore().contains(ChatColor.GRAY + "Pull yourself towards your enemies!");
        }
    }

    // Command to give the player a grappling hook
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("givebroadsword")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                // Check if the player is an operator
                if (player.isOp()) {
                    ItemStack broadsword = createBroadsword();
                    player.getInventory().addItem(broadsword);
                    player.sendMessage(ChatColor.GREEN + "You have been given the Broadsword of Power!");
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return false;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
                return false;
            }
        }

        if (command.getName().equalsIgnoreCase("giveharpoongun")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                // Check if the player is an operator
                if (player.isOp()) {
                    ItemStack harpoonLauncher = createHarpoonLauncher();
                    player.getInventory().addItem(harpoonLauncher);
                    player.sendMessage(ChatColor.GREEN + "You have been given the Harpoon Launcher!");
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return false;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
                return false;
            }
        }

        return false;
    }


    // Create the Broadsword item
    private ItemStack createBroadsword() {
        ItemStack broadsword = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = broadsword.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "Broadsword of Power");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Wield the power of the warrior."));
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(UUID.randomUUID(), "attackSpeed", -3.5, AttributeModifier.Operation.ADD_NUMBER));
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(UUID.randomUUID(), "attackDamage", 13, AttributeModifier.Operation.ADD_NUMBER));
        meta.setUnbreakable(true);  // Make the sword unbreakable
        meta.setCustomModelData(1);
        broadsword.setItemMeta(meta);
        return broadsword;
    }

    private void registerBroadswordCraftingRecipe() {
        // Create a new ItemStack for the Broadsword of Power
        ItemStack broadsword = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = broadsword.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "Broadsword of Power");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Wield the power of the warrior."));
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED,
                new AttributeModifier(UUID.randomUUID(), "attackSpeed", -3.5, AttributeModifier.Operation.ADD_NUMBER));
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE,
                new AttributeModifier(UUID.randomUUID(), "attackDamage", 13, AttributeModifier.Operation.ADD_NUMBER));
        meta.setCustomModelData(1);
        meta.setUnbreakable(true);  // Make the sword unbreakable
        broadsword.setItemMeta(meta);

        // Create the crafting recipe
        NamespacedKey key = new NamespacedKey(this, "broadsword_of_power");
        ShapedRecipe recipe = new ShapedRecipe(key, broadsword);
        recipe.shape("BAB", "BSB", "BNB");
        recipe.setIngredient('B', Material.IRON_INGOT);
        recipe.setIngredient('A', Material.DIAMOND);
        recipe.setIngredient('S', Material.DIAMOND_SWORD);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);

        // Register the recipe with the server
        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (event.getRecipe().getResult().getType() == Material.IRON_SWORD) {
            ItemStack result = event.getRecipe().getResult();
            if (result.hasItemMeta() && result.getItemMeta().hasDisplayName() &&
                    result.getItemMeta().getDisplayName().equals(ChatColor.DARK_PURPLE + "Broadsword of Power")) {

                CraftingInventory inventory = event.getInventory();
                final Player player = (Player) event.getWhoClicked(); // Make player final

                // Check if the crafting grid has enough ingredients
                if (hasEnoughIngredients(inventory)) {
                    event.setCancelled(true); // Cancel the default crafting behavior

                    // Set custom model data to 1
                    ItemMeta meta = result.getItemMeta();
                    meta.setCustomModelData(1);
                    result.setItemMeta(meta);

                    Location location = player.getEyeLocation(); // Get the player's eye location

                    // Create an ItemDisplay for the broadsword with custom model data
                    ItemDisplay itemDisplay = location.getWorld().spawn(location, ItemDisplay.class);
                    itemDisplay.setItemStack(result);

                    // Create an invisible ArmorStand to track the health
                    ArmorStand healthArmorStand = location.getWorld().spawn(location.clone().add(0, -1, 0), ArmorStand.class);
                    healthArmorStand.setVisible(false);
                    healthArmorStand.setInvulnerable(false);
                    healthArmorStand.setGravity(false);
                    healthArmorStand.setCustomNameVisible(true);
                    healthArmorStand.setCustomName(ChatColor.GREEN + "Health: 100");

                    // Create a TextDisplay for the item name
                    Location itemNameLocation = location.clone().add(0, 2, 0);
                    TextDisplay itemNameDisplay = itemNameLocation.getWorld().spawn(itemNameLocation, TextDisplay.class);
                    itemNameDisplay.setText(ChatColor.RED + "Broadsword of Power");
                    itemNameDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);

                    // Create a TextDisplay for the countdown
                    Location countdownLocation = itemNameLocation.clone().add(0, -0.5, 0);
                    TextDisplay countdownDisplay = countdownLocation.getWorld().spawn(countdownLocation, TextDisplay.class);
                    countdownDisplay.setText("Broadsword will drop in 25 minutes");
                    countdownDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);

                    // Create a BossBar to display the crafting location
                    String bossBarText = "Broadsword of Power being crafted at " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
                    BossBar bossBar = Bukkit.createBossBar(bossBarText, BarColor.PURPLE, BarStyle.SOLID);
                    bossBar.addPlayer(player);

                    // Send a title to all players
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle(ChatColor.DARK_PURPLE + "MYTHIC WEAPON", ChatColor.GRAY + "Defend the item as it charges!", 10, 70, 20);
                    }

                    boolean[] isCooldownExpired = {false};
                    BukkitRunnable[] runnables = new BukkitRunnable[3]; // Array to hold the scheduled tasks
                    final double[] health = {100}; // Health of the armor stand (100 = 20 hearts)

                    // Schedule a task to update the TextDisplays' rotation
                    runnables[0] = new BukkitRunnable() {
                        @Override
                        public void run() {
                            updateTextDisplayRotation(player, itemNameDisplay);
                            updateTextDisplayRotation(player, countdownDisplay);
                            updateArmorStandRotation(player, healthArmorStand); // New method to update armor stand rotation
                        }
                    };
                    runnables[0].runTaskTimer(this, 0L, 1L);

                    // Schedule a task to update the ItemDisplay's rotation and manage particles
                    runnables[1] = new BukkitRunnable() {
                        float yaw = 0;
                        int particleCount = 0;

                        @Override
                        public void run() {
                            updateItemDisplayRotation(itemDisplay, yaw);
                            if (!isCooldownExpired[0]) {
                                spawnParticles(itemDisplay.getLocation(), Particle.FLAME, 1);
                                particleCount++;
                                if (particleCount >= 120) {
                                    spawnParticles(itemDisplay.getLocation(), Particle.FLAME, 0);
                                    particleCount = 0;
                                }
                            }
                            yaw += 3;
                            if (yaw >= 360) {
                                yaw = 0;
                            }
                        }
                    };
                    runnables[1].runTaskTimer(this, 0L, 1L);

                    // Schedule the item drop and update the countdown
                    runnables[2] = new BukkitRunnable() {
                        int countdown = 1500; // 25 minutes in seconds

                        @Override
                        public void run() {
                            if (countdown > 0) {
                                int minutes = countdown / 60;
                                countdownDisplay.setText("Broadsword will drop in " + minutes + " minutes");
                                healthArmorStand.setCustomName(ChatColor.GREEN + "Health: " + Math.round(health[0]));
                                countdown--;
                            } else {
                                World world = location.getWorld();
                                if (world != null) {
                                    world.dropItemNaturally(location, result);
                                }
                                countdownDisplay.remove();
                                itemDisplay.remove();
                                itemNameDisplay.remove();
                                healthArmorStand.remove();
                                this.cancel();
                                isCooldownExpired[0] = true;
                                bossBar.removePlayer(player);
                            }
                        }
                    };
                    runnables[2].runTaskTimer(this, 0L, 20L);

                    // Event handler for EntityDamageByEntityEvent
                    Bukkit.getPluginManager().registerEvents(new Listener() {
                        @EventHandler
                        public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
                            if (event.getEntity().equals(healthArmorStand)) {
                                double newHealth = health[0] - event.getFinalDamage();
                                health[0] = newHealth > 0 ? newHealth : 0;
                                if (health[0] <= 0) {
                                    event.setCancelled(true);
                                    countdownDisplay.remove();
                                    itemDisplay.remove();
                                    itemNameDisplay.remove();
                                    healthArmorStand.remove();
                                    bossBar.removePlayer(player);
                                    isCooldownExpired[0] = true;
                                    for (BukkitRunnable runnable : runnables) {
                                        runnable.cancel(); // Cancel all scheduled tasks
                                    }
                                    player.sendMessage(ChatColor.RED + "The crafting of the Broadsword of Power has been stopped!");
                                }
                            }
                        }
                    }, this);

                    // Remove the ingredients from the crafting grid
                    removeIngredients(inventory);
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You don't have enough ingredients in the crafting grid to craft the Broadsword of Power!");
                }
            }
        }
    }


    @EventHandler
    public void onBroadswordPlayerFallDamage(EntityDamageEvent event) {
        // Check if the entity is a player
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            // Check if the damage is from falling
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                ItemStack itemInHand = player.getInventory().getItemInMainHand();

                // Check if the player is holding the Broadsword of Power
                if (itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasDisplayName()) {
                    String displayName = itemInHand.getItemMeta().getDisplayName();
                    if (displayName.equals(ChatColor.DARK_PURPLE + "Broadsword of Power")) {
                        event.setCancelled(true); // Cancel the fall damage
                    }
                }
            }
        }
    }



    private void updateTextDisplayRotation(Player player, TextDisplay textDisplay) {
        Location playerLocation = player.getLocation();
        Location textLocation = textDisplay.getLocation();

        double deltaX = playerLocation.getX() - textLocation.getX();
        double deltaZ = playerLocation.getZ() - textLocation.getZ();

        double yaw = Math.toDegrees(Math.atan2(-deltaX, deltaZ));

        textLocation.setYaw((float) yaw);
        textLocation.setPitch(0);
        textDisplay.teleport(textLocation);
    }

    private void updateArmorStandRotation(Player player, ArmorStand armorStand) {
        Location playerLocation = player.getLocation();
        Location armorStandLocation = armorStand.getLocation();

        double deltaX = playerLocation.getX() - armorStandLocation.getX();
        double deltaZ = playerLocation.getZ() - armorStandLocation.getZ();

        double yaw = Math.toDegrees(Math.atan2(-deltaX, deltaZ));

        armorStandLocation.setYaw((float) yaw);
        armorStandLocation.setPitch(0);
        armorStand.teleport(armorStandLocation);
    }

    private void updateItemDisplayRotation(ItemDisplay itemDisplay, float yaw) {
        Location itemLocation = itemDisplay.getLocation();
        itemLocation.setYaw(yaw);
        itemLocation.setPitch(0);
        itemDisplay.teleport(itemLocation);
    }

    private void spawnParticles(Location location, Particle particle, int count) {
        World world = location.getWorld();
        if (world != null) {
            world.spawnParticle(particle, location, count, 0.3, 0.3, 0.3, 0.1);
        }
    }

    private boolean hasEnoughIngredients(CraftingInventory inventory) {
        int ironIngotCount = 0;
        int diamondCount = 0;
        int diamondSwordCount = 0;
        int netheriteIngotCount = 0;

        for (ItemStack item : inventory.getMatrix()) {
            if (item != null) {
                if (item.getType() == Material.IRON_INGOT) {
                    ironIngotCount++;
                } else if (item.getType() == Material.DIAMOND) {
                    diamondCount++;
                } else if (item.getType() == Material.DIAMOND_SWORD) {
                    diamondSwordCount++;
                } else if (item.getType() == Material.NETHERITE_INGOT) {
                    netheriteIngotCount++;
                }
            }
        }

        return ironIngotCount >= 6 && diamondCount >= 1 && diamondSwordCount >= 1 && netheriteIngotCount >= 1;
    }

    private void removeIngredients(CraftingInventory inventory) {
        for (ItemStack item : inventory.getMatrix()) {
            if (item != null) {
                if (item.getType() == Material.IRON_INGOT || item.getType() == Material.DIAMOND ||
                        item.getType() == Material.DIAMOND_SWORD || item.getType() == Material.NETHERITE_INGOT) {
                    item.setAmount(item.getAmount() - 1);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerUseBroadsword(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if the player is holding the Broadsword of Power
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().getDisplayName().equals(ChatColor.DARK_PURPLE + "Broadsword of Power")) {
            // Check if the player is right-clicking
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // Check if the player is not on cooldown
                if (!lastUseMap.containsKey(player.getUniqueId()) || lastUseMap.get(player.getUniqueId()) + cooldown <= System.currentTimeMillis()) {
                    // Apply the dash effect
                    org.bukkit.util.Vector direction = player.getLocation().getDirection().multiply(2);
                    player.setVelocity(player.getVelocity().add(direction));

                    // Spawn purple particles (adjust the values as needed)
                    player.getWorld().spawnParticle(Particle.REDSTONE,
                            player.getLocation().add(0, 1, 0), // Adjust the position
                            10, // Number of particles
                            new Particle.DustOptions(Color.fromRGB(255, 0, 255), 1.0f)); // Purple color

                    // Update the last use time
                    lastUseMap.put(player.getUniqueId(), System.currentTimeMillis());
                } else {
                    long secondsLeft = ((lastUseMap.get(player.getUniqueId()) + cooldown) - System.currentTimeMillis()) / 1000;
                    player.sendMessage(ChatColor.RED + "The Broadsword of Power is still recharging! " + secondsLeft + " seconds left.");
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Broadsword Cooldown: " + secondsLeft + "s"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand != null && itemInHand.getType() == Material.IRON_SWORD) {
            ItemMeta meta = itemInHand.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.DARK_PURPLE + "Broadsword of Power")) {
                // Spawn particles at the player's location
                player.getWorld().spawnParticle(Particle.REDSTONE, player.getLocation(), 10, new Particle.DustOptions(Color.PURPLE, 1));
            }
        }
    }

    private void registerCraftingRecipe() {
        // Create a new ItemStack for the custom Nether Star
        ItemStack supercharge = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = supercharge.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Supercharge");
        meta.setLore(Collections.singletonList(ChatColor.AQUA + "Use this to gain special abilities!"));
        supercharge.setItemMeta(meta);

        // Create the crafting recipe
        NamespacedKey key = new NamespacedKey(this, "supercharge");
        ShapedRecipe recipe = new ShapedRecipe(key, supercharge);
        recipe.shape(" N ", "NAN", " N ");
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('A', Material.GOLDEN_APPLE);

        // Register the recipe with the serverstop
        Bukkit.addRecipe(recipe);
    }

    // Create the Harpoon Launcher item
    private ItemStack createHarpoonLauncher() {
        ItemStack harpoonLauncher = new ItemStack(Material.LEAD);
        ItemMeta meta = harpoonLauncher.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Harpoon Launcher");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Pull yourself towards your enemies!"));
        meta.setUnbreakable(true);  // Make the harpoon launcher unbreakable
        meta.setCustomModelData(1);
        harpoonLauncher.setItemMeta(meta);
        return harpoonLauncher;
    }



    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String itemName = item.getItemMeta().getDisplayName();
            if (itemName.equals(ChatColor.AQUA + "Harpoon Launcher")) {
                if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

                    // Cooldown check
                    UUID playerId = player.getUniqueId();
                    long currentTime = System.currentTimeMillis();

                    if (harpoonCooldowns.containsKey(playerId)) {
                        long lastUseTime = harpoonCooldowns.get(playerId);
                        if (currentTime - lastUseTime < COOLDOWN_TIME) {
                            long timeLeft = (COOLDOWN_TIME - (currentTime - lastUseTime)) / 1000;
                            player.sendMessage(ChatColor.RED + "Harpoon Launcher is on cooldown! Try again in " + timeLeft + " seconds.");
                            return;
                        }
                    }

                    // Set cooldown time
                    harpoonCooldowns.put(playerId, currentTime);

                    // Check if the player has a trident in their inventory
                    ItemStack tridentItem = null;
                    for (ItemStack inventoryItem : player.getInventory().getContents()) {
                        if (inventoryItem != null && inventoryItem.getType() == Material.TRIDENT) {
                            tridentItem = inventoryItem;
                            break;
                        }
                    }

                    if (tridentItem == null) {
                        player.sendMessage(ChatColor.RED + "You need a trident in your inventory to use the harpoon launcher!");
                        return;
                    }

                    // Remove one trident from the player's inventory
                    tridentItem.setAmount(tridentItem.getAmount() - 1);

                    // Launch a trident
                    Trident trident = player.launchProjectile(Trident.class);
                    trident.setMetadata("HarpoonLauncher", new FixedMetadataValue(this, true));

                    // Track the initial location of the trident
                    Location initialLocation = trident.getLocation();
                    trident.setMetadata("InitialLocation", new FixedMetadataValue(this, initialLocation));

                    // Schedule a task to check the distance the trident has traveled
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!trident.isValid()) {
                                this.cancel();
                                return;
                            }

                            Location currentLocation = trident.getLocation();
                            double distance = currentLocation.distance(initialLocation);
                            if (distance > 15) {
                                Vector velocity = trident.getVelocity();
                                velocity.multiply(0.1); // Reduce velocity to force it to land
                                trident.setVelocity(velocity);
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(this, 0L, 1L);

                    // Spawn an invisible bat at the player's location
                    Bat bat = player.getWorld().spawn(player.getLocation(), Bat.class);
                    bat.setSilent(true);
                    bat.setInvulnerable(true);
                    bat.setInvisible(true);
                    bat.setAI(false); // Disable AI so it does not move

                    // Add fire resistance to the bat
                    bat.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1, false, false));

                    // Leash the bat to the player
                    bat.setLeashHolder(player);

                    // Store the bat in the trident metadata
                    trident.setMetadata("LeashedBat", new FixedMetadataValue(this, bat));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            // Check if the damage cause is falling
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                ItemStack itemInHand = player.getInventory().getItemInMainHand();

                // Check if the player is holding the Harpoon Launcher
                if (itemInHand != null && itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasDisplayName()) {
                    String itemName = itemInHand.getItemMeta().getDisplayName();
                    if (itemName.equals(ChatColor.AQUA + "Harpoon Launcher")) {
                        // Cancel the fall damage
                        event.setCancelled(true);
                    }
                }
            }
        }
    }


    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String itemName = item.getItemMeta().getDisplayName();
            if (itemName.equals(ChatColor.AQUA + "Harpoon Launcher")) {
                // Cancel the event to prevent leashing
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHookProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile.hasMetadata("HarpoonLauncher") && projectile.getShooter() instanceof Player) {
            Player player = (Player) projectile.getShooter();
            Location hitLoc = projectile.getLocation();

            // Retrieve the leashed bat from the trident metadata
            Bat bat = (Bat) projectile.getMetadata("LeashedBat").get(0).value();

            // Teleport the bat to the hit location
            bat.teleport(hitLoc);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.getLocation().distance(hitLoc) < 1.0) {
                        // Remove the bat when the player reaches the hit location
                        bat.remove();
                        this.cancel();
                        return;
                    }

                    // Calculate the vector from the player to the hit location
                    Vector vector = hitLoc.toVector().subtract(player.getLocation().toVector());
                    double distance = vector.length(); // Distance to the hit location

                    // Determine the amount of upward force
                    double upwardForce = 0;
                    if (hitLoc.getY() > player.getLocation().getY()) {
                        upwardForce = Math.min(1.0, (hitLoc.getY() - player.getLocation().getY()) / distance); // Adjust this to control upward force
                    }

                    // Normalize the vector and apply the calculated upward force
                    vector.normalize().multiply(0.5); // Adjust this to control the base speed

                    // Check for blocks in front of the player
                    Vector direction = player.getLocation().getDirection();
                    Block blockInFront = player.getWorld().getBlockAt(player.getLocation().add(direction.multiply(1.5)));

                    if (blockInFront.getType().isSolid()) {
                        vector.setY(Math.max(vector.getY(), 0.5)); // Add upward force if there's a block in front
                    } else {
                        vector.setY(vector.getY() + upwardForce); // Add the upward force
                    }

                    // Pull the player towards the hit location
                    player.setVelocity(vector);
                }
            }.runTaskTimer(this, 0L, 1L);
        }
    }



    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        if (event.getEntity().getItemStack().getType() == Material.LEAD) {
            // Check the distance between the spawn location and any nearby tridents with the HarpoonLauncher metadata
            for (Trident trident : event.getLocation().getWorld().getEntitiesByClass(Trident.class)) {
                if (trident.hasMetadata("HarpoonLauncher") && trident.getLocation().distance(event.getLocation()) < 5.0) { // Increase range (e.g., 5.0)
                    event.getEntity().remove(); // Remove the lead that spawns near the trident
                }
            }
        }
    }

    private void registerHarpoonLauncherCraftingRecipe() {
        // Create a new ItemStack for the Harpoon Launcher
        ItemStack harpoonLauncher = new ItemStack(Material.LEAD);
        ItemMeta meta = harpoonLauncher.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Harpoon Launcher");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Pull yourself towards your enemies!"));

        // Set the custom model data
        meta.setCustomModelData(1);

        // Make the harpoon launcher unbreakable
        meta.setUnbreakable(true);

        harpoonLauncher.setItemMeta(meta);

        // Create the crafting recipe
        NamespacedKey key = new NamespacedKey(this, "harpoon_launcher");
        ShapedRecipe recipe = new ShapedRecipe(key, harpoonLauncher);
        recipe.shape("AAA", "LTL", "ACA");
        recipe.setIngredient('L', Material.LEAD);
        recipe.setIngredient('T', Material.TRIDENT);
        recipe.setIngredient('C', Material.CROSSBOW);
        // Remove the following line:
        // recipe.setIngredient('A', Material.AIR);

        // Register the recipe with the server
        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onCraftHarpoonLauncherItem(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (result.hasItemMeta() && result.getItemMeta().hasDisplayName()) {
            String displayName = result.getItemMeta().getDisplayName();
            if (displayName.equals(ChatColor.AQUA + "Harpoon Launcher")) {
                CraftingInventory inventory = event.getInventory();
                Player player = (Player) event.getWhoClicked();

                if (hasEnoughIngredientsForHarpoonLauncher(inventory)) {
                    event.setCancelled(true);

                    // Set custom model data for the Harpoon Launcher
                    ItemMeta meta = result.getItemMeta();
                    if (meta != null) {
                        meta.setCustomModelData(1); // Set custom model data to 1
                        result.setItemMeta(meta);
                    }

                    Location location = player.getEyeLocation();
                    ItemDisplay itemDisplay = location.getWorld().spawn(location, ItemDisplay.class);
                    itemDisplay.setItemStack(result); // Ensure the ItemDisplay uses the updated item with custom model data

                    ArmorStand healthArmorStand = location.getWorld().spawn(location.clone().add(0, -1, 0), ArmorStand.class);
                    healthArmorStand.setVisible(false);
                    healthArmorStand.setInvulnerable(false);
                    healthArmorStand.setGravity(false);
                    healthArmorStand.setCustomNameVisible(true);
                    healthArmorStand.setCustomName(ChatColor.GREEN + "Health: 100");

                    Location itemNameLocation = location.clone().add(0, 2, 0);
                    TextDisplay itemNameDisplay = itemNameLocation.getWorld().spawn(itemNameLocation, TextDisplay.class);
                    itemNameDisplay.setText(displayName);
                    itemNameDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);

                    Location countdownLocation = itemNameLocation.clone().add(0, -0.5, 0);
                    TextDisplay countdownDisplay = countdownLocation.getWorld().spawn(countdownLocation, TextDisplay.class);
                    countdownDisplay.setText(displayName + " will drop in 25 minutes");
                    countdownDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);

                    String bossBarText = displayName + " being crafted at " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
                    BossBar bossBar = Bukkit.createBossBar(bossBarText, BarColor.PURPLE, BarStyle.SOLID);
                    bossBar.addPlayer(player);

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle(ChatColor.DARK_PURPLE + "MYTHIC WEAPON", ChatColor.GRAY + "Defend the item as it charges!", 10, 70, 20);
                    }

                    boolean[] isCooldownExpired = {false};
                    BukkitRunnable[] runnables = new BukkitRunnable[3];
                    final double[] health = {100};

                    runnables[0] = new BukkitRunnable() {
                        @Override
                        public void run() {
                            updateTextDisplayRotation(player, itemNameDisplay);
                            updateTextDisplayRotation(player, countdownDisplay);
                            updateArmorStandRotation(player, healthArmorStand);
                        }
                    };
                    runnables[0].runTaskTimer(this, 0L, 1L);

                    runnables[1] = new BukkitRunnable() {
                        float yaw = 0;
                        int particleCount = 0;

                        @Override
                        public void run() {
                            updateItemDisplayRotation(itemDisplay, yaw);
                            if (!isCooldownExpired[0]) {
                                spawnParticles(itemDisplay.getLocation(), Particle.FLAME, 1);
                                particleCount++;
                                if (particleCount >= 120) {
                                    spawnParticles(itemDisplay.getLocation(), Particle.FLAME, 0);
                                    particleCount = 0;
                                }
                            }
                            yaw += 3;
                            if (yaw >= 360) {
                                yaw = 0;
                            }
                        }
                    };
                    runnables[1].runTaskTimer(this, 0L, 1L);

                    runnables[2] = new BukkitRunnable() {
                        int countdown = 1500; // 25 minutes in seconds

                        @Override
                        public void run() {
                            if (countdown > 0) {
                                int minutes = countdown / 60;
                                countdownDisplay.setText(displayName + " will drop in " + minutes + " minutes");
                                healthArmorStand.setCustomName(ChatColor.GREEN + "Health: " + Math.round(health[0]));
                                countdown--;
                            } else {
                                World world = location.getWorld();
                                if (world != null) {
                                    world.dropItemNaturally(location, result);
                                }
                                countdownDisplay.remove();
                                itemDisplay.remove();
                                itemNameDisplay.remove();
                                healthArmorStand.remove();
                                this.cancel();
                                isCooldownExpired[0] = true;
                                bossBar.removePlayer(player);
                            }
                        }
                    };
                    runnables[2].runTaskTimer(this, 0L, 20L);

                    Bukkit.getPluginManager().registerEvents(new Listener() {
                        @EventHandler
                        public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
                            if (event.getEntity().equals(healthArmorStand)) {
                                double newHealth = health[0] - event.getFinalDamage();
                                health[0] = newHealth > 0 ? newHealth : 0;
                                if (health[0] <= 0) {
                                    event.setCancelled(true);
                                    countdownDisplay.remove();
                                    itemDisplay.remove();
                                    itemNameDisplay.remove();
                                    healthArmorStand.remove();
                                    bossBar.removePlayer(player);
                                    isCooldownExpired[0] = true;
                                    for (BukkitRunnable runnable : runnables) {
                                        runnable.cancel();
                                    }
                                    player.sendMessage(ChatColor.RED + "The crafting of " + displayName + " has been stopped!");
                                }
                            }
                        }
                    }, this);

                    removeIngredientsHarpoon(inventory);
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You don't have enough ingredients in the crafting grid to craft the " + displayName + "!");
                }
            }
        }
    }


    private boolean hasEnoughIngredientsForHarpoonLauncher(CraftingInventory inventory) {
        int tridentCount = 0;
        int leadCount = 0;
        int crossbowCount = 0;

        for (ItemStack item : inventory.getMatrix()) {
            if (item != null) {
                if (item.getType() == Material.TRIDENT) {
                    tridentCount++;
                } else if (item.getType() == Material.LEAD) {
                    leadCount++;
                } else if (item.getType() == Material.CROSSBOW) {
                    crossbowCount++;
                }
            }
        }

        return tridentCount >= 1 && leadCount >= 2 && crossbowCount >= 1;
    }

    private void removeIngredientsHarpoon(CraftingInventory inventory) {
        for (ItemStack item : inventory.getMatrix()) {
            if (item != null) {
                if (item.getType() == Material.TRIDENT || item.getType() == Material.LEAD || item.getType() == Material.CROSSBOW) {
                    item.setAmount(item.getAmount() - 1);
                }
            }
        }
    }




    public class SonicBlastCommandExecutor implements CommandExecutor {
        private final SonicWeapon plugin;

        public SonicBlastCommandExecutor(SonicWeapon plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { //Sonic Bow
            if (sender instanceof Player) {
                Player player = (Player) sender;

                // Check if the player is an operator
                if (player.isOp()) {
                    // Create a new ItemStack for your custom weapon
                    ItemStack sonicBlast = new ItemStack(Material.BOW);
                    // Get the ItemMeta of the ItemStack
                    ItemMeta meta = sonicBlast.getItemMeta();

                    // Set the custom name
                    meta.setDisplayName(ChatColor.AQUA + "Sonic Bow");

                    // Set the lore (meta description)
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Unleash the power of sound with this mystical bow!");
                    meta.setLore(lore);
                    meta.setCustomModelData(1);

                    // Apply the updated ItemMeta to the ItemStack
                    sonicBlast.setItemMeta(meta);

                    // Give the player your custom weapon
                    player.getInventory().addItem(sonicBlast);
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return false;
                }
            } else {
                sender.sendMessage("You must be a player to use this command.");
                return false;
            }
        }
    }


        private void registerSonicBowCraftingRecipe() {
        // Create a new ItemStack for the Sonic Bow
        ItemStack sonicBow = new ItemStack(Material.BOW);
        ItemMeta meta = sonicBow.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Sonic Bow");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Unleash the power of sound with this mystical bow!"));

        // Set the custom model data
        meta.setCustomModelData(1);

        // Make the sonic bow unbreakable
        meta.setUnbreakable(true);

        sonicBow.setItemMeta(meta);

        // Create the crafting recipe
        NamespacedKey key = new NamespacedKey(this, "sonic_bow");
        ShapedRecipe recipe = new ShapedRecipe(key, sonicBow);
        recipe.shape("DCD", "CBC", "DCD");
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('C', Material.ECHO_SHARD);
        recipe.setIngredient('B', Material.BOW);

        // Register the recipe with the server
        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onCraftSonicWeaponItem(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        if (result.hasItemMeta() && result.getItemMeta().hasDisplayName()) {
            String displayName = result.getItemMeta().getDisplayName();
            if (displayName.equals(ChatColor.AQUA + "Sonic Bow")) {
                CraftingInventory inventory = event.getInventory();
                Player player = (Player) event.getWhoClicked();

                if (hasEnoughIngredientsForSonicBow(inventory)) {
                    event.setCancelled(true);

                    // Set custom model data to 1
                    ItemMeta meta = result.getItemMeta();
                    meta.setCustomModelData(1);
                    result.setItemMeta(meta);

                    Location location = player.getEyeLocation();
                    ItemDisplay itemDisplay = location.getWorld().spawn(location, ItemDisplay.class);
                    itemDisplay.setItemStack(result);

                    ArmorStand healthArmorStand = location.getWorld().spawn(location.clone().add(0, -1, 0), ArmorStand.class);
                    healthArmorStand.setVisible(false);
                    healthArmorStand.setInvulnerable(false);
                    healthArmorStand.setGravity(false);
                    healthArmorStand.setCustomNameVisible(true);
                    healthArmorStand.setCustomName(ChatColor.GREEN + "Health: 100");

                    Location itemNameLocation = location.clone().add(0, 2, 0);
                    TextDisplay itemNameDisplay = itemNameLocation.getWorld().spawn(itemNameLocation, TextDisplay.class);
                    itemNameDisplay.setText(displayName);
                    itemNameDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);

                    Location countdownLocation = itemNameLocation.clone().add(0, -0.5, 0);
                    TextDisplay countdownDisplay = countdownLocation.getWorld().spawn(countdownLocation, TextDisplay.class);
                    countdownDisplay.setText(displayName + " will drop in 25 minutes");
                    countdownDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);

                    String bossBarText = displayName + " being crafted at " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
                    BossBar bossBar = Bukkit.createBossBar(bossBarText, BarColor.PURPLE, BarStyle.SOLID);
                    bossBar.addPlayer(player);

                    // Send a title to all players
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle(ChatColor.DARK_PURPLE + "MYTHIC WEAPON", ChatColor.GRAY + "Defend the item as it charges!", 10, 70, 20);
                    }

                    boolean[] isCooldownExpired = {false};
                    BukkitRunnable[] runnables = new BukkitRunnable[3];
                    final double[] health = {100};

                    runnables[0] = new BukkitRunnable() {
                        @Override
                        public void run() {
                            updateTextDisplayRotation(player, itemNameDisplay);
                            updateTextDisplayRotation(player, countdownDisplay);
                            updateArmorStandRotation(player, healthArmorStand);
                        }
                    };
                    runnables[0].runTaskTimer(this, 0L, 1L);

                    runnables[1] = new BukkitRunnable() {
                        float yaw = 0;
                        int particleCount = 0;

                        @Override
                        public void run() {
                            updateItemDisplayRotation(itemDisplay, yaw);
                            if (!isCooldownExpired[0]) {
                                spawnParticles(itemDisplay.getLocation(), Particle.FLAME, 1);
                                particleCount++;
                                if (particleCount >= 120) {
                                    spawnParticles(itemDisplay.getLocation(), Particle.FLAME, 0);
                                    particleCount = 0;
                                }
                            }
                            yaw += 3;
                            if (yaw >= 360) {
                                yaw = 0;
                            }
                        }
                    };
                    runnables[1].runTaskTimer(this, 0L, 1L);

                    runnables[2] = new BukkitRunnable() {
                        int countdown = 1500; // 25 minutes in seconds

                        @Override
                        public void run() {
                            if (countdown > 0) {
                                int minutes = countdown / 60;
                                countdownDisplay.setText(displayName + " will drop in " + minutes + " minutes");
                                healthArmorStand.setCustomName(ChatColor.GREEN + "Health: " + Math.round(health[0]));
                                countdown--;
                            } else {
                                World world = location.getWorld();
                                if (world != null) {
                                    world.dropItemNaturally(location, result);
                                }
                                countdownDisplay.remove();
                                itemDisplay.remove();
                                itemNameDisplay.remove();
                                healthArmorStand.remove();
                                this.cancel();
                                isCooldownExpired[0] = true;
                                bossBar.removePlayer(player);
                            }
                        }
                    };
                    runnables[2].runTaskTimer(this, 0L, 20L);

                    Bukkit.getPluginManager().registerEvents(new Listener() {
                        @EventHandler
                        public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
                            if (event.getEntity().equals(healthArmorStand)) {
                                double newHealth = health[0] - event.getFinalDamage();
                                health[0] = newHealth > 0 ? newHealth : 0;
                                if (health[0] <= 0) {
                                    event.setCancelled(true);
                                    countdownDisplay.remove();
                                    itemDisplay.remove();
                                    itemNameDisplay.remove();
                                    healthArmorStand.remove();
                                    bossBar.removePlayer(player);
                                    isCooldownExpired[0] = true;
                                    for (BukkitRunnable runnable : runnables) {
                                        runnable.cancel();
                                    }
                                    player.sendMessage(ChatColor.RED + "The crafting of " + displayName + " has been stopped!");
                                }
                            }
                        }
                    }, this);

                    removeIngredientsSonicBow(inventory);
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You don't have enough ingredients in the crafting grid to craft the " + displayName + "!");
                }
            }
        }
    }


    private boolean hasEnoughIngredientsForSonicBow(CraftingInventory inventory) {
        int diamondCount = 0;
        int echoShardCount = 0;
        int bowCount = 0;

        for (ItemStack item : inventory.getMatrix()) {
            if (item != null) {
                if (item.getType() == Material.DIAMOND) {
                    diamondCount++;
                } else if (item.getType() == Material.ECHO_SHARD) {
                    echoShardCount++;
                } else if (item.getType() == Material.BOW) {
                    bowCount++;
                }
            }
        }

        return diamondCount >= 4 && echoShardCount >= 4 && bowCount >= 1;
    }

    private void removeIngredientsSonicBow(CraftingInventory inventory) {
        for (ItemStack item : inventory.getMatrix()) {
            if (item != null) {
                if (item.getType() == Material.DIAMOND || item.getType() == Material.ECHO_SHARD || item.getType() == Material.BOW) {
                    item.setAmount(item.getAmount() - 1);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

        if (action.equals(Action.RIGHT_CLICK_AIR) || action.equals(Action.RIGHT_CLICK_BLOCK)) {
            if (item != null && item.getType() == Material.BOW && item.getItemMeta().hasDisplayName() &&
                    item.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "Sonic Bow")) {
                // Check if the bow is in the offhand slot
                if (event.getHand() == EquipmentSlot.OFF_HAND) {
                    player.sendMessage(ChatColor.RED + "You cannot use the Sonic Bow in your offhand.");
                    event.setCancelled(true);
                    return;
                }
                // Launch the sonic blast
                player.launchProjectile(Arrow.class);
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            if (arrow.getShooter() instanceof Player) {
                Player player = (Player) arrow.getShooter();
                ItemStack mainHandItem = player.getInventory().getItemInMainHand();
                ItemStack offHandItem = player.getInventory().getItemInOffHand();

                boolean isSonicBow = (mainHandItem != null && mainHandItem.getType() == Material.BOW &&
                        mainHandItem.getItemMeta().hasDisplayName() &&
                        mainHandItem.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "Sonic Bow")) ||
                        (offHandItem != null && offHandItem.getType() == Material.BOW &&
                                offHandItem.getItemMeta().hasDisplayName() &&
                                offHandItem.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "Sonic Bow"));

                if (isSonicBow) {
                    // Check for cooldown
                    if (cooldowns.containsKey(player.getUniqueId())) {
                        long secondsLeft = ((cooldowns.get(player.getUniqueId()) / 1000) + 5) - (System.currentTimeMillis() / 1000);
                        if (secondsLeft > 0) {
                            player.sendMessage("You can't use this for another " + secondsLeft + " seconds!");
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("Sonic Bow Cooldown: " + secondsLeft + "s"));
                            event.setCancelled(true);
                            return;
                        } else {
                            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                        }
                    } else {
                        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                    }
                    // Add the arrow to the trail
                    trail.addArrow(arrow);
                }
            }
        }
    }


    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            if (arrow.getShooter() instanceof Player) {
                Player player = (Player) arrow.getShooter();
                if (player.getInventory().getItemInMainHand().getType() == Material.BOW &&
                        player.getInventory().getItemInMainHand().getItemMeta().hasDisplayName() &&
                        player.getInventory().getItemInMainHand().getItemMeta().getDisplayName().equals(ChatColor.AQUA + "Sonic Bow")) {
                    // Get the location of the hit
                    Location hitLocation = arrow.getLocation();
                    // Create a sonic boom effect
                    hitLocation.getWorld().spawnParticle(Particle.valueOf("SONIC_BOOM"), hitLocation, 10);
                    // Play the sonic boom sound
                    hitLocation.getWorld().playSound(hitLocation, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0F, 1.0F);
                    // Get all entities within a certain radius
                    Collection<Entity> nearbyEntities = hitLocation.getWorld().getNearbyEntities(hitLocation, 10, 10, 10);
                    // Apply the sonic boom damage to all entities within the radius
                    for (Entity entity : nearbyEntities) {
                        if (entity instanceof LivingEntity) {
                            ((LivingEntity) entity).damage(5);
                        }
                    }
                }
            }
        }
    }

    public class BoostCreditsCommandExecutor implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (command.getName().equalsIgnoreCase("boostcredits")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    player.sendMessage(ChatColor.GOLD + "BoostWeapon Plugin Credits:");
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "iPlayGames2020: " + ChatColor.DARK_PURPLE + "Developer");
                    player.sendMessage(ChatColor.GREEN + "Rando Person: " + ChatColor.DARK_GREEN + "Designer");
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
                    return false;
                }
            }
            return false;
        }
    }

    public class Trail {

        private Particle particle;
        private List<Arrow> arrows = new ArrayList<>();
        private Particle.DustOptions dustOptions = new Particle.DustOptions(Color.PURPLE, 1);

        public Trail(Particle particle) {
            this.particle = particle;
        }

        public void addArrow(Arrow arrow) {
            arrows.add(arrow);
        }

        public void tick() {
            Iterator<Arrow> iterator = arrows.iterator();
            while (iterator.hasNext()) {
                Arrow arrow = iterator.next();
                if (arrow.isDead() || arrow.isOnGround()) {
                    iterator.remove();
                } else {
                    arrow.getWorld().spawnParticle(particle, arrow.getLocation(), 1, dustOptions);
                }
            }
        }
    }
}