package gc.grivyzom;

import gc.grivyzom.commands.VarietyLootCommand;
import gc.grivyzom.commands.VarietyLootTabCompleter;
import gc.grivyzom.items.ItemRegistry;
import gc.grivyzom.listeners.ItemEventListener;
import gc.grivyzom.mechanics.conditions.ConditionChecker;
import gc.grivyzom.mechanics.detection.ActionDetector;
import gc.grivyzom.mechanics.parser.MechanicsParser;
import gc.grivyzom.util.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VarietyMain extends JavaPlugin {

    private static VarietyMain instance;
    private MessageManager messageManager;
    private ItemRegistry itemRegistry;
    private ActionDetector actionDetector;
    private ConditionChecker conditionChecker;
    private ItemEventListener eventListener;

    @Override
    public void onEnable() {
        instance = this;

        // Inicializar gestores en orden de dependencia
        getLogger().info("Inicializando VarietyLoot...");

        try {
            // 1. Inicializar sistemas básicos
            messageManager = new MessageManager(this);
            itemRegistry = new ItemRegistry(this);

            // 2. Inicializar sistemas de mecánicas
            conditionChecker = new ConditionChecker(this);
            actionDetector = new ActionDetector(this, conditionChecker);

            // 3. Registrar listeners de eventos
            eventListener = new ItemEventListener(this, actionDetector);
            getServer().getPluginManager().registerEvents(eventListener, this);

            // 4. Registrar comandos
            registerCommands();

            // 5. Enviar mensaje de inicio
            messageManager.sendStartupMessage();

            // 6. Log de estadísticas
            logStartupStats();

            getLogger().info("¡VarietyLoot iniciado correctamente!");

        } catch (Exception e) {
            getLogger().severe("Error crítico al inicializar VarietyLoot: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Deshabilitando VarietyLoot...");

        try {
            // Enviar mensaje de cierre
            if (messageManager != null) {
                messageManager.sendShutdownMessage();
            }

            // Limpiar recursos en orden inverso de inicialización
            if (actionDetector != null) {
                actionDetector.shutdown();
                getLogger().info("ActionDetector limpiado");
            }

            if (conditionChecker != null) {
                conditionChecker.cleanupCache();
                getLogger().info("ConditionChecker limpiado");
            }

            if (itemRegistry != null) {
                getLogger().info("Guardando configuración de ítems...");
                itemRegistry.save();
            }

            getLogger().info("VarietyLoot deshabilitado correctamente");

        } catch (Exception e) {
            getLogger().severe("Error al deshabilitar VarietyLoot: " + e.getMessage());
        } finally {
            instance = null;
        }
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

    public ActionDetector getActionDetector() {
        return actionDetector;
    }

    public ConditionChecker getConditionChecker() {
        return conditionChecker;
    }

    private void registerCommands() {
        try {
            getCommand("varietyloot").setExecutor(new VarietyLootCommand(this));
            getCommand("varietyloot").setTabCompleter(new VarietyLootTabCompleter());
            getLogger().info("Comandos registrados correctamente");
        } catch (Exception e) {
            getLogger().severe("Error al registrar comandos: " + e.getMessage());
        }
    }

    /**
     * Recarga toda la configuración del plugin
     */
    public void reloadPlugin() {
        getLogger().info("Recargando VarietyLoot...");

        try {
            // Recargar mensajes
            messageManager.reloadMessages();

            // Limpiar cache de condiciones
            conditionChecker.cleanupCache();

            // Recargar ítems
            itemRegistry.reload();

            getLogger().info("VarietyLoot recargado correctamente");
            logReloadStats();

        } catch (Exception e) {
            getLogger().severe("Error al recargar VarietyLoot: " + e.getMessage());
            throw new RuntimeException("Error en la recarga", e);
        }
    }

    /**
     * Registra estadísticas de inicio en el log
     */
    private void logStartupStats() {
        getLogger().info("=== Estadísticas de Inicio ===");
        getLogger().info("- Ítems registrados: " + itemRegistry.getRegisteredItemIds().size());
        getLogger().info("- Condiciones personalizadas: " + conditionChecker.getStats().get("custom_conditions"));
        getLogger().info("- Listeners registrados: 1 (ItemEventListener)");
        getLogger().info("=============================");
    }

    /**
     * Registra estadísticas de recarga en el log
     */
    private void logReloadStats() {
        getLogger().info("=== Estadísticas de Recarga ===");
        getLogger().info("- Ítems cargados: " + itemRegistry.getRegisteredItemIds().size());
        getLogger().info("- Cache de condiciones limpiado");
        getLogger().info("- Tareas periódicas activas: " + actionDetector.getActivePeriodicTasks());
        getLogger().info("==============================");
    }

    /**
     * Obtiene información de estado del plugin para debugging
     */
    public String getStatusInfo() {
        StringBuilder status = new StringBuilder();
        status.append("=== Estado de VarietyLoot ===\n");
        status.append("Versión: ").append(getDescription().getVersion()).append("\n");
        status.append("Estado: ").append(isEnabled() ? "Habilitado" : "Deshabilitado").append("\n");

        if (itemRegistry != null) {
            status.append("Ítems registrados: ").append(itemRegistry.getRegisteredItemIds().size()).append("\n");
        }

        if (actionDetector != null) {
            status.append("Tareas periódicas: ").append(actionDetector.getActivePeriodicTasks()).append("\n");
            status.append("Cooldowns: ").append(actionDetector.getCooldownManager().getDebugInfo()).append("\n");
        }

        if (conditionChecker != null) {
            var conditionStats = conditionChecker.getStats();
            status.append("Condiciones cacheadas: ").append(conditionStats.get("cached_conditions")).append("\n");
            status.append("Condiciones personalizadas: ").append(conditionStats.get("custom_conditions")).append("\n");
        }

        status.append("============================");
        return status.toString();
    }

    /**
     * Verifica si el plugin está funcionando correctamente
     */
    public boolean isHealthy() {
        try {
            return isEnabled() &&
                    messageManager != null &&
                    itemRegistry != null &&
                    actionDetector != null &&
                    actionDetector.getCooldownManager().isHealthy() &&
                    conditionChecker != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Limpia recursos de un jugador específico (cuando se desconecta)
     */
    public void cleanupPlayer(org.bukkit.entity.Player player) {
        if (actionDetector != null) {
            actionDetector.cleanupPlayer(player);
        }

        if (conditionChecker != null) {
            conditionChecker.cleanupPlayer(player);
        }

        getLogger().fine("Recursos limpiados para el jugador: " + player.getName());
    }

    /**
     * Método de utilidad para obtener estadísticas completas
     */
    public java.util.Map<String, Object> getAllStats() {
        java.util.Map<String, Object> allStats = new java.util.HashMap<>();

        allStats.put("plugin_version", getDescription().getVersion());
        allStats.put("plugin_enabled", isEnabled());
        allStats.put("plugin_healthy", isHealthy());

        if (itemRegistry != null) {
            allStats.putAll(itemRegistry.getStats());
        }

        if (conditionChecker != null) {
            var conditionStats = conditionChecker.getStats();
            conditionStats.forEach((k, v) -> allStats.put("condition_" + k, v));
        }

        if (actionDetector != null) {
            allStats.put("periodic_tasks", actionDetector.getActivePeriodicTasks());
        }

        return allStats;
    }
}