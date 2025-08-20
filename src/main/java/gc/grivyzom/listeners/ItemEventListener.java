package gc.grivyzom.listeners;

import gc.grivyzom.VarietyMain;
import gc.grivyzom.items.ItemDefinition;
import gc.grivyzom.mechanics.TriggerType;
import gc.grivyzom.mechanics.actions.ActionContext;
import gc.grivyzom.mechanics.detection.ActionDetector;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener principal que detecta eventos de Minecraft y los traduce a triggers de ítems
 * Maneja la comunicación entre el sistema de eventos de Bukkit y el ActionDetector
 */
public class ItemEventListener implements Listener {

    private final VarietyMain plugin;
    private final ActionDetector actionDetector;

    // Cache para optimizar verificaciones repetitivas
    private final ConcurrentHashMap<String, Long> lastInteractionCache;

    // Control de spam para evitar múltiples ejecuciones
    private static final long INTERACTION_COOLDOWN = 100; // 100ms entre interacciones del mismo tipo

    public ItemEventListener(VarietyMain plugin, ActionDetector actionDetector) {
        this.plugin = plugin;
        this.actionDetector = actionDetector;
        this.lastInteractionCache = new ConcurrentHashMap<>();
    }

    // ==========================================
    // EVENTOS DE INTERACCIÓN
    // ==========================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!plugin.getItemRegistry().isCustomItem(item)) {
            return;
        }

        TriggerType trigger = mapActionToTrigger(event.getAction(), player.isSneaking());
        if (trigger == null) {
            return;
        }

        // Verificar spam protection
        if (isSpamming(player, trigger)) {
            return;
        }

        // Crear contexto con información del evento
        ItemDefinition definition = plugin.getItemRegistry().getDefinitionFromItemStack(item);
        ActionContext context = new ActionContext.Builder(player, definition, trigger)
                .itemStack(item)
                .targetLocation(event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : null)
                .build();

        // Ejecutar acciones
        boolean executed = actionDetector.detectAndExecute(player, item, trigger, context);

        // Cancelar evento si se ejecutaron acciones para evitar comportamiento vanilla
        if (executed) {
            event.setCancelled(true);
        }
    }

    // ==========================================
    // EVENTOS DE COMBATE
    // ==========================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        ItemStack weapon = player.getInventory().getItemInMainHand();

        if (!plugin.getItemRegistry().isCustomItem(weapon)) {
            return;
        }

        ItemDefinition definition = plugin.getItemRegistry().getDefinitionFromItemStack(weapon);
        ActionContext context = new ActionContext.Builder(player, definition, TriggerType.ATTACK_ENTITY)
                .itemStack(weapon)
                .target(event.getEntity())
                .damage(event.getDamage())
                .build();

        actionDetector.detectAndExecute(player, weapon, TriggerType.ATTACK_ENTITY, context);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (!plugin.getItemRegistry().isCustomItem(weapon)) {
            return;
        }

        ItemDefinition definition = plugin.getItemRegistry().getDefinitionFromItemStack(weapon);
        ActionContext context = new ActionContext.Builder(killer, definition, TriggerType.KILL_ENTITY)
                .itemStack(weapon)
                .target(event.getEntity())
                .build();

        actionDetector.detectAndExecute(killer, weapon, TriggerType.KILL_ENTITY, context);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Verificar ítems en armor slots y manos
        checkItemsForTrigger(player, TriggerType.DAMAGE_TAKEN, context ->
                context.damage(event.getDamage()).build());
    }

    // ==========================================
    // EVENTOS DE CONSUMO
    // ==========================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!plugin.getItemRegistry().isCustomItem(item)) {
            return;
        }

        ItemDefinition definition = plugin.getItemRegistry().getDefinitionFromItemStack(item);

        // Determinar tipo de consumo
        TriggerType trigger = item.getType().isEdible() ? TriggerType.EAT : TriggerType.DRINK;

        ActionContext context = new ActionContext.Builder(player, definition, trigger)
                .itemStack(item)
                .build();

        boolean executed = actionDetector.detectAndExecute(player, item, trigger, context);

        // También ejecutar trigger genérico CONSUME
        ActionContext consumeContext = new ActionContext.Builder(player, definition, TriggerType.CONSUME)
                .itemStack(item)
                .build();

        actionDetector.detectAndExecute(player, item, TriggerType.CONSUME, consumeContext);

        if (executed) {
            event.setCancelled(true);
        }
    }

    // ==========================================
    // EVENTOS DE MOVIMIENTO
    // ==========================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
        if (!event.isSprinting()) {
            return; // Solo cuando empieza a correr
        }

        Player player = event.getPlayer();
        checkItemsForTrigger(player, TriggerType.SPRINT, context -> context.build());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return; // Solo cuando empieza a agacharse
        }

        Player player = event.getPlayer();
        checkItemsForTrigger(player, TriggerType.SNEAK, context -> context.build());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJump(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Detectar salto comparando Y
        if (event.getTo() != null && event.getFrom() != null) {
            double yDiff = event.getTo().getY() - event.getFrom().getY();
            if (yDiff > 0.1 && player.getVelocity().getY() > 0) {
                checkItemsForTrigger(player, TriggerType.JUMP, context -> context.build());
            }
        }
    }

    // ==========================================
    // EVENTOS DE BLOQUES
    // ==========================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!plugin.getItemRegistry().isCustomItem(tool)) {
            return;
        }

        ItemDefinition definition = plugin.getItemRegistry().getDefinitionFromItemStack(tool);
        ActionContext context = new ActionContext.Builder(player, definition, TriggerType.BREAK_BLOCK)
                .itemStack(tool)
                .targetLocation(event.getBlock().getLocation())
                .additionalData(event.getBlock().getType().name())
                .build();

        boolean executed = actionDetector.detectAndExecute(player, tool, TriggerType.BREAK_BLOCK, context);

        if (executed && definition.hasTrigger(TriggerType.BREAK_BLOCK)) {
            // Permitir que las mecánicas custom manejen el rompimiento
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (!plugin.getItemRegistry().isCustomItem(item)) {
            return;
        }

        ItemDefinition definition = plugin.getItemRegistry().getDefinitionFromItemStack(item);
        ActionContext context = new ActionContext.Builder(player, definition, TriggerType.PLACE_BLOCK)
                .itemStack(item)
                .targetLocation(event.getBlock().getLocation())
                .additionalData(event.getBlock().getType().name())
                .build();

        boolean executed = actionDetector.detectAndExecute(player, item, TriggerType.PLACE_BLOCK, context);

        if (executed) {
            event.setCancelled(true);
        }
    }

    // ==========================================
    // EVENTOS DE INVENTARIO Y EQUIPAMIENTO
    // ==========================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if (!plugin.getItemRegistry().isCustomItem(item)) {
            return;
        }

        // Detectar equipamiento/desequipamiento según el slot
        if (isArmorSlot(event.getSlot()) || isHotbarSlot(event.getSlot())) {
            scheduleEquipmentCheck(player, item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Iniciar monitoreo para ítems que el jugador ya tiene equipados
        scheduleEquipmentScan(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Limpiar recursos del jugador
        actionDetector.cleanupPlayer(player);

        // Limpiar cache de interacciones
        String playerKey = player.getUniqueId().toString();
        lastInteractionCache.entrySet().removeIf(entry -> entry.getKey().contains(playerKey));
    }

    // ==========================================
    // MÉTODOS AUXILIARES
    // ==========================================

    /**
     * Mapea una acción de Bukkit a un TriggerType
     */
    private TriggerType mapActionToTrigger(Action action, boolean sneaking) {
        switch (action) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                return sneaking ? TriggerType.SHIFT_RIGHT_CLICK : TriggerType.RIGHT_CLICK;

            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                return sneaking ? TriggerType.SHIFT_LEFT_CLICK : TriggerType.LEFT_CLICK;

            default:
                return null;
        }
    }

    /**
     * Verifica si un jugador está spammeando interacciones
     */
    private boolean isSpamming(Player player, TriggerType trigger) {
        String key = player.getUniqueId() + ":" + trigger.name();
        long currentTime = System.currentTimeMillis();
        long lastTime = lastInteractionCache.getOrDefault(key, 0L);

        if (currentTime - lastTime < INTERACTION_COOLDOWN) {
            return true;
        }

        lastInteractionCache.put(key, currentTime);
        return false;
    }

    /**
     * Verifica todos los ítems relevantes de un jugador para un trigger específico
     */
    private void checkItemsForTrigger(Player player, TriggerType trigger,
                                      java.util.function.Function<ActionContext.Builder, ActionContext> contextBuilder) {

        // Verificar ítem en mano principal
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (plugin.getItemRegistry().isCustomItem(mainHand)) {
            ItemDefinition def = plugin.getItemRegistry().getDefinitionFromItemStack(mainHand);
            ActionContext context = contextBuilder.apply(
                    new ActionContext.Builder(player, def, trigger).itemStack(mainHand)
            );
            actionDetector.detectAndExecute(player, mainHand, trigger, context);
        }

        // Verificar armadura si el trigger no requiere estar en la mano
        if (!trigger.requiresInHand()) {
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                if (plugin.getItemRegistry().isCustomItem(armor)) {
                    ItemDefinition def = plugin.getItemRegistry().getDefinitionFromItemStack(armor);
                    ActionContext context = contextBuilder.apply(
                            new ActionContext.Builder(player, def, trigger).itemStack(armor)
                    );
                    actionDetector.detectAndExecute(player, armor, trigger, context);
                }
            }
        }
    }

    /**
     * Programa una verificación de equipamiento con delay
     */
    private void scheduleEquipmentCheck(Player player, ItemStack item) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                checkEquipmentChanges(player, item);
            }
        }, 1L);
    }

    /**
     * Programa un escaneo completo del equipamiento del jugador
     */
    private void scheduleEquipmentScan(Player player) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                scanPlayerEquipment(player);
            }
        }, 20L); // 1 segundo después del join
    }

    /**
     * Verifica cambios en el equipamiento y activa triggers correspondientes
     */
    private void checkEquipmentChanges(Player player, ItemStack item) {
        if (!plugin.getItemRegistry().isCustomItem(item)) {
            return;
        }

        ItemDefinition definition = plugin.getItemRegistry().getDefinitionFromItemStack(item);

        // Verificar si el ítem está actualmente equipado
        boolean equipped = isItemEquipped(player, item);

        if (equipped) {
            // Trigger ON_EQUIP
            ActionContext context = new ActionContext.Builder(player, definition, TriggerType.ON_EQUIP)
                    .itemStack(item)
                    .build();

            actionDetector.detectAndExecute(player, item, TriggerType.ON_EQUIP, context);

            // Iniciar monitoreo periódico si es necesario
            actionDetector.startPeriodicMonitoring(player, item);
        } else {
            // Trigger ON_UNEQUIP
            ActionContext context = new ActionContext.Builder(player, definition, TriggerType.ON_UNEQUIP)
                    .itemStack(item)
                    .build();

            actionDetector.detectAndExecute(player, item, TriggerType.ON_UNEQUIP, context);

            // Detener monitoreo periódico
            actionDetector.stopPeriodicMonitoring(player, definition);
        }
    }

    /**
     * Escanea todo el equipamiento del jugador para inicializar triggers
     */
    private void scanPlayerEquipment(Player player) {
        // Escanear manos
        ItemStack[] hands = {
                player.getInventory().getItemInMainHand(),
                player.getInventory().getItemInOffHand()
        };

        for (ItemStack item : hands) {
            if (plugin.getItemRegistry().isCustomItem(item)) {
                actionDetector.startPeriodicMonitoring(player, item);
            }
        }

        // Escanear armadura
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (plugin.getItemRegistry().isCustomItem(armor)) {
                actionDetector.startPeriodicMonitoring(player, armor);
            }
        }
    }

    /**
     * Verifica si un ítem está actualmente equipado por el jugador
     */
    private boolean isItemEquipped(Player player, ItemStack item) {
        // Verificar manos
        if (item.equals(player.getInventory().getItemInMainHand()) ||
                item.equals(player.getInventory().getItemInOffHand())) {
            return true;
        }

        // Verificar armadura
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (item.equals(armor)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifica si un slot es de armadura
     */
    private boolean isArmorSlot(int slot) {
        return slot >= 36 && slot <= 39; // Slots de armadura en el inventario
    }

    /**
     * Verifica si un slot es de la hotbar
     */
    private boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot <= 8; // Slots de la hotbar
    }
}