package gc.grivyzom.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VarietyLootTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> completions = new ArrayList<>();

        // Primera palabra (subcomandos principales)
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("help", "reload", "version", "give", "list");

            for (String subcommand : subcommands) {
                if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    // Verificar permisos para mostrar solo comandos disponibles
                    if (hasPermissionForSubcommand(sender, subcommand)) {
                        completions.add(subcommand);
                    }
                }
            }
        }

        return completions;
    }

    /**
     * Verifica si el sender tiene permisos para un subcomando específico
     */
    private boolean hasPermissionForSubcommand(CommandSender sender, String subcommand) {
        switch (subcommand.toLowerCase()) {
            case "reload":
                return sender.hasPermission("varietyloot.reload");
            case "give":
                return sender.hasPermission("varietyloot.give");
            case "help":
            case "version":
            case "list":
            default:
                return sender.hasPermission("varietyloot.use");
        }
    }
}