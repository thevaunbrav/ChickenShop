package net.chickenshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class ChickenShop extends JavaPlugin implements Listener, CommandExecutor {
    private Economy economy;
    private final Map<UUID, Selection> selections = new HashMap<>();
    private record Selection(int category, int index, int quantity) {}
    private record ShopHolder(UUID owner, int category) implements InventoryHolder {
        public Inventory getInventory() { return null; }
    }
    private static final String[] NAMES = {"END", "NETHER", "PvP", "YEMEK", "KRİSTALLER"};
    private static final Material[] ICONS = {Material.END_STONE, Material.NETHERRACK, Material.TOTEM_OF_UNDYING, Material.COOKED_BEEF, Material.AMETHYST_SHARD};
    private static final EntityType[] MOBS = {EntityType.PIG, EntityType.COW, EntityType.ZOMBIE, EntityType.SPIDER, EntityType.SKELETON, EntityType.CREEPER, EntityType.ZOMBIFIED_PIGLIN, EntityType.BLAZE, EntityType.IRON_GOLEM};
    private static final String[] MOB_NAMES = {"Domuz", "İnek", "Zombi", "Örümcek", "İskelet", "Creeper", "Zombileşmiş Piglin", "Blaze", "Demir Golem"};
    private static final Material[][] ITEMS = {
        {Material.ENDER_CHEST,Material.ENDER_PEARL,Material.END_STONE,Material.DRAGON_BREATH,Material.END_ROD,Material.CHORUS_FRUIT,Material.POPPED_CHORUS_FRUIT,Material.SHULKER_SHELL,Material.SHULKER_BOX},
        {Material.BLAZE_ROD,Material.NETHER_WART,Material.GLOWSTONE_DUST,Material.MAGMA_CREAM,Material.GHAST_TEAR,Material.QUARTZ,Material.SOUL_SAND,Material.MAGMA_BLOCK,Material.CRYING_OBSIDIAN},
        {Material.OBSIDIAN,Material.END_CRYSTAL,Material.RESPAWN_ANCHOR,Material.GLOWSTONE,Material.TOTEM_OF_UNDYING,Material.ENDER_PEARL,Material.GOLDEN_APPLE,Material.EXPERIENCE_BOTTLE,Material.TRIDENT},
        {Material.POTATO,Material.SWEET_BERRIES,Material.MELON_SLICE,Material.CARROT,Material.APPLE,Material.COOKED_CHICKEN,Material.COOKED_BEEF,Material.GOLDEN_CARROT,Material.GOLDEN_APPLE}
    };
    private static final String[][] ITEM_NAMES = {
        {"Ender Sandığı","Ender İncisi","End Taşı","Ejderha Nefesi","End Çubuğu","Koro Meyvesi","Patlamış Koro Meyvesi","Shulker Kabuğu","Shulker Kutusu"},
        {"Blaze Çubuğu","Nether Yumrusu","Işık Taşı Tozu","Magma Kremi","Ghast Gözyaşı","Nether Kuvarsı","Ruh Kumu","Magma Bloğu","Ağlayan Obsidyen"},
        {"Obsidyen","End Kristali","Yeniden Doğma Çapası","Işık Taşı","Ölümsüzlük Totemi","Ender İncisi","Altın Elma","Deneyim Şişesi","Üç Dişli Mızrak"},
        {"Patates","Tatlı Meyveler","Karpuz Dilimi","Havuç","Elma","Pişmiş Tavuk","Pişmiş Biftek","Altın Havuç","Altın Elma"}
    };
    private static final double[][] PRICES = {
        {250,100,15,200,150,30,45,1200,1800},
        {200,50,35,125,350,45,25,100,250},
        {200,450,1200,80,3500,100,500,120,5000},
        {20,15,15,15,20,35,40,75,500}
    };
    @Override public void onEnable() {
        saveDefaultConfig();
        RegisteredServiceProvider<Economy> service = getServer().getServicesManager().getRegistration(Economy.class);
        if(service == null || getServer().getPluginManager().getPlugin("PlayerPoints") == null) {
            getLogger().severe("Vault ekonomi servisi ve PlayerPoints gerekli!");
            getServer().getPluginManager().disablePlugin(this); return;
        }
        economy = service.getProvider();
        Objects.requireNonNull(getCommand("shop")).setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player p)) { sender.sendMessage("Bu komut oyuncular icindir."); return true; }
        if(!p.hasPermission("chickenshop.use")) { p.sendMessage("§cYetkin yok."); return true; }
        openHome(p); return true;
    }
    private ItemStack button(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
    private void openHome(Player p) {
        Inventory inv = Bukkit.createInventory(new ShopHolder(p.getUniqueId(), -1), 27, "SUNUCU MAĞAZASI");
        int[] slots = {11,12,13,14,15};
        for(int i=0;i<5;i++)
            inv.setItem(slots[i], button(ICONS[i], "§e§l"+NAMES[i], i==4 ? "§dPlayerPoints puanları" : "§6Dolar ile alışveriş", "§7Açmak için tıkla"));
        p.openInventory(inv);
    }
    private int mobSlot(int i) { return 9+i; }
    private void openCategory(Player p, int c) {
        Inventory inv = Bukkit.createInventory(new ShopHolder(p.getUniqueId(), c), 27, NAMES[c]);
        if(c==4) {
            for(int i=0;i<MOBS.length;i++) {
                int price = getConfig().getInt("spawners."+MOBS[i].name(), 100);
                inv.setItem(mobSlot(i), i==8
                    ? button(Material.SPAWNER,"§d"+MOB_NAMES[i]+" Spawnerı","§7Fiyat: §d"+price+" puan","§c§lDİKKAT: §cDemir Golem XP vermez!","§eSatın almak için tıkla")
                    : button(Material.SPAWNER,"§d"+MOB_NAMES[i]+" Spawnerı","§7Fiyat: §d"+price+" puan","§eSatın almak için tıkla"));
            }
        } else {
            for(int i=0;i<ITEMS[c].length;i++) {
                Material m=ITEMS[c][i];
                inv.setItem(9+i,button(m,"§f"+ITEM_NAMES[c][i],"§7Fiyat: §6$"+PRICES[c][i],"§eSatın almak için tıkla"));
            }
        }
        inv.setItem(18,button(Material.BARRIER,"§cGeri"));
        p.openInventory(inv);
    }
    private void openQuantity(Player p, int category, int index, int quantity) {
        Material material = ITEMS[category][index];
        int maxStack = Math.max(1, material.getMaxStackSize());
        int step = maxStack >= 64 ? 64 : maxStack >= 16 ? 16 : 1;
        int amount = Math.max(1, Math.min(maxStack, quantity));
        selections.put(p.getUniqueId(), new Selection(category, index, amount));
        Inventory inv = Bukkit.createInventory(new ShopHolder(p.getUniqueId(), -2), 27, "Miktar Seç • " + ITEM_NAMES[category][index]);
        inv.setItem(9, button(Material.RED_STAINED_GLASS_PANE, "§c-"+step));
        inv.setItem(10, button(Material.RED_STAINED_GLASS_PANE, "§c-10"));
        inv.setItem(11, button(Material.RED_STAINED_GLASS_PANE, "§c-1"));
        ItemStack display = button(material, "§e"+ITEM_NAMES[category][index],
                "§7Miktar: §f"+amount,
                "§7Birim fiyat: §6$"+PRICES[category][index],
                "§7Toplam: §6$"+String.format(java.util.Locale.US, "%.2f", PRICES[category][index]*amount));
        display.setAmount(Math.min(amount, maxStack));
        inv.setItem(13, display);
        inv.setItem(15, button(Material.LIME_STAINED_GLASS_PANE, "§a+1"));
        inv.setItem(16, button(Material.LIME_STAINED_GLASS_PANE, "§a+10"));
        inv.setItem(17, button(Material.LIME_STAINED_GLASS_PANE, "§a+"+step));
        inv.setItem(21, button(Material.LIME_CONCRETE, "§a§lSatın Al", "§7Menü açık kalır"));
        inv.setItem(23, button(Material.BARRIER, "§c§lİptal", "§7Kategoriye dön"));
        p.openInventory(inv);
    }
    private void refreshQuantity(Player p, Selection selection, int quantity) {
        // Yeniden açmak yerine mevcut envanteri güncelle: spam alışverişte ekran kapanmaz.
        int maxStack = Math.max(1, ITEMS[selection.category()][selection.index()].getMaxStackSize());
        int amount = Math.max(1, Math.min(maxStack, quantity));
        selections.put(p.getUniqueId(), new Selection(selection.category(), selection.index(), amount));
        Material material = ITEMS[selection.category()][selection.index()];
        ItemStack display = button(material, "§e"+ITEM_NAMES[selection.category()][selection.index()],
                "§7Miktar: §f"+amount,
                "§7Birim fiyat: §6$"+PRICES[selection.category()][selection.index()],
                "§7Toplam: §6$"+String.format(java.util.Locale.US, "%.2f", PRICES[selection.category()][selection.index()]*amount));
        display.setAmount(Math.min(amount, Math.max(1, material.getMaxStackSize())));
        p.getOpenInventory().getTopInventory().setItem(13, display);
    }
    private void buySelected(Player p, Selection selection) {
        Material material = ITEMS[selection.category()][selection.index()];
        int quantity = selection.quantity();
        double price = PRICES[selection.category()][selection.index()] * quantity;
        if(!Double.isFinite(price) || price<=0) {p.sendMessage("§cGeçersiz fiyat."); return;}
        if(!economy.has(p, price)) {p.sendMessage("§cYeterli paran yok. Gerekli: $"+price);return;}
        if(!economy.withdrawPlayer(p, price).transactionSuccess()) {p.sendMessage("§cÖdeme başarısız.");return;}
        int maxStack = Math.max(1, material.getMaxStackSize());
        for(int left=quantity;left>0;) {
            int take=Math.min(maxStack,left);
            give(p,new ItemStack(material,take));
            left-=take;
        }
        p.sendMessage("§a"+quantity+"x "+ITEM_NAMES[selection.category()][selection.index()]+" satın alındı! §6-$"+String.format(java.util.Locale.US,"%.2f",price));
    }
    private Object pointsApi() throws Exception {
        Object plugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        return plugin.getClass().getMethod("getAPI").invoke(plugin);
    }
    private int getPoints(Player p) {
        try {
            Object api=pointsApi();
            return ((Number)api.getClass().getMethod("look",UUID.class).invoke(api,p.getUniqueId())).intValue();
        } catch(Exception e) { getLogger().warning("PlayerPoints bakiyesi okunamadi: "+e.getMessage()); return -1; }
    }
    private boolean takePoints(Player p,int amount) {
        try {
            Object api=pointsApi();
            return Boolean.TRUE.equals(api.getClass().getMethod("take",UUID.class,int.class).invoke(api,p.getUniqueId(),amount));
        } catch(Exception e) { getLogger().warning("PlayerPoints odeme hatasi: "+e.getMessage()); return false; }
    }
    private ItemStack spawner(int i) {
        ItemStack item=new ItemStack(Material.SPAWNER);
        BlockStateMeta meta=(BlockStateMeta)item.getItemMeta();
        if(meta.getBlockState() instanceof CreatureSpawner cs) {
            cs.setSpawnedType(MOBS[i]);
            meta.setBlockState(cs);
        }
        meta.setDisplayName("§d"+MOB_NAMES[i]+" Spawnerı");
        if(i==8) meta.setLore(Arrays.asList("§c§lDİKKAT: §cDemir Golem XP vermez!", "§7XP: §8Bu özellik bu spawner için kullanılamaz."));
        item.setItemMeta(meta);
        return item;
    }
    private void give(Player p,ItemStack item) {
        p.getInventory().addItem(item).values().forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(),left));
    }
    @EventHandler public void close(InventoryCloseEvent event) {
        if(event.getInventory().getHolder() instanceof ShopHolder holder && holder.category()==-2)
            Bukkit.getScheduler().runTask(this, () -> {
                if(!(event.getPlayer().getOpenInventory().getTopInventory().getHolder() instanceof ShopHolder h) || h.category()!=-2)
                    selections.remove(event.getPlayer().getUniqueId());
            });
    }
    @EventHandler public void click(InventoryClickEvent event) {
        if(!(event.getView().getTopInventory().getHolder() instanceof ShopHolder holder)) return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(holder.owner())) return;
        int slot=event.getRawSlot();
        if(slot<0 || slot>=event.getView().getTopInventory().getSize()) return;
        if(holder.category()==-1) {
            int[] slots={11,12,13,14,15};
            for(int i=0;i<slots.length;i++) if(slot==slots[i]) { openCategory(p,i); return; }
            return;
        }
        if(holder.category()==-2) {
            Selection selected=selections.get(p.getUniqueId());
            if(selected==null) {openHome(p);return;}
            Material material=ITEMS[selected.category()][selected.index()];
            int maxStack=Math.max(1,material.getMaxStackSize());
            int step=maxStack>=64?64:maxStack>=16?16:1;
            if(slot==23) {selections.remove(p.getUniqueId());openCategory(p,selected.category());return;}
            if(slot==21) {buySelected(p,selected);return;}
            int delta=switch(slot) {
                case 9 -> -step;
                case 10 -> -10;
                case 11 -> -1;
                case 15 -> 1;
                case 16 -> 10;
                case 17 -> step;
                default -> 0;
            };
            if(delta!=0) refreshQuantity(p,selected,selected.quantity()+delta);
            return;
        }
        if(slot==18) { openHome(p); return; }
        int c=holder.category();
        if(c==4) {
            int i=slot-9;
            if(i<0 || i>=MOBS.length) return;
            int price=getConfig().getInt("spawners."+MOBS[i].name(),100);
            if(price<=0) {p.sendMessage("§cFiyat geçersiz.");return;}
            int balance=getPoints(p);
            if(balance<0) {p.sendMessage("§cPuan sistemi okunamadı.");return;}
            if(balance<price) {p.sendMessage("§cYeterli puanın yok.");return;}
            if(!takePoints(p,price)) {p.sendMessage("§cPuan ödemesi başarısız.");return;}
            give(p,spawner(i)); p.sendMessage("§a"+MOB_NAMES[i]+" Spawnerı satın alındı! §d-"+price+" puan");
            openCategory(p,c); return;
        }
        int i=slot-9;
        if(i<0 || i>=ITEMS[c].length) return;
        openQuantity(p,c,i,1);
    }
}
