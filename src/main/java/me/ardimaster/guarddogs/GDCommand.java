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

        if (args.length == 0 || (args.length == 1 && args[0].equals("help"))) {
            sendHelp(sender);
            return true;
        }

        if (args.length == 1) {
            switch (args[0]) {
                case "materials":
                    sender.sendMessage(ChatColor.RED + "/guarddogs materials [create|disable|ignore] [Material] " +
                            ChatColor.GOLD + "- set the material required to perform the specified guard dog action");
                    sender.sendMessage(ChatColor.RED + "/guarddogs materials [extradamage|ignitechance|teleport] " +
                            "[Material]" + ChatColor.GOLD + "- set the material required for the specified guard " +
                            "dog special ability");
                    break;
                case "enable":
                    sender.sendMessage(ChatColor.RED + "/guarddogs enable notifyupdates" + ChatColor.GOLD +
                            " - enables the on-join update alert for admins");
                    sender.sendMessage(ChatColor.RED + "/guarddogs enable [extradamage|ignitechance|teleport]" +
                            ChatColor.GOLD + " - enables the specified guard dog special ability");
                    break;
                case "disable":
                    sender.sendMessage(ChatColor.RED + "/guarddogs disable notifyupdates" + ChatColor.GOLD +
                            " - disables the on-join update alert for admins");
                    sender.sendMessage(ChatColor.RED + "/guarddogs disable [extradamage|ignitechance|teleport]" +
                            ChatColor.GOLD + " - disables the specified guard dog special ability");
                    break;
                default:
                    sendHelp(sender);
                    break;
            }
            return true;
        }

        if (args.length == 2 && args[0].equals("enable")) {
            switch (args[1]) {
                case "extradamage":
                    plugin.extraDamage = true;
                    sender.sendMessage(ChatColor.DARK_GREEN + "Guard Dog" + ChatColor.GREEN + "s can now deal " +
                            "extra damage.");
                    break;
                case "ignitechance":
                    plugin.igniteChance = true;
                    sender.sendMessage(ChatColor.DARK_GREEN + "Guard Dog" + ChatColor.GREEN + "s can now have a" +
                            "chance to ignite their targets upon attack.");
                    break;
                case "teleport":
                    plugin.teleport = true;
                    sender.sendMessage(ChatColor.DARK_GREEN + "Gaurd Dog" + ChatColor.GREEN + "s can now teleport" +
                            "home when they are low on health");
                    break;
                case "notifyupdates":
                    plugin.notifyUpdates = true;
                    sender.sendMessage(ChatColor.GREEN + "Admins will now be notified about available updates " +
                            "on join.");
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "/guarddogs enable notifyupdates" + ChatColor.GOLD +
                            " - enables the on-join update alert for admins");
                    sender.sendMessage(ChatColor.RED + "/guarddogs enable [extradamage|ignitechance|teleport]" +
                            ChatColor.GOLD + " - enables the specified guard dog special ability");
            }
        }

        if (args.length == 2 && args[0].equals("disable")) {
            switch (args[1]) {
                case "extradamage":
                    plugin.extraDamage = false;
                    sender.sendMessage(ChatColor.DARK_GREEN + "Guard Dog" + ChatColor.GREEN + "s can no longer " +
                            "deal extra damage.");
                    break;
                case "ignitechance":
                    plugin.igniteChance = false;
                    sender.sendMessage(ChatColor.DARK_GREEN + "Guard Dog" + ChatColor.GREEN + "s can no longer " +
                            "have a chance to ignite their targets upon attack.");
                    break;
                case "teleport":
                    plugin.teleport = false;
                    sender.sendMessage(ChatColor.DARK_GREEN + "Gaurd Dog" + ChatColor.GREEN + "s can now teleport" +
                            "home when they are low on health");
                    break;
                case "notifyupdates":
                    plugin.notifyUpdates = false;
                    sender.sendMessage(ChatColor.GREEN + "Admins will now be notified about available updates " +
                            "on join.");
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "/guarddogs disable notifyupdates" + ChatColor.GOLD +
                            " - enables the on-join update alert for admins");
                    sender.sendMessage(ChatColor.RED + "/guarddogs disable [extradamage|ignitechance|teleport]" +
                            ChatColor.GOLD + " - disable the specified guard dog special ability");
            }
        }

        if (args.length == 3 && args[0].equals("materials")) {
            Material tmp = Material.getMaterial(args[2]);
            switch (args[1]) {
                case "create":
                    if (tmp != null) {
                        plugin.createMat = tmp;
                        sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a tamed " +
                                "wolf with " + ChatColor.AQUA + args[2] + ChatColor.GREEN + " to make it a guard dog.");
                    } else {
                        sender.sendMessage(ChatColor.RED + args[2] + " is not a valid material!");
                    }
                    break;
                case "disable":
                    if (tmp != null) {
                        plugin.disableMat = tmp;
                        sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a guard " +
                                "dog with " + ChatColor.AQUA + args[2] + ChatColor.GREEN + " to disable it.");
                    } else {
                        sender.sendMessage(ChatColor.RED + args[2] + " is not a valid material!");
                    }
                    break;
                case "ignore":
                    if (tmp != null) {
                        plugin.ignoreMat = tmp;
                        sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a guard " +
                                "dog with " + ChatColor.AQUA + args[2] + ChatColor.GREEN + " to make it ignore " +
                                "a player.");
                    } else {
                        sender.sendMessage(ChatColor.RED + args[2] + " is not a valid material!");
                    }
                    break;
                case "extradamage":
                    if (tmp != null) {
                        plugin.extraDamageMat = tmp;
                        sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a guard " +
                                "dog with " + ChatColor.AQUA + args[2] + ChatColor.GREEN + " to make it deal " +
                                "extra damage.");
                    } else {
                        sender.sendMessage(ChatColor.RED + args[2] + " is not a valid material!");
                    }
                    break;
                case "ignitechance":
                    if (tmp != null) {
                        plugin.igniteChanceMat = tmp;
                        sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a guard " +
                                "dog with " + ChatColor.AQUA + args[2] + ChatColor.GREEN + " to increase its " +
                                "chance of ignote its enemies.");
                    } else {
                        sender.sendMessage(ChatColor.RED + args[2] + " is not a valid material!");
                    }
                    break;
                case "teleport":
                    if (tmp != null) {
                        plugin.teleportMat = tmp;
                        sender.sendMessage(ChatColor.GREEN + "Material changed. From now on, right click a guard " +
                                "dog with " + ChatColor.AQUA + args[2] + ChatColor.GREEN + " to allow it to " +
                                "teleport to its home location when it's low on health.");
                    } else {
                        sender.sendMessage(ChatColor.RED + args[2] + " is not a valid material");
                    }
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "/guarddogs materials [create|disable|ignore] [Material] " +
                            ChatColor.GOLD + "- set the material required to perform the specified guard dog action");
                    sender.sendMessage(ChatColor.RED + "/guarddogs materials [extradamage|ignitechance|teleport] " +
                            "[Material]" + ChatColor.GOLD + "- set the material required for the specified guard " +
                            "dog special ability");
                    break;
            }
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("max")) {
            switch (args[1]) {
                case "extradamage":
                    plugin.extraDamageMax = Integer.parseInt(args[2]);
                    sender.sendMessage(ChatColor.GREEN + "Guard Dogs on this server will from now on be able to " +
                            "deal up to " + args[2] + " extra damage. Existing higher values have not been reset.");
                    break;
                case "ignitechance":
                    if (Integer.parseInt(args[2]) <= 10) {
                        plugin.igniteChanceMax = Integer.parseInt(args[2]);
                        sender.sendMessage(ChatColor.GREEN + "Guard Dogs on this server will from now on be able to " +
                                "have a maximum chance of " + args[2] + "0% to set their enemies on fire. Existing " +
                                "higher values have not been reset.");
                    } else {
                        sender.sendMessage(ChatColor.DARK_AQUA  + "ignitechance" + ChatColor.RED + " is counted " +
                                "in setps of 10%, so 10 (100%) is maximum.");
                    }
                    break;
                case "teleport":
                    plugin.teleportMax = Integer.parseInt(args[2]);
                    sender.sendMessage(ChatColor.GREEN + "Guard Dogs on this server will from now on be able to " +
                            "teleport home when lowon health for up to " + args[2] + " times. Existing higher " +
                            "values have not been reset.");
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "/guarddogs max [extradamage|ignitechance|teleport] " +
                            "[maximum]" + ChatColor.GOLD + " - set the maximum for the specified guard dog action.");
                    sender.sendMessage(ChatColor.DARK_AQUA + "extradamage" + ChatColor.GOLD + " - sets the " +
                            "maximum amount of extra damage a guard dog can deal. Counted if half-hearts.");
                    sender.sendMessage(ChatColor.DARK_AQUA + "ignitechance" + ChatColor.GOLD + " - sets the " +
                            "maximum chance of setting an enemy on fire that a guard dog can have. Counted in " +
                            "10%, maximum is 10 (100%).");
                    sender.sendMessage(ChatColor.DARK_AQUA + "teleport" + ChatColor.GOLD + " - sets the maximum " +
                            "amount of times a guard dog can teleport home when low on health.");
                    break;
            }

            return true;
        }
        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "/guarddogs version" + ChatColor.GOLD + " - displays version " +
                "information");
        sender.sendMessage(ChatColor.RED + "/guarddogs materials [create|disable|ignore] [Material]" +
                ChatColor.GOLD + " - set the material required to perform the specified guard dog action");
        sender.sendMessage(ChatColor.RED + "/guarddogs materials [extradamage|ignitechance|teleport] " +
                "[Material]" + ChatColor.GOLD + " - set the material required for the specified guard " +
                "dog special ability");
        sender.sendMessage(ChatColor.RED + "/guarddogs [enable|disable] notifyupdates" + ChatColor.GOLD +
                " - enables or disables the on-join update alert for admins");
        sender.sendMessage(ChatColor.RED + "/guarddogs [enable|disable] [extradamage|ignitechance|teleport]" +
                ChatColor.GOLD + " - enables or disables the specified guard dog special ability");
        sender.sendMessage(ChatColor.RED + "/guarddogs max [extradamage|ignitechance|teleport] [maximum]" +
                ChatColor.GOLD + " - sets the maximum for the specified guard dog special ability.");
    }
}
