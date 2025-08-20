package gc.grivyzom;

import gc.grivyzom.commands.VarietyLootCommand;
import gc.grivyzom.commands.VarietyLootTabCompleter;
import gc.grivyzom.items.ItemRegistry;
import gc.grivyzom.util.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VarietyMain extends JavaPlugin {

    private static VarietyMain instance;
    private MessageManager messageManager;
    private ItemRegistry itemRegistry;

    @Override
    public void onEnable() {
        instance = this;

        // Inicializar gestores
        messageManager = new MessageManager(this);
        itemRegistry = new ItemRegistry(this);

        // Enviar mensaje de inicio
        messageManager.sendStartupMessage();

        // Registrar comandos
        registerCommands();

        // Log de estadísticas de ítems
        getLogger().info("Sistema de ítems inicializado:");
        getLogger().info("- Ítems registrados: " + itemRegistry.getRegisteredItemIds().size());

        getLogger().info("¡VarietyLoot iniciado correctamente!");
    }

    @Override
    public void onDisable() {
        // Enviar mensaje de cierre
        if (messageManager != null) {
            messageManager.sendShutdownMessage();
        }

        // Limpiar recursos
        if (itemRegistry != null) {
            getLogger().info("Guardando configuración de ítems...");
            itemRegistry.save();
        }

        instance = null;
    }

    public static VarietyMain getInstance() {
        return instance;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public ItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    private void registerCommands() {
        getCommand("varietyloot").setExecutor(new VarietyLootCommand(this));
        getCommand("varietyloot").setTabCompleter(new VarietyLootTabCompleter());
    }

    /**
     * Recarga toda la configuración del plugin
     */
    public void reloadPlugin() {
        getLogger().info("Recargando VarietyLoot...");

        // Recargar mensajes
        messageManager.reloadMessages();

        // Recargar ítems
        itemRegistry.reload();

        getLogger().info("VarietyLoot recargado correctamente");
        getLogger().info("- Ítems cargados: " + itemRegistry.getRegisteredItemIds().size());
    }
}