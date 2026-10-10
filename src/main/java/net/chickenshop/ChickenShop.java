package net.chickenshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class ChickenShop extends JavaPlugin implements Listener, CommandExecutor {
    private Economy economy;
    private record ShopHolder(UUID owner, int category) implements InventoryHolder {
        public Inventory getInventory() { return null; }
    }
    private static final String[] NAMES = {"SUNUCU MAĞAZASI", "END", "NETHER", "ALETLER", "KRİSTALLER"};
    private static final Material[] ICONS = {Material.GRASS_BLOCK, Material.END_STONE, Material.NETHERRACK, Material.DIAMOND_PICKAXE, Material.AMETHYST_SHARD};
    private static final EntityType[] MOBS = {EntityType.PIG, EntityType.COW, EntityType.ZOMBIE, EntityType.SPIDER, EntityType.SKELETON, EntityType.CREEPER, EntityType.ZOMBIFIED_PIGLIN, EntityType.BLAZE, EntityType.IRON_GOLEM};
    private static final String[] MOB_NAMES = {"Domuz", "İnek", "Zombi", "Örümcek", "İskelet", "Creeper", "Zombileşmiş Piglin", "Blaze", "Demir Golem"};
    private static final Material[][] ITEMS = {
        {Material.DIRT,Material.STONE,Material.OAK_LOG,Material.COBBLESTONE,Material.GLASS,Material.SAND,Material.OBSIDIAN},
        {Material.END_STONE,Material.ENDER_PEARL,Material.CHORUS_FRUIT,Material.PURPUR_BLOCK,Material.END_ROD,Material.SHULKER_SHELL,Material.DRAGON_BREATH},
        {Material.NETHERRACK,Material.SOUL_SAND,Material.QUARTZ,Material.GLOWSTONE,Material.BLAZE_ROD,Material.NETHER_WART,Material.ANCIENT_DEBRIS},
        {Material.IRON_PICKAXE,Material.DIAMOND_PICKAXE,Material.NETHERITE_PICKAXE,Material.DIAMOND_AXE,Material.DIAMOND_SHOVEL,Material.SHEARS,Material.FISHING_ROD}
    };
    private static final double[][] PRICES = {
        {10,20,50,15,30,15,200}, {15,100,30,60,150,1200,200},
        {10,25,45,80,200,50,3000}, {250,1800,12000,1500,750,100,150}
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
        Inventory inv = Bukkit.createInventory(new ShopHolder(p.getUniqueId(), -1), 45, "§8ChickenShop §7• §eMağaza");
        int[] slots = {11,12,13,14,15};
        for(int i=0;i<5;i++)
            inv.setItem(slots[i], button(ICONS[i], "§e§l"+NAMES[i], i==4 ? "§dPlayerPoints puanları" : "§6Dolar ile alışveriş", "§7Açmak için tıkla"));
        p.openInventory(inv);
    }
    private int mobSlot(int i) { return 19+i; }
    private void openCategory(Player p, int c) {
        Inventory inv = Bukkit.createInventory(new ShopHolder(p.getUniqueId(), c), 54, "§8ChickenShop §7• "+NAMES[c]);
        if(c==4) {
            inv.setItem(4,button(Material.AMETHYST_SHARD,"§dPuan bakiyesi","§f"+getPoints(p)+" puan"));
            for(int i=0;i<MOBS.length;i++) {
                int price = getConfig().getInt("spawners."+MOBS[i].name(), 100);
                inv.setItem(mobSlot(i),button(Material.SPAWNER,"§d"+MOB_NAMES[i]+" Spawnerı","§7Fiyat: §d"+price+" puan","§eSatın almak için tıkla"));
            }
            inv.setItem(49,button(Material.TRIPWIRE_HOOK,"§eKasa anahtarları","§7Kasalar hazır olunca eklenecek"));
        } else {
            for(int i=0;i<ITEMS[c].length;i++) {
                Material m=ITEMS[c][i];
                inv.setItem(19+i,button(m,"§f"+m.name().replace('_',' '),"§7Fiyat: §6$"+PRICES[c][i],"§eSatın almak için tıkla"));
            }
        }
        inv.setItem(53,button(Material.ARROW,"§eGeri"));
        p.openInventory(inv);
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
        item.setItemMeta(meta);
        return item;
    }
    private void give(Player p,ItemStack item) {
        p.getInventory().addItem(item).values().forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(),left));
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
        if(slot==53) { openHome(p); return; }
        int c=holder.category();
        if(c==4) {
            int i=slot-19;
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
        int i=slot-19;
        if(i<0 || i>=ITEMS[c].length) return;
        double price=PRICES[c][i];
        if(!economy.has(p,price)) {p.sendMessage("§cYeterli paran yok.");return;}
        if(!economy.withdrawPlayer(p,price).transactionSuccess()) {p.sendMessage("§cÖdeme başarısız.");return;}
        give(p,new ItemStack(ITEMS[c][i]));
        p.sendMessage("§aSatın alındı! §6-$"+price);
    }
}
