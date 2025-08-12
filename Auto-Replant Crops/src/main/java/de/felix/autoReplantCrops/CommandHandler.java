package de.felix.autoReplantCrops;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandHandler implements CommandExecutor {

    private final AutoReplantCrops plugin;
    private final CropFieldManager fieldManager;

    public CommandHandler(AutoReplantCrops plugin, CropFieldManager fieldManager) {
        this.plugin = plugin;
        this.fieldManager = fieldManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("createfield")) {
            if (!sender.hasPermission("autoreplant.createfield")) {
                sender.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, diesen Befehl zu verwenden.");
                return true;
            }
            return fieldManager.handleCreateFieldCommand(sender, args);
        }

        if (label.equalsIgnoreCase("reloadfields")) {
            if (!sender.hasPermission("autoreplant.reloadfields")) {
                sender.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, diesen Befehl zu verwenden.");
                return true;
            }

            plugin.reloadConfig();
            fieldManager.loadFields();
            sender.sendMessage(ChatColor.GREEN + "Alle Felder wurden neu geladen.");
            return true;
        }

        if (label.equalsIgnoreCase("deletefield")) {
            if (!sender.hasPermission("autoreplant.deletefield")) {
                sender.sendMessage(ChatColor.RED + "Du hast keine Berechtigung, diesen Befehl zu verwenden.");
                return true;
            }

            if (args.length == 0) {
                // Kein Argument -> letztes Feld löschen
                String lastFieldId = fieldManager.getLastFieldId();
                if (lastFieldId == null) {
                    sender.sendMessage(ChatColor.RED + "Es existieren keine Felder zum Löschen.");
                    return true;
                }
                boolean success = fieldManager.deleteField(lastFieldId);
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "Letztes Feld (" + lastFieldId + ") wurde gelöscht.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Fehler beim Löschen des letzten Felds.");
                }
                return true;
            }

            if (args.length == 1) {
                String fieldId = args[0];
                if (!fieldManager.fields.containsKey(fieldId)) {
                    sender.sendMessage(ChatColor.RED + "Feld " + fieldId + " existiert nicht.");
                    return true;
                }
                boolean success = fieldManager.deleteField(fieldId);
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "Feld " + fieldId + " wurde gelöscht.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Fehler beim Löschen von Feld " + fieldId + ".");
                }
                return true;
            }

            sender.sendMessage(ChatColor.RED + "Nutzung: /deletefield [fieldId]");
            return true;
        }

        return false;
    }
}
