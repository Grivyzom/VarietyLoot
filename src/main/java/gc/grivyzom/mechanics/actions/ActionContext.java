package gc.grivyzom.mechanics.actions;

import gc.grivyzom.items.ItemDefinition;
import gc.grivyzom.mechanics.TriggerType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Clase que encapsula toda la información necesaria para ejecutar una acción
 * Proporciona contexto completo sobre quién, qué, dónde y cuándo se ejecuta una acción
 */
public class ActionContext {

    // Información básica
    private final Player player;
    private final ItemDefinition itemDefinition;
    private final ItemStack itemStack;
    private final TriggerType trigger;

    // Información de ubicación
    private final Location location;
    private final Location targetLocation;

    // Información de objetivo
    private final Entity target;
    private final Player targetPlayer;

    // Información adicional
    private final String additionalData;
    private final double damage;
    private final boolean cancelled;

    /**
     * Constructor principal del contexto
     */
    private ActionContext(Builder builder) {
        this.player = builder.player;
        this.itemDefinition = builder.itemDefinition;
        this.itemStack = builder.itemStack;
        this.trigger = builder.trigger;
        this.location = builder.location;
        this.targetLocation = builder.targetLocation;
        this.target = builder.target;
        this.targetPlayer = builder.targetPlayer;
        this.additionalData = builder.additionalData;
        this.damage = builder.damage;
        this.cancelled = builder.cancelled;
    }

    // Getters principales
    public Player getPlayer() { return player; }
    public ItemDefinition getItemDefinition() { return itemDefinition; }
    public ItemStack getItemStack() { return itemStack; }
    public TriggerType getTrigger() { return trigger; }
    public Location getLocation() { return location; }
    public Location getTargetLocation() { return targetLocation; }
    public Entity getTarget() { return target; }
    public Player getTargetPlayer() { return targetPlayer; }
    public String getAdditionalData() { return additionalData; }
    public double getDamage() { return damage; }
    public boolean isCancelled() { return cancelled; }

    /**
     * Métodos de conveniencia para verificar el contexto
     */
    public boolean hasTarget() { return target != null; }
    public boolean hasTargetPlayer() { return targetPlayer != null; }
    public boolean hasTargetLocation() { return targetLocation != null; }
    public boolean hasAdditionalData() { return additionalData != null && !additionalData.isEmpty(); }

    /**
     * Builder pattern para crear contextos de manera flexible
     */
    public static class Builder {
        // Campos obligatorios
        private Player player;
        private ItemDefinition itemDefinition;
        private TriggerType trigger;

        // Campos opcionales
        private ItemStack itemStack;
        private Location location;
        private Location targetLocation;
        private Entity target;
        private Player targetPlayer;
        private String additionalData;
        private double damage = 0.0;
        private boolean cancelled = false;

        public Builder(Player player, ItemDefinition itemDefinition, TriggerType trigger) {
            this.player = player;
            this.itemDefinition = itemDefinition;
            this.trigger = trigger;
            this.location = player.getLocation(); // Ubicación del jugador por defecto
        }

        public Builder itemStack(ItemStack itemStack) {
            this.itemStack = itemStack;
            return this;
        }

        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        public Builder targetLocation(Location targetLocation) {
            this.targetLocation = targetLocation;
            return this;
        }

        public Builder target(Entity target) {
            this.target = target;
            if (target instanceof Player) {
                this.targetPlayer = (Player) target;
            }
            return this;
        }

        public Builder targetPlayer(Player targetPlayer) {
            this.targetPlayer = targetPlayer;
            this.target = targetPlayer;
            return this;
        }

        public Builder additionalData(String additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        public Builder damage(double damage) {
            this.damage = damage;
            return this;
        }

        public Builder cancelled(boolean cancelled) {
            this.cancelled = cancelled;
            return this;
        }

        public ActionContext build() {
            return new ActionContext(this);
        }
    }

    /**
     * Método de conveniencia para crear un contexto básico
     */
    public static ActionContext createSimple(Player player, ItemDefinition itemDefinition, TriggerType trigger) {
        return new Builder(player, itemDefinition, trigger).build();
    }

    /**
     * Método de conveniencia para crear un contexto con objetivo
     */
    public static ActionContext createWithTarget(Player player, ItemDefinition itemDefinition,
                                                 TriggerType trigger, Entity target) {
        return new Builder(player, itemDefinition, trigger)
                .target(target)
                .build();
    }

    @Override
    public String toString() {
        return "ActionContext{" +
                "player=" + (player != null ? player.getName() : "null") +
                ", item=" + (itemDefinition != null ? itemDefinition.getId() : "null") +
                ", trigger=" + trigger +
                ", hasTarget=" + hasTarget() +
                '}';
    }
}