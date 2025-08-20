package gc.grivyzom.mechanics.detection;

import gc.grivyzom.VarietyMain;
import gc.grivyzom.items.ItemDefinition;
import gc.grivyzom.mechanics.TriggerType;
import gc.grivyzom.mechanics.actions.Action;
import gc.grivyzom.mechanics.actions.ActionContext;
import gc.grivyzom.util.Cooldowns;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Sistema central de detección y ejecución de acciones para ítems personalizados
 * Maneja la lógica de cuando y como ejecutar las acciones basadas en triggers
 */
public class ActionDetector {

    private final VarietyMain plugin;
    private final Cooldowns cooldownManager;

    // Cache para optimizar verificaciones frecuentes
    private final ConcurrentHashMap<String, Boolean> triggerCache;

    // Control de ejecución para triggers periódicos
    private final ConcurrentHashMap<String, BukkitRunnable> periodicTasks;

    public ActionDetector(VarietyMain plugin) {
        this.plugin = plugin;
        this.cooldownManager = new Cooldowns();
        this.triggerCache = new ConcurrentHashMap<>();
        this.periodicTasks = new ConcurrentHashMap<>();
    }

    /**
     * Detecta y ejecuta acciones para un trigger específico
     * @param player El jugador que ejecuta la acción
     * @param itemStack El ítem involucrado
     * @param trigger El tipo de trigger activado
     * @param context Contexto adicional para la acción
     * @return true si se ejecutaron acciones, false en caso contrario
     */
    public boolean detectAndExecute(Player player, ItemStack itemStack, TriggerType trigger, ActionContext context) {
        // Validaciones básicas
        if (!isValidExecution(player, itemStack, trigger)) {
            return false;
        }

        // Obtener definición del ítem
        ItemDefinition definition = plugin.getItemRegistry().getDefinitionFromItemStack(itemStack);
        if (definition == null) {
            return false;
        }

        // Verificar si el ítem tiene este trigger
        if (!definition.hasTrigger(trigger)) {
            return false;
        }

        // Verificar cooldown
        if (!checkCooldown(player, definition, trigger)) {
            return false;
        }

        // Verificar permisos
        if (!checkPermissions(player, definition)) {
            return false;
        }

        // Ejecutar acciones
        return executeActions(player, definition, trigger, context);
    }

    /**
     * Versión simplificada para triggers básicos
     */
    public boolean detectAndExecute(Player player, ItemStack itemStack, TriggerType trigger) {
        ItemDefinition definition = plugin.getItemRegistry().getDefinitionFromItemStack(itemStack);
        if (definition == null) {
            return false;
        }

        ActionContext context = new ActionContext.Builder(player, definition, trigger)
                .itemStack(itemStack)
                .build();

        return detectAndExecute(player, itemStack, trigger, context);
    }

    /**
     * Inicia el monitoreo de triggers periódicos para un jugador
     */
    public void startPeriodicMonitoring(Player player, ItemStack itemStack) {
        ItemDefinition definition = plugin.getItemRegistry().getDefinitionFromItemStack(itemStack);
        if (definition == null) {
            return;
        }

        String taskKey = generateTaskKey(player, definition);

        // Verificar si ya hay una tarea corriendo
        if (periodicTasks.containsKey(taskKey)) {
            return;
        }

        // Crear tarea para WHILE_HELD
        if (definition.hasTrigger(TriggerType.WHILE_HELD)) {
            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    // Verificar si el jugador sigue teniendo el ítem en la mano
                    ItemStack currentItem = player.getInventory().getItemInMainHand();
                    if (!plugin.getItemRegistry().getItemFactory().matches(currentItem, definition)) {
                        // El jugador ya no tiene el ítem, cancelar tarea
                        this.cancel();
                        periodicTasks.remove(taskKey);
                        return;
                    }

                    // Ejecutar acciones de WHILE_HELD
                    ActionContext context = new ActionContext.Builder(player, definition, TriggerType.WHILE_HELD)
                            .itemStack(currentItem)
                            .build();

                    executeActions(player, definition, TriggerType.WHILE_HELD, context);
                }
            };

            task.runTaskTimer(plugin, 20L, 20L); // Cada segundo
            periodicTasks.put(taskKey, task);
        }

        // Crear tarea para PERIODIC
        if (definition.hasTrigger(TriggerType.PERIODIC)) {
            BukkitRunnable task = new BukkitRunnable() {
                @Override
                public void run() {
                    ActionContext context = new ActionContext.Builder(player, definition, TriggerType.PERIODIC)
                            .itemStack(itemStack)
                            .build();

                    executeActions(player, definition, TriggerType.PERIODIC, context);
                }
            };

            // Intervalo basado en el cooldown del ítem (mínimo 5 segundos)
            long interval = Math.max(definition.getCooldown() * 20L, 100L);
            task.runTaskTimer(plugin, interval, interval);
            periodicTasks.put(taskKey + "_periodic", task);
        }
    }

    /**
     * Detiene el monitoreo periódico para un jugador e ítem específico
     */
    public void stopPeriodicMonitoring(Player player, ItemDefinition definition) {
        String taskKey = generateTaskKey(player, definition);

        // Cancelar tarea WHILE_HELD
        BukkitRunnable task = periodicTasks.remove(taskKey);
        if (task != null) {
            task.cancel();
        }

        // Cancelar tarea PERIODIC
        task = periodicTasks.remove(taskKey + "_periodic");
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Limpia todas las tareas periódicas de un jugador (cuando se desconecta)
     */
    public void cleanupPlayer(Player player) {
        String playerKey = player.getUniqueId().toString();

        periodicTasks.entrySet().removeIf(entry -> {
            if (entry.getKey().contains(playerKey)) {
                entry.getValue().cancel();
                return true;
            }
            return false;
        });

        cooldownManager.cleanupPlayer(player);
    }

    /**
     * Valida si la ejecución es posible
     */
    private boolean isValidExecution(Player player, ItemStack itemStack, TriggerType trigger) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        if (itemStack == null || itemStack.getType().isAir()) {
            return false;
        }

        if (trigger == null) {
            return false;
        }

        // Verificar si el trigger requiere que el ítem esté en la mano
        if (trigger.requiresInHand()) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();

            if (!itemStack.equals(mainHand) && !itemStack.equals(offHand)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Verifica el cooldown del ítem
     */
    private boolean checkCooldown(Player player, ItemDefinition definition, TriggerType trigger) {
        if (definition.getCooldown() <= 0) {
            return true; // Sin cooldown
        }

        String cooldownKey = generateCooldownKey(player, definition, trigger);

        if (cooldownManager.isOnCooldown(cooldownKey)) {
            long remaining = cooldownManager.getRemainingTime(cooldownKey);
            plugin.getMessageManager().sendMessage(player, "items.cooldown", "time", String.valueOf(remaining));
            return false;
        }

        return true;
    }

    /**
     * Verifica los permisos del jugador para usar el ítem
     */
    private boolean checkPermissions(Player player, ItemDefinition definition) {
        if (definition.getPermission() == null || definition.getPermission().isEmpty()) {
            return true; // Sin permiso requerido
        }

        if (!player.hasPermission(definition.getPermission())) {
            plugin.getMessageManager().sendNoPermissionMessage(player);
            return false;
        }

        return true;
    }

    /**
     * Ejecuta todas las acciones asociadas a un trigger
     */
    private boolean executeActions(Player player, ItemDefinition definition, TriggerType trigger, ActionContext context) {
        List<Action> actions = definition.getActionsForTrigger(trigger);

        if (actions.isEmpty()) {
            return false;
        }

        boolean anyExecuted = false;

        for (Action action : actions) {
            try {
                if (action.canExecute(context)) {
                    if (action.getDelay() > 0) {
                        // Ejecutar con retraso
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                executeActionSafely(action, context);
                            }
                        }.runTaskLater(plugin, action.getDelay());
                    } else {
                        // Ejecutar inmediatamente
                        executeActionSafely(action, context);
                    }
                    anyExecuted = true;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE,
                        "Error ejecutando acción " + action.getType() + " para ítem " + definition.getId(), e);
            }
        }

        // Aplicar cooldown si se ejecutó alguna acción
        if (anyExecuted && definition.getCooldown() > 0) {
            String cooldownKey = generateCooldownKey(player, definition, trigger);
            cooldownManager.setCooldown(cooldownKey, definition.getCooldown());
        }

        // Consumir ítem si es necesario
        if (anyExecuted && definition.isConsumable()) {
            consumeItem(player, context.getItemStack());
        }

        return anyExecuted;
    }

    /**
     * Ejecuta una acción de forma segura con manejo de errores
     */
    private void executeActionSafely(Action action, ActionContext context) {
        try {
            boolean success = action.execute(context);
            if (!success) {
                plugin.getLogger().warning("Acción " + action.getType() + " falló para el jugador " +
                        context.getPlayer().getName());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Error crítico ejecutando acción " + action.getType(), e);
        }
    }

    /**
     * Consume un ítem del inventario del jugador
     */
    private void consumeItem(Player player, ItemStack itemStack) {
        if (itemStack != null && itemStack.getAmount() > 0) {
            itemStack.setAmount(itemStack.getAmount() - 1);

            if (itemStack.getAmount() <= 0) {
                // El ítem se agotó, notificar al sistema de monitoreo
                ItemDefinition definition = plugin.getItemRegistry().getDefinitionFromItemStack(itemStack);
                if (definition != null) {
                    stopPeriodicMonitoring(player, definition);
                }
            }
        }
    }

    /**
     * Genera una clave única para el cooldown
     */
    private String generateCooldownKey(Player player, ItemDefinition definition, TriggerType trigger) {
        return player.getUniqueId() + ":" + definition.getId() + ":" + trigger.name();
    }

    /**
     * Genera una clave única para las tareas periódicas
     */
    private String generateTaskKey(Player player, ItemDefinition definition) {
        return player.getUniqueId() + ":" + definition.getId();
    }

    /**
     * Limpia recursos al deshabilitar el plugin
     */
    public void shutdown() {
        // Cancelar todas las tareas periódicas
        periodicTasks.values().forEach(BukkitRunnable::cancel);
        periodicTasks.clear();

        // Limpiar cache
        triggerCache.clear();

        plugin.getLogger().info("ActionDetector limpiado correctamente");
    }

    // Getters para testing y debugging
    public Cooldowns getCooldownManager() {
        return cooldownManager;
    }

    public int getActivePeriodicTasks() {
        return periodicTasks.size();
    }
}