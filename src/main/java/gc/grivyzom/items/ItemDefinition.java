package gc.grivyzom.items;

import gc.grivyzom.mechanics.TriggerType;
import gc.grivyzom.mechanics.actions.Action;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;

/**
 * Representa la definición completa de un ítem personalizado
 * Esta clase contiene toda la información necesaria para crear y gestionar un ítem
 */
public class ItemDefinition {

    // Identificación básica
    private final String id;
    private final String displayName;
    private final List<String> lore;
    private final Material material;

    // Propiedades del ítem
    private final int customModelData;
    private final boolean unbreakable;
    private final Map<Enchantment, Integer> enchantments;
    private final boolean glowing;

    // Mecánicas del ítem
    private final Map<TriggerType, List<Action>> triggers;
    private final int cooldown; // En segundos
    private final boolean consumable;

    // Configuración avanzada
    private final boolean stackable;
    private final int maxStackSize;
    private final String permission;
    private final boolean dropOnDeath;

    /**
     * Constructor principal para crear una definición de ítem
     */
    public ItemDefinition(String id, String displayName, List<String> lore, Material material,
                          int customModelData, boolean unbreakable, Map<Enchantment, Integer> enchantments,
                          boolean glowing, Map<TriggerType, List<Action>> triggers, int cooldown,
                          boolean consumable, boolean stackable, int maxStackSize, String permission,
                          boolean dropOnDeath) {
        this.id = id;
        this.displayName = displayName;
        this.lore = lore;
        this.material = material;
        this.customModelData = customModelData;
        this.unbreakable = unbreakable;
        this.enchantments = enchantments;
        this.glowing = glowing;
        this.triggers = triggers;
        this.cooldown = cooldown;
        this.consumable = consumable;
        this.stackable = stackable;
        this.maxStackSize = maxStackSize;
        this.permission = permission;
        this.dropOnDeath = dropOnDeath;
    }

    // Getters
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public Material getMaterial() { return material; }
    public int getCustomModelData() { return customModelData; }
    public boolean isUnbreakable() { return unbreakable; }
    public Map<Enchantment, Integer> getEnchantments() { return enchantments; }
    public boolean isGlowing() { return glowing; }
    public Map<TriggerType, List<Action>> getTriggers() { return triggers; }
    public int getCooldown() { return cooldown; }
    public boolean isConsumable() { return consumable; }
    public boolean isStackable() { return stackable; }
    public int getMaxStackSize() { return maxStackSize; }
    public String getPermission() { return permission; }
    public boolean shouldDropOnDeath() { return dropOnDeath; }

    /**
     * Verifica si el ítem tiene un trigger específico
     */
    public boolean hasTrigger(TriggerType trigger) {
        return triggers.containsKey(trigger) && !triggers.get(trigger).isEmpty();
    }

    /**
     * Obtiene las acciones para un trigger específico
     */
    public List<Action> getActionsForTrigger(TriggerType trigger) {
        return triggers.getOrDefault(trigger, List.of());
    }

    /**
     * Verifica si el ítem tiene alguna mecánica definida
     */
    public boolean hasMechanics() {
        return !triggers.isEmpty();
    }

    @Override
    public String toString() {
        return "ItemDefinition{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", material=" + material +
                ", triggers=" + triggers.size() +
                '}';
    }
}