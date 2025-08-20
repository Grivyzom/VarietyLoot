package gc.grivyzom.commands;

import gc.grivyzom.VarietyMain;
import gc.grivyzom.items.ItemDefinition;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

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
                    sendNoPermission(sender);
                    return true;
                }

                try {
                    plugin.reloadPlugin();
                    String successMessage = plugin.getMessageManager().getMessage("commands.reload.success");
                    sender.sendMessage(successMessage);
                } catch (Exception e) {
                    String errorMessage = plugin.getMessageManager().getMessage("commands.reload.error");
                    sender.sendMessage(errorMessage);
                    plugin.getLogger().severe("Error al recargar configuración: " + e.getMessage());
                }
                break;

            case "give":
            case "dar":
                handleGiveCommand(sender, args);
                break;

            case "list":
            case "lista":
                handleListCommand(sender);
                break;

            case "info":
                handleInfoCommand(sender, args);
                break;

            case "stats":
            case "estadisticas":
                handleStatsCommand(sender);
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

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("varietyloot.give")) {
            sendNoPermission(sender);
            return;
        }

        if (args.length < 2) {
            String usage = plugin.getMessageManager().getMessage("commands.give.usage");
            sender.sendMessage(usage);
            return;
        }

        String itemId = args[1];
        int amount = 1;
        Player targetPlayer = null;

        // Parsear cantidad si se proporciona
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0 || amount > 64) {
                    sender.sendMessage("§c✘ La cantidad debe estar entre 1 y 64");
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c✘ Cantidad inválida: " + args[2]);
                return;
            }
        }

        // Parsear jugador objetivo si se proporciona
        if (args.length >= 4) {
            targetPlayer = Bukkit.getPlayer(args[3]);
            if (targetPlayer == null) {
                String message = plugin.getMessageManager().getMessage("errors.invalid-player", "player", args[3]);
                sender.sendMessage(message);
                return;
            }
        } else if (sender instanceof Player) {
            targetPlayer = (Player) sender;
        }

        if (targetPlayer == null) {
            sender.sendMessage("§c✘ Debes especificar un jugador desde la consola");
            return;
        }

        // Verificar si el ítem existe
        ItemDefinition definition = plugin.getItemRegistry().getItemDefinition(itemId);
        if (definition == null) {
            String message = plugin.getMessageManager().getMessage("commands.give.item-not-found", "item", itemId);
            sender.sendMessage(message);
            return;
        }

        // Crear y dar el ítem
        ItemStack itemStack = plugin.getItemRegistry().createItemStack(itemId, amount);
        if (itemStack == null) {
            sender.sendMessage("§c✘ Error al crear el ítem");
            return;
        }

        targetPlayer.getInventory().addItem(itemStack);

        String successMessage = plugin.getMessageManager().getMessage("commands.give.success", "item", definition.getDisplayName());
        targetPlayer.sendMessage(successMessage);

        if (!sender.equals(targetPlayer)) {
            sender.sendMessage("§a✓ Ítem " + definition.getDisplayName() + " §agiven to " + targetPlayer.getName());
        }
    }

    private void handleListCommand(CommandSender sender) {
        String header = plugin.getMessageManager().getMessage("commands.list.header");
        sender.sendMessage(header);

        var definitions = plugin.getItemRegistry().getAllDefinitions();
        if (definitions.isEmpty()) {
            String empty = plugin.getMessageManager().getMessage("commands.list.empty");
            sender.sendMessage(empty);
        } else {
            for (ItemDefinition def : definitions) {
                String description = def.getLore().isEmpty() ? "Sin descripción" : def.getLore().get(0);
                String format = plugin.getMessageManager().getMessage("commands.list.format",
                        "item", def.getId(), "description", description);
                sender.sendMessage(format);
            }
        }

        String footer = plugin.getMessageManager().getMessage("commands.list.footer");
        sender.sendMessage(footer);
    }

    private void handleInfoCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§e! Uso: /varietyloot info <ítem>");
            return;
        }

        String itemId = args[1];
        ItemDefinition definition = plugin.getItemRegistry().getItemDefinition(itemId);

        if (definition == null) {
            String message = plugin.getMessageManager().getMessage("commands.give.item-not-found", "item", itemId);
            sender.sendMessage(message);
            return;
        }

        sender.sendMessage("§6§m              §r §6Información del Ítem §6§m              ");
        sender.sendMessage("§eID: §f" + definition.getId());
        sender.sendMessage("§eNombre: " + definition.getDisplayName());
        sender.sendMessage("§eMaterial: §f" + definition.getMaterial());
        sender.sendMessage("§eApilable: §f" + (definition.isStackable() ? "Sí" : "No"));
        sender.sendMessage("§eConsumible: §f" + (definition.isConsumable() ? "Sí" : "No"));
        sender.sendMessage("§eCooldown: §f" + definition.getCooldown() + "s");
        sender.sendMessage("§eMecánicas: §f" + (definition.hasMechanics() ? "Sí" : "No"));
        if (definition.getPermission() != null) {
            sender.sendMessage("§ePermiso: §f" + definition.getPermission());
        }
        sender.sendMessage("§6§m                                                    ");
    }

    private void handleStatsCommand(CommandSender sender) {
        Map<String, Object> stats = plugin.getItemRegistry().getStats();

        sender.sendMessage("§6§m              §r §6Estadísticas VarietyLoot §6§m              ");
        sender.sendMessage("§eÍtems totales: §f" + stats.get("total_items"));
        sender.sendMessage("§eÍtems con mecánicas: §f" + stats.get("items_with_mechanics"));
        sender.sendMessage("§eÍtems consumibles: §f" + stats.get("consumable_items"));
        sender.sendMessage("§eÍtems con cooldown: §f" + stats.get("items_with_cooldown"));
        sender.sendMessage("§6§m                                                      ");
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
        sender.sendMessage("§e/varietyloot info <ítem> §7- §fVer información de un ítem");
        sender.sendMessage("§e/varietyloot stats §7- §fVer estadísticas del plugin");
        sender.sendMessage(footer);
    }

    private void sendNoPermission(CommandSender sender) {
        if (sender instanceof Player) {
            plugin.getMessageManager().sendNoPermissionMessage((Player) sender);
        } else {
            sender.sendMessage(plugin.getMessageManager().getNoPermissionMessage());
        }
    }
}