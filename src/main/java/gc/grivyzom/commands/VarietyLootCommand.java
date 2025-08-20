package gc.grivyzom.commands;

import gc.grivyzom.VarietyMain;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VarietyLootCommand implements CommandExecutor {

    private final VarietyMain plugin;

    public VarietyLootCommand(VarietyMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Verificar permisos básicos
        if (!sender.hasPermission("varietyloot.use")) {
            if (sender instanceof Player) {
                plugin.getMessageManager().sendNoPermissionMessage((Player) sender);
            } else {
                sender.sendMessage(plugin.getMessageManager().getNoPermissionMessage());
            }
            return true;
        }

        // Si no hay argumentos, mostrar ayuda
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        // Manejar subcomandos
        switch (args[0].toLowerCase()) {
            case "help":
            case "ayuda":
                sendHelpMessage(sender);
                break;

            case "reload":
            case "recargar":
                if (!sender.hasPermission("varietyloot.reload")) {
                    if (sender instanceof Player) {
                        plugin.getMessageManager().sendNoPermissionMessage((Player) sender);
                    } else {
                        sender.sendMessage(plugin.getMessageManager().getNoPermissionMessage());
                    }
                    return true;
                }

                try {
                    plugin.getMessageManager().reloadMessages();

                    String successMessage = plugin.getMessageManager().getMessage("commands.reload.success");
                    sender.sendMessage(successMessage);
                } catch (Exception e) {
                    String errorMessage = plugin.getMessageManager().getMessage("commands.reload.error");
                    sender.sendMessage(errorMessage);
                    plugin.getLogger().severe("Error al recargar configuración: " + e.getMessage());
                }
                break;

            case "version":
            case "ver":
                String version = plugin.getDescription().getVersion();
                String author = plugin.getDescription().getAuthors().isEmpty() ?
                        "Desconocido" : plugin.getDescription().getAuthors().get(0);

                sender.sendMessage("§6VarietyLoot §7v" + version + " §fpor §b" + author);
                break;

            default:
                String unknownMessage = plugin.getMessageManager().getMessage("general.unknown-command");
                sender.sendMessage(unknownMessage);
                break;
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        String header = plugin.getMessageManager().getMessage("commands.help.header");
        String line1 = plugin.getMessageManager().getMessage("commands.help.line1");
        String line2 = plugin.getMessageManager().getMessage("commands.help.line2");
        String line3 = plugin.getMessageManager().getMessage("commands.help.line3");
        String footer = plugin.getMessageManager().getMessage("commands.help.footer");

        sender.sendMessage(header);
        sender.sendMessage(line1);
        sender.sendMessage(line2);
        sender.sendMessage(line3);
        sender.sendMessage(footer);
    }
}