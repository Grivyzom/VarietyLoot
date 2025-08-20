package gc.grivyzom.items;

import gc.grivyzom.VarietyMain;
import gc.grivyzom.mechanics.TriggerType;
import gc.grivyzom.mechanics.actions.Action;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fábrica para crear ItemStacks desde ItemDefinitions
 * Se encarga de convertir las definiciones en ítems funcionales de Minecraft
 */
public class ItemFactory {

    private static final String NBT_ITEM_ID = "varietyloot_item_id";
    private static final String NBT_ITEM_VERSION = "varietyloot_item_version";

    private final VarietyMain plugin;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey itemVersionKey;

    public ItemFactory(VarietyMain plugin) {
        this.plugin = plugin;
        this.itemIdKey = new NamespacedKey(plugin, NBT_ITEM_ID);
        this.itemVersionKey = new NamespacedKey(plugin, NBT_ITEM_VERSION);
    }

    /**
     * Crea un ItemStack desde una ItemDefinition
     * @param definition La definición del ítem
     * @param amount La cantidad de ítems a crear
     * @return El ItemStack creado
     */
    public ItemStack createItemStack(ItemDefinition definition, int amount) {
        // Crear el ItemStack base
        ItemStack item = new ItemStack(definition.getMaterial(), amount);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            plugin.getLogger().warning("No se pudo obtener ItemMeta para el material: " + definition.getMaterial());
            return item;
        }

        // Configurar nombre
        if (definition.getDisplayName() != null && !definition.getDisplayName().isEmpty()) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', definition.getDisplayName()));
        }

        // Configurar lore
        if (definition.getLore() != null && !definition.getLore().isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : definition.getLore()) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
        }

        // Configurar CustomModelData
        if (definition.getCustomModelData() > 0) {
            meta.setCustomModelData(definition.getCustomModelData());
        }

        // Configurar indestructible
        if (definition.isUnbreakable()) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }

        // Añadir encantamientos
        if (definition.getEnchantments() != null) {
            for (Map.Entry<Enchantment, Integer> entry : definition.getEnchantments().entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }

        // Configurar brillo sin encantamientos
        if (definition.isGlowing() && (definition.getEnchantments() == null || definition.getEnchantments().isEmpty())) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // Ocultar atributos por defecto para una apariencia más limpia
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // Almacenar ID del ítem en NBT para identificación
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, definition.getId());
        meta.getPersistentDataContainer().set(itemVersionKey, PersistentDataType.STRING, "1.0");

        // Aplicar meta al item
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Crea un ItemStack con cantidad 1
     */
    public ItemStack createItemStack(ItemDefinition definition) {
        return createItemStack(definition, 1);
    }

    /**
     * Verifica si un ItemStack es un ítem personalizado de VarietyLoot
     * @param itemStack El ItemStack a verificar
     * @return true si es un ítem personalizado, false en caso contrario
     */
    public boolean isCustomItem(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        return meta.getPersistentDataContainer().has(itemIdKey, PersistentDataType.STRING);
    }

    /**
     * Obtiene el ID de un ítem personalizado
     * @param itemStack El ItemStack del ítem personalizado
     * @return El ID del ítem o null si no es un ítem personalizado
     */
    public String getCustomItemId(ItemStack itemStack) {
        if (!isCustomItem(itemStack)) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();
        return meta.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    /**
     * Verifica si un ItemStack corresponde a una ItemDefinition específica
     * @param itemStack El ItemStack a verificar
     * @param definition La definición a comparar
     * @return true si corresponden, false en caso contrario
     */
    public boolean matches(ItemStack itemStack, ItemDefinition definition) {
        String itemId = getCustomItemId(itemStack);
        return itemId != null && itemId.equals(definition.getId());
    }

    /**
     * Actualiza un ItemStack existente con una nueva definición
     * Útil para mantener ítems actualizados cuando se cambia la configuración
     * @param itemStack El ItemStack a actualizar
     * @param newDefinition La nueva definición
     * @return true si se actualizó correctamente
     */
    public boolean updateItemStack(ItemStack itemStack, ItemDefinition newDefinition) {
        if (!isCustomItem(itemStack)) {
            return false;
        }

        String currentId = getCustomItemId(itemStack);
        if (!newDefinition.getId().equals(currentId)) {
            return false;
        }

        // Mantener la cantidad actual
        int currentAmount = itemStack.getAmount();

        // Crear nuevo ItemStack con la definición actualizada
        ItemStack newItem = createItemStack(newDefinition, currentAmount);

        // Copiar los contenidos del nuevo item al item actual
        itemStack.setType(newItem.getType());
        itemStack.setItemMeta(newItem.getItemMeta());

        return true;
    }

    /**
     * Crea una copia exacta de un ItemStack personalizado
     * @param itemStack El ItemStack a clonar
     * @return Una copia exacta del ítem
     */
    public ItemStack cloneCustomItem(ItemStack itemStack) {
        if (!isCustomItem(itemStack)) {
            return null;
        }

        return itemStack.clone();
    }
}