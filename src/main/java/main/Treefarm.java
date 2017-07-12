package main;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sun.org.apache.regexp.internal.RE;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import sun.reflect.generics.tree.Tree;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * Created by Cory on 12/03/2017.
 */
public class Treefarm extends JavaPlugin implements Listener {

    private WorldEditPlugin worldEditPlugin = null;
    private YamlConfiguration yamlConfiguration;
    private File f = new File(this.getDataFolder(), "treefarms.yml");
    private final HashMap<String, RegionWrapper> treeMap = new HashMap<String, RegionWrapper>();
    private final Random random = new Random();

    @Override
    public void onEnable() {
        worldEditPlugin = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        if(worldEditPlugin == null){
            System.out.println("TREEFARM: I NEED WORLDEDIT!");
            this.getPluginLoader().disablePlugin(this);
        }
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getDataFolder().mkdirs();
        if(!f.exists()) {
            try {
                f.createNewFile();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        yamlConfiguration = YamlConfiguration.loadConfiguration(f);

        for(String s : yamlConfiguration.getKeys(false)) {
            ConfigurationSection conf = yamlConfiguration.getConfigurationSection(s);
            RegionWrapper regionWrapper = new RegionWrapper(new Location(Bukkit.getWorld(conf.getString("world")), conf.getInt("minX"),
                    conf.getInt("minY"), conf.getInt("minZ")),
                    new Location(Bukkit.getWorld(conf.getString("world")), conf.getInt("maxX"),
                            conf.getInt("maxY"), conf.getInt("maxZ")));
            treeMap.put(s, regionWrapper);
        }
    }

    private void save() {
        try {
            this.yamlConfiguration.save(f);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void addTreeFarm(String name, Location loc1, Location loc2) {
        int minX, maxX, minY, maxY, minZ, maxZ;
        minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        yamlConfiguration.set(name + ".world", loc1.getWorld().getName());
        yamlConfiguration.set(name + ".minX", minX);
        yamlConfiguration.set(name + ".minY", minY);
        yamlConfiguration.set(name + ".minZ", minZ);
        yamlConfiguration.set(name + ".maxX", maxX);
        yamlConfiguration.set(name + ".maxY", maxY);
        yamlConfiguration.set(name + ".maxZ", maxZ);

        this.treeMap.put(name, new RegionWrapper(loc1, loc2));
        this.save();
    }

    public boolean farmExists(String farm) {
        Iterator iterator = this.treeMap.keySet().iterator();
        while(iterator.hasNext()) {
            String s = (String) iterator.next();
            if(farm.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    public String listFarms() {
        StringBuilder sb = new StringBuilder();
        for(String s : this.treeMap.keySet()) {
            sb.append(s + ", ");
        }
        if(sb.length() == 0) return "No treefarms active.";
        sb.substring(0, sb.length()-3);
        return sb.toString();
    }

    public boolean deleteFarm(String name) {
        boolean found = false;
        Iterator iterator = this.treeMap.keySet().iterator();
        while(iterator.hasNext()) {
            String s = (String) iterator.next();
            if(s.equalsIgnoreCase(name)) {
                found = true;
                iterator.remove();
                yamlConfiguration.set(s, null);
                save();
            }
        }
        return found;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Location loc = block.getLocation();
        if(block.getType() == Material.LOG) {
            if(isInTreeFarm(loc)) {
                 if(e.getBlock().getType() == Material.LOG) {
                    int rdm = random.nextInt(150);
                    if(rdm <= 10) {
                        e.getBlock().getWorld().dropItem(loc, new ItemStack(Material.COAL, 1));
                    } else if(rdm >= 145) {
                        e.getBlock().getWorld().dropItem(loc, new ItemStack(Material.IRON_INGOT, 1));
                    } else if(rdm == 100) {
                        int rdm1 = random.nextInt(1);
                        if(rdm1 == 1) {
                            e.getBlock().getWorld().dropItem(loc, new ItemStack(Material.DIAMOND, 1));
                        }
                    }
                 }
                 if(new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY()-1, loc.getBlockZ()).getBlock().getType() == Material.DIRT) {
                    e.setCancelled(true);
                    loc.getBlock().setType(Material.SAPLING);
                    loc.getBlock().setData(TreeSpecies.BIRCH.getData());
                    ItemStack item = new ItemStack(Material.LOG);
                    item.setDurability((short)2);
                    loc.getWorld().dropItem(loc, item);
                }
            }
        } else if(block.getType() == Material.SAPLING) {
            if(isInTreeFarm(loc)) {
                e.getPlayer().sendMessage(ChatColor.GREEN + "No breaking saplings in the treefarm!");
                e.setCancelled(true);
            }
        }
    }

    public boolean isInTreeFarm(Location loc) {
        for(RegionWrapper region : treeMap.values()) {
            if(contains(loc, region)) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(Location loc, RegionWrapper region) {
        Location l1 = region.getL1();
        Location l2 = region.getL2();

        int x1 = l1.getBlockX();
        int y1 = l1.getBlockY();
        int z1 = l1.getBlockZ();
        int x2 = l2.getBlockX();
        int y2 = l2.getBlockY();
        int z2 = l2.getBlockZ();

        return loc.getBlockX() >= l1.getBlockX() && loc.getBlockX() <= l2.getBlockX()
                && loc.getBlockY() >= l1.getBlockY() && loc.getBlockY() <= l2.getBlockY()
                && loc.getBlockZ() >= l1.getBlockZ() && loc.getBlockZ() <= l2.getBlockZ();
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if(!s.isOp()) return false;
        if(label.equalsIgnoreCase("treefarm")) {
            if(args.length < 1 || args.length > 2) {
                s.sendMessage(helpMenu());
            } else if(args.length == 1) {
                if(args[0].equalsIgnoreCase("list")) {
                    s.sendMessage(ChatColor.GREEN + this.listFarms());
                } else {
                    s.sendMessage(helpMenu());
                }
            } else if(args.length == 2) {
                if(args[0].equalsIgnoreCase("create")) {
                    String farm = args[1];
                    if(farmExists(farm)) {
                        s.sendMessage(ChatColor.GREEN + "Treefarm '" + farm + "' already exists.");
                        return false;
                    }
                    Selection selection = this.worldEditPlugin.getSelection((Player)s);
                    if(selection != null) {
                        this.addTreeFarm(farm, selection.getMinimumPoint(), selection.getMaximumPoint());
                        s.sendMessage(ChatColor.GREEN + "Successfully added treefarm '" + farm + "'!");
                    } else {
                        s.sendMessage(ChatColor.GREEN + "You must have a first and second WorldEdit selection!");
                    }
                } else if(args[0].equalsIgnoreCase("del")) {
                    String farm = args[1];
                    if(!deleteFarm(farm)) {
                        s.sendMessage(ChatColor.GREEN + "Treefarm '" + farm + "' not found. /treefarm list");
                    } else {
                        s.sendMessage(ChatColor.GREEN + "Treefarm '" + farm + "' successfully removed.");
                    }
                } else {
                    s.sendMessage(helpMenu());
                }
            }
        }
        return false;
    }

    public String helpMenu() {
        return "=========[&2Treefarm&f]=========\n".replace('&', '§') +
                "&a/treefarm create <name>\n".replace('&', '§') +
                "/treefarm list\n".replace('&', '§') +
                "/treefarm del <name>&f\n".replace('&', '§') +
                "=========[&2Treefarm&f]=========".replace('&', '§');

    }
}
class RegionWrapper {

    private final Location l1, l2;

    public RegionWrapper(Location l1, Location l2) {
        this.l1 = l1;
        this.l2 = l2;
    }

    public Location getL1() {
        return this.l1;
    }

    public Location getL2() {
         return this.l2;
    }
}
