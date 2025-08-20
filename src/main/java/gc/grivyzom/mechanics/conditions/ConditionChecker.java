package gc.grivyzom.mechanics.conditions;

import gc.grivyzom.VarietyMain;
import gc.grivyzom.mechanics.actions.ActionContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Sistema de verificación de condiciones para determinar si las acciones deben ejecutarse
 * Permite crear lógica compleja de activación basada en el estado del jugador y el entorno
 */
public class ConditionChecker {

    private final VarietyMain plugin;

    // Cache de condiciones evaluadas recientemente para optimización
    private final ConcurrentHashMap<String, CachedCondition> conditionCache;

    // Registro de verificadores de condiciones personalizadas
    private final Map<String, Predicate<ActionContext>> customConditions;

    private static final long CACHE_DURATION = 1000; // 1 segundo de cache

    public ConditionChecker(VarietyMain plugin) {
        this.plugin = plugin;
        this.conditionCache = new ConcurrentHashMap<>();
        this.customConditions = new ConcurrentHashMap<>();

        registerDefaultConditions();
    }

    /**
     * Verifica si todas las condiciones especificadas se cumplen
     * @param context El contexto de la acción
     * @param conditions Lista de condiciones a verificar
     * @return true si todas las condiciones se cumplen
     */
    public boolean checkConditions(ActionContext context, List<Condition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return true; // Sin condiciones = siempre verdadero
        }

        for (Condition condition : conditions) {
            if (!checkSingleCondition(context, condition)) {
                return false; // Falla una condición = falla todo
            }
        }

        return true;
    }

    /**
     * Verifica una condición individual con cache
     */
    private boolean checkSingleCondition(ActionContext context, Condition condition) {
        // Generar clave de cache
        String cacheKey = generateCacheKey(context, condition);

        // Verificar cache
        CachedCondition cached = conditionCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.getResult();
        }

        // Evaluar condición
        boolean result = evaluateCondition(context, condition);

        // Almacenar en cache
        conditionCache.put(cacheKey, new CachedCondition(result, System.currentTimeMillis()));

        return result;
    }

    /**
     * Evalúa una condición específica
     */
    private boolean evaluateCondition(ActionContext context, Condition condition) {
        try {
            switch (condition.getType().toLowerCase()) {
                // Condiciones de salud
                case "health_above":
                    return checkHealthAbove(context.getPlayer(), condition.getValue());
                case "health_below":
                    return checkHealthBelow(context.getPlayer(), condition.getValue());
                case "health_percentage_above":
                    return checkHealthPercentageAbove(context.getPlayer(), condition.getValue());
                case "health_percentage_below":
                    return checkHealthPercentageBelow(context.getPlayer(), condition.getValue());

                // Condiciones de hambre
                case "hunger_above":
                    return checkHungerAbove(context.getPlayer(), condition.getValue());
                case "hunger_below":
                    return checkHungerBelow(context.getPlayer(), condition.getValue());

                // Condiciones de experiencia
                case "level_above":
                    return checkLevelAbove(context.getPlayer(), condition.getValue());
                case "level_below":
                    return checkLevelBelow(context.getPlayer(), condition.getValue());

                // Condiciones de efectos de poción
                case "has_potion_effect":
                    return checkHasPotionEffect(context.getPlayer(), condition.getStringValue());
                case "missing_potion_effect":
                    return !checkHasPotionEffect(context.getPlayer(), condition.getStringValue());

                // Condiciones de inventario
                case "has_item":
                    return checkHasItem(context.getPlayer(), condition.getStringValue(), condition.getValue());
                case "missing_item":
                    return !checkHasItem(context.getPlayer(), condition.getStringValue(), condition.getValue());

                // Condiciones de permisos
                case "has_permission":
                    return context.getPlayer().hasPermission(condition.getStringValue());
                case "missing_permission":
                    return !context.getPlayer().hasPermission(condition.getStringValue());

                // Condiciones de tiempo y clima
                case "is_day":
                    return checkIsDay(context.getPlayer());
                case "is_night":
                    return !checkIsDay(context.getPlayer());
                case "is_raining":
                    return context.getPlayer().getWorld().hasStorm();
                case "is_clear":
                    return !context.getPlayer().getWorld().hasStorm();

                // Condiciones de ubicación
                case "y_above":
                    return context.getLocation().getY() > condition.getValue();
                case "y_below":
                    return context.getLocation().getY() < condition.getValue();
                case "in_biome":
                    return checkInBiome(context.getPlayer(), condition.getStringValue());

                // Condiciones de estado del jugador
                case "is_sneaking":
                    return context.getPlayer().isSneaking();
                case "is_sprinting":
                    return context.getPlayer().isSprinting();
                case "is_flying":
                    return context.getPlayer().isFlying();
                case "is_in_water":
                    return context.getPlayer().isInWater();
                case "is_on_ground":
                    return context.getPlayer().isOnGround();

                // Condiciones de combate
                case "is_in_combat":
                    return checkIsInCombat(context.getPlayer());
                case "target_is_player":
                    return context.getTargetPlayer() != null;
                case "target_health_below":
                    return checkTargetHealthBelow(context, condition.getValue());

                // Condiciones personalizadas
                default:
                    return checkCustomCondition(condition.getType(), context);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error evaluando condición " + condition.getType() + ": " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // VERIFICADORES ESPECÍFICOS
    // ==========================================

    private boolean checkHealthAbove(Player player, double value) {
        return player.getHealth() > value;
    }

    private boolean checkHealthBelow(Player player, double value) {
        return player.getHealth() < value;
    }

    private boolean checkHealthPercentageAbove(Player player, double percentage) {
        double healthPercentage = (player.getHealth() / player.getMaxHealth()) * 100;
        return healthPercentage > percentage;
    }

    private boolean checkHealthPercentageBelow(Player player, double percentage) {
        double healthPercentage = (player.getHealth() / player.getMaxHealth()) * 100;
        return healthPercentage < percentage;
    }

    private boolean checkHungerAbove(Player player, double value) {
        return player.getFoodLevel() > value;
    }

    private boolean checkHungerBelow(Player player, double value) {
        return player.getFoodLevel() < value;
    }

    private boolean checkLevelAbove(Player player, double value) {
        return player.getLevel() > value;
    }

    private boolean checkLevelBelow(Player player, double value) {
        return player.getLevel() < value;
    }

    private boolean checkHasPotionEffect(Player player, String effectName) {
        try {
            PotionEffectType effectType = PotionEffectType.getByName(effectName.toUpperCase());
            return effectType != null && player.hasPotionEffect(effectType);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkHasItem(Player player, String materialName, double amount) {
        try {
            org.bukkit.Material material = org.bukkit.Material.valueOf(materialName.toUpperCase());
            int requiredAmount = (int) amount;
            int totalAmount = 0;

            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material) {
                    totalAmount += item.getAmount();
                }
            }

            return totalAmount >= requiredAmount;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkIsDay(Player player) {
        long time = player.getWorld().getTime();
        return time >= 0 && time < 13000; // 6:00 AM a 7:00 PM aprox
    }

    private boolean checkInBiome(Player player, String biomeName) {
        try {
            org.bukkit.block.Biome biome = org.bukkit.block.Biome.valueOf(biomeName.toUpperCase());
            return player.getLocation().getBlock().getBiome() == biome;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkIsInCombat(Player player) {
        // Verificar si el jugador tiene efectos de combate recientes
        // Esto puede expandirse con un sistema de combate más sofisticado
        return player.getLastDamage() > 0 &&
                (System.currentTimeMillis() - player.getLastDamage()) < 10000; // 10 segundos
    }

    private boolean checkTargetHealthBelow(ActionContext context, double value) {
        if (context.getTarget() instanceof org.bukkit.entity.LivingEntity) {
            org.bukkit.entity.LivingEntity livingTarget = (org.bukkit.entity.LivingEntity) context.getTarget();
            return livingTarget.getHealth() < value;
        }
        return false;
    }

    private boolean checkCustomCondition(String type, ActionContext context) {
        Predicate<ActionContext> checker = customConditions.get(type);
        return checker != null && checker.test(context);
    }

    // ==========================================
    // GESTIÓN DE CONDICIONES PERSONALIZADAS
    // ==========================================

    /**
     * Registra una condición personalizada
     * @param name Nombre de la condición
     * @param checker Función que evalúa la condición
     */
    public void registerCustomCondition(String name, Predicate<ActionContext> checker) {
        customConditions.put(name.toLowerCase(), checker);
        plugin.getLogger().info("Condición personalizada registrada: " + name);
    }

    /**
     * Registra condiciones por defecto del sistema
     */
    private void registerDefaultConditions() {
        // Condición de cooldown personalizado
        registerCustomCondition("not_on_cooldown", context -> {
            if (context.getItemDefinition().getCooldown() <= 0) {
                return true;
            }
            // Implementar verificación de cooldown específica si es necesario
            return true;
        });

        // Condición de mundo específico
        registerCustomCondition("in_world", context -> {
            // Esta condición necesitaría parámetros adicionales en la implementación real
            return true;
        });
    }

    // ==========================================
    // UTILIDADES
    // ==========================================

    /**
     * Genera una clave de cache para una condición
     */
    private String generateCacheKey(ActionContext context, Condition condition) {
        return context.getPlayer().getUniqueId() + ":" +
                condition.getType() + ":" +
                condition.getValue() + ":" +
                condition.getStringValue();
    }

    /**
     * Limpia el cache de condiciones expiradas
     */
    public void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        conditionCache.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));
    }

    /**
     * Limpia todas las condiciones cacheadas de un jugador
     */
    public void cleanupPlayer(Player player) {
        String playerKey = player.getUniqueId().toString();
        conditionCache.entrySet().removeIf(entry -> entry.getKey().contains(playerKey));
    }

    /**
     * Obtiene estadísticas del sistema de condiciones
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("cached_conditions", conditionCache.size());
        stats.put("custom_conditions", customConditions.size());

        long currentTime = System.currentTimeMillis();
        long expiredCount = conditionCache.values().stream()
                .mapToLong(cached -> cached.isExpired(currentTime) ? 1 : 0)
                .sum();

        stats.put("expired_cache_entries", expiredCount);
        return stats;
    }

    // ==========================================
    // CLASE AUXILIAR PARA CACHE
    // ==========================================

    private static class CachedCondition {
        private final boolean result;
        private final long timestamp;

        public CachedCondition(boolean result, long timestamp) {
            this.result = result;
            this.timestamp = timestamp;
        }

        public boolean getResult() {
            return result;
        }

        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        public boolean isExpired(long currentTime) {
            return (currentTime - timestamp) > CACHE_DURATION;
        }
    }
}