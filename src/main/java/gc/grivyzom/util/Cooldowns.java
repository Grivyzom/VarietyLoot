package gc.grivyzom.util;

import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sistema eficiente de gestión de cooldowns para ítems y acciones
 * Utiliza timestamps para un rendimiento óptimo y limpieza automática
 */
public class Cooldowns {

    // Almacena el timestamp cuando expira cada cooldown
    private final ConcurrentHashMap<String, Long> cooldowns;

    // Cache para mejorar el rendimiento en consultas frecuentes
    private final ConcurrentHashMap<String, Boolean> statusCache;

    // Control de limpieza automática
    private long lastCleanup;
    private static final long CLEANUP_INTERVAL = 300000; // 5 minutos
    private static final long CACHE_DURATION = 1000; // 1 segundo

    public Cooldowns() {
        this.cooldowns = new ConcurrentHashMap<>();
        this.statusCache = new ConcurrentHashMap<>();
        this.lastCleanup = System.currentTimeMillis();
    }

    /**
     * Establece un cooldown para una clave específica
     * @param key La clave única del cooldown
     * @param seconds Duración del cooldown en segundos
     */
    public void setCooldown(String key, int seconds) {
        if (seconds <= 0) {
            removeCooldown(key);
            return;
        }

        long expirationTime = System.currentTimeMillis() + (seconds * 1000L);
        cooldowns.put(key, expirationTime);

        // Invalidar cache para esta clave
        statusCache.remove(key);

        // Limpieza periódica
        performPeriodicCleanup();
    }

    /**
     * Establece un cooldown usando un Player y un identificador
     * @param player El jugador
     * @param identifier Identificador adicional (ej: nombre del ítem)
     * @param seconds Duración en segundos
     */
    public void setCooldown(Player player, String identifier, int seconds) {
        setCooldown(generateKey(player, identifier), seconds);
    }

    /**
     * Verifica si una clave está en cooldown
     * @param key La clave a verificar
     * @return true si está en cooldown, false en caso contrario
     */
    public boolean isOnCooldown(String key) {
        // Verificar cache primero
        Boolean cached = statusCache.get(key);
        if (cached != null) {
            return cached;
        }

        Long expirationTime = cooldowns.get(key);
        if (expirationTime == null) {
            statusCache.put(key, false);
            return false;
        }

        long currentTime = System.currentTimeMillis();
        boolean onCooldown = currentTime < expirationTime;

        // Si el cooldown expiró, limpiarlo
        if (!onCooldown) {
            cooldowns.remove(key);
        }

        // Actualizar cache
        statusCache.put(key, onCooldown);
        return onCooldown;
    }

    /**
     * Verifica si un jugador está en cooldown para un identificador específico
     */
    public boolean isOnCooldown(Player player, String identifier) {
        return isOnCooldown(generateKey(player, identifier));
    }

    /**
     * Obtiene el tiempo restante de cooldown en segundos
     * @param key La clave del cooldown
     * @return Segundos restantes o 0 si no hay cooldown
     */
    public long getRemainingTime(String key) {
        Long expirationTime = cooldowns.get(key);
        if (expirationTime == null) {
            return 0;
        }

        long remaining = (expirationTime - System.currentTimeMillis()) / 1000L;
        return Math.max(0, remaining);
    }

    /**
     * Obtiene el tiempo restante para un jugador e identificador específico
     */
    public long getRemainingTime(Player player, String identifier) {
        return getRemainingTime(generateKey(player, identifier));
    }

    /**
     * Remueve un cooldown específico
     * @param key La clave del cooldown a remover
     */
    public void removeCooldown(String key) {
        cooldowns.remove(key);
        statusCache.remove(key);
    }

    /**
     * Remueve un cooldown para un jugador e identificador específico
     */
    public void removeCooldown(Player player, String identifier) {
        removeCooldown(generateKey(player, identifier));
    }

    /**
     * Limpia todos los cooldowns de un jugador específico
     * @param player El jugador cuyos cooldowns limpiar
     */
    public void cleanupPlayer(Player player) {
        String playerKey = player.getUniqueId().toString();

        // Remover cooldowns del jugador
        cooldowns.entrySet().removeIf(entry -> entry.getKey().contains(playerKey));

        // Limpiar cache del jugador
        statusCache.entrySet().removeIf(entry -> entry.getKey().contains(playerKey));
    }

    /**
     * Limpia todos los cooldowns expirados
     * @return Número de cooldowns limpiados
     */
    public int cleanupExpired() {
        long currentTime = System.currentTimeMillis();
        int cleaned = 0;

        // Limpiar cooldowns expirados
        var iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (currentTime >= entry.getValue()) {
                iterator.remove();
                statusCache.remove(entry.getKey());
                cleaned++;
            }
        }

        lastCleanup = currentTime;
        return cleaned;
    }

    /**
     * Obtiene estadísticas del sistema de cooldowns
     * @return Map con información estadística
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();

        stats.put("active_cooldowns", cooldowns.size());
        stats.put("cached_status", statusCache.size());
        stats.put("last_cleanup", lastCleanup);

        // Estadísticas adicionales
        long currentTime = System.currentTimeMillis();
        long expiredCount = cooldowns.values().stream()
                .mapToLong(expTime -> currentTime >= expTime ? 1 : 0)
                .sum();

        stats.put("expired_cooldowns", expiredCount);
        stats.put("active_cooldowns_actual", cooldowns.size() - expiredCount);

        return stats;
    }

    /**
     * Verifica y actualiza el tiempo restante de un cooldown específico
     * @param key La clave del cooldown
     * @param newSeconds Nueva duración en segundos (solo si es mayor que la actual)
     * @return true si se actualizó, false en caso contrario
     */
    public boolean updateCooldown(String key, int newSeconds) {
        Long currentExpiration = cooldowns.get(key);
        long newExpiration = System.currentTimeMillis() + (newSeconds * 1000L);

        // Solo actualizar si el nuevo cooldown es mayor
        if (currentExpiration == null || newExpiration > currentExpiration) {
            cooldowns.put(key, newExpiration);
            statusCache.remove(key); // Invalidar cache
            return true;
        }

        return false;
    }

    /**
     * Obtiene todos los cooldowns activos de un jugador
     * @param player El jugador
     * @return Map con identificadores y tiempos restantes
     */
    public Map<String, Long> getPlayerCooldowns(Player player) {
        String playerKey = player.getUniqueId().toString();
        Map<String, Long> playerCooldowns = new ConcurrentHashMap<>();
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<String, Long> entry : cooldowns.entrySet()) {
            if (entry.getKey().contains(playerKey)) {
                long remaining = (entry.getValue() - currentTime) / 1000L;
                if (remaining > 0) {
                    // Extraer el identificador de la clave
                    String identifier = extractIdentifier(entry.getKey());
                    playerCooldowns.put(identifier, remaining);
                }
            }
        }

        return playerCooldowns;
    }

    /**
     * Reduce el tiempo de un cooldown específico
     * @param key La clave del cooldown
     * @param seconds Segundos a reducir
     * @return true si se redujo exitosamente
     */
    public boolean reduceCooldown(String key, int seconds) {
        Long expirationTime = cooldowns.get(key);
        if (expirationTime == null) {
            return false;
        }

        long reduction = seconds * 1000L;
        long newExpiration = expirationTime - reduction;
        long currentTime = System.currentTimeMillis();

        if (newExpiration <= currentTime) {
            // El cooldown se agotó
            removeCooldown(key);
        } else {
            cooldowns.put(key, newExpiration);
            statusCache.remove(key); // Invalidar cache
        }

        return true;
    }

    /**
     * Limpia todos los cooldowns (usado principalmente para testing)
     */
    public void clearAll() {
        cooldowns.clear();
        statusCache.clear();
    }

    /**
     * Genera una clave única para un jugador e identificador
     */
    private String generateKey(Player player, String identifier) {
        return player.getUniqueId().toString() + ":" + identifier;
    }

    /**
     * Extrae el identificador de una clave compuesta
     */
    private String extractIdentifier(String key) {
        int lastColon = key.lastIndexOf(':');
        return lastColon != -1 ? key.substring(lastColon + 1) : key;
    }

    /**
     * Realiza limpieza periódica automática
     */
    private void performPeriodicCleanup() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanup > CLEANUP_INTERVAL) {
            cleanupExpired();
        }
    }

    /**
     * Verifica si el sistema está funcionando correctamente
     * @return true si está operativo
     */
    public boolean isHealthy() {
        try {
            // Verificaciones básicas de salud
            boolean mapsHealthy = cooldowns != null && statusCache != null;
            boolean sizesReasonable = cooldowns.size() < 10000 && statusCache.size() < 10000;

            return mapsHealthy && sizesReasonable;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtiene información de debugging
     */
    public String getDebugInfo() {
        return String.format(
                "Cooldowns: active=%d, cached=%d, lastCleanup=%dms ago",
                cooldowns.size(),
                statusCache.size(),
                System.currentTimeMillis() - lastCleanup
        );
    }
}