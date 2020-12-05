package me.ardimaster.guarddogs;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GDCommand implements CommandExecutor {
    private final GuardDogs plugin;

    public GDCommand(GuardDogs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (!sender.hasPermission("guarddogs.admin")) {
                sender.sendMessage(ChatColor.RED + "Sorry, but you don't have permission to use this command.");
                return true;
            }
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("version")) {
            if (plugin.getDescription().getVersion().equals(plugin.currentVersion)) {
                sender.sendMessage(ChatColor.GREEN + "You are using plugin " + ChatColor.DARK_GREEN +
                        "Guard Dogs" + ChatColor.GREEN + ", version " + ChatColor.AQUA +
                        plugin.getDescription().getVersion() + ChatColor.GREEN + ". No updates available.");
            } else if (plugin.currentVersion.equals("ERROR")) {
                sender.sendMessage(ChatColor.GREEN + "You are using plugin " + ChatColor.DARK_GREEN +
                        "Guard Dogs" + ChatColor.GREEN + ", version " + ChatColor.AQUA +
                        plugin.getDescription().getVersion() + ChatColor.GREEN + ". An " + ChatColor.RED + "Error" +
                        ChatColor.GREEN + " occured while trying to check for updates, please check manually " +
                        "at" + ChatColor.AQUA + "http://dev.bukkit.org/bukkit-plugins/guard-dogs");
            } else {
                sender.sendMessage(ChatColor.GREEN + "You are using plugin " + ChatColor.DARK_GREEN +
                        "Guard Dogs" + ChatColor.GREEN + ", version " + ChatColor.AQUA +
                        plugin.getDescription().getVersion() + ChatColor.GREEN + ". Version " + ChatColor.AQUA +
                        plugin.currentVersion + ChatColor.GREEN + " is available! Grab it at " + ChatColor.AQUA +
                        "http://dev.bukkit.org/bukkit-plugins/guard-dogs");
            }
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.saveGuards();
            plugin.loadGuards();
            sender.sendMessage("GuardDogs config reloaded.");
        }

        if (args.length == 0 || (args.length == 1 && args[0].equals("help"))) {
            sendHelp(sender);
            return true;
        }

        if (args.length == 1) {
            sender.sendMessage("[GuardDogs] All configuration is now done in the config file directly.");
            return true;
        }

        if (args.length == 2 && args[0].equals("enable")) {
            sender.sendMessage("[GuardDogs] All configuration is now done in the config file directly.");
            return true;
        }

        if (args.length == 2 && args[0].equals("disable")) {
            sender.sendMessage("[GuardDogs] All configuration is now done in the config file directly.");
            return true;
        }

        if (args.length == 3 && args[0].equals("materials")) {
            sender.sendMessage("[GuardDogs] All configuration is now done in the config file directly.");
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("max")) {
            sender.sendMessage("[GuardDogs] All configuration is now done in the config file directly.");
            return true;
        }
        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "/guarddogs version" + ChatColor.GOLD + " - displays version " +
                "information");
        sender.sendMessage(ChatColor.RED + "/guarddogs reload" + ChatColor.GOLD + " - reload config");
        sender.sendMessage("(All configuration is now done in the config file directly.)");
    }
}
