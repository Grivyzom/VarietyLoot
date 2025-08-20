package gc.grivyzom.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class MessageManager {

    private final JavaPlugin plugin;
    private File messagesFile;
    private FileConfiguration messagesConfig;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        createMessagesFile();
    }


    private void createMessagesFile() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Cargar los valores por defecto desde el archivo interno
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            messagesConfig.setDefaults(defaultConfig);
        }
    }


    public void reloadMessages() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            messagesConfig.setDefaults(defaultConfig);
        }
    }

    public String getMessage(String path) {
        String message = messagesConfig.getString(path, "&cMensaje no encontrado: " + path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String path, String... placeholders) {
        String message = getMessage(path);

        // Aplicar placeholders automáticos del plugin
        message = message.replace("{version}", plugin.getDescription().getVersion());
        message = message.replace("{author}", plugin.getDescription().getAuthors().isEmpty() ?
                "Desconocido" : plugin.getDescription().getAuthors().get(0));
        message = message.replace("{plugin}", plugin.getDescription().getName());

        // Aplicar placeholders personalizados
        if (placeholders.length % 2 == 0) {
            for (int i = 0; i < placeholders.length; i += 2) {
                message = message.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }

        return message;
    }


    public void sendMessage(Player player, String path, String... placeholders) {
        String message = getMessage(path, placeholders);
        if (!message.isEmpty()) {
            player.sendMessage(message);
        }
    }


    public void sendStartupMessage() {
        String message = getMessage("console.startup");
        plugin.getLogger().info(ChatColor.stripColor(message));
    }

    public void sendShutdownMessage() {
        String message = getMessage("console.shutdown");
        plugin.getLogger().info(ChatColor.stripColor(message));
    }


    public String getNoPermissionMessage() {
        return getMessage("general.no-permission");
    }


    public void sendNoPermissionMessage(Player player) {
        sendMessage(player, "general.no-permission");
    }


    public void saveMessages() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar el archivo messages.yml", e);
        }
    }


    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}