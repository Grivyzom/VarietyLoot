package gc.grivyzom.items;

import gc.grivyzom.VarietyMain;
import gc.grivyzom.mechanics.TriggerType;
import gc.grivyzom.mechanics.actions.Action;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Registro central que gestiona todas las definiciones de ítems personalizados
 * Se encarga de cargar, almacenar y proporcionar acceso a los ítems
 */
public class ItemRegistry {

    private final VarietyMain plugin;
    private final ItemFactory itemFactory;
    private final Map<String, ItemDefinition> registeredItems;
    private File itemsFile;
    private FileConfiguration itemsConfig;

    public ItemRegistry(VarietyMain plugin) {
        this.plugin = plugin;
        this.itemFactory = new ItemFactory(plugin);
        this.registeredItems = new ConcurrentHashMap<>();
        createItemsFile();
        loadItems();
    }

    /**
     * Crea el archivo items.yml si no existe
     */
    private void createItemsFile() {
        itemsFile = new File(plugin.getDataFolder(), "items.yml");

        if (!itemsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("items.yml", false);
        }

        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
    }

    /**
     * Carga todos los ítems desde el archivo de configuración
     */
    public void loadItems() {
        registeredItems.clear();

        if (itemsConfig == null) {
            plugin.getLogger().warning("No se pudo cargar el archivo items.yml");
            return;
        }

        ConfigurationSection itemsSection = itemsConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().info("No se encontraron ítems en items.yml");
            return;
        }

        int loadedCount = 0;

        for (String itemId : itemsSection.getKeys(false)) {
            try {
                ItemDefinition definition = loadItemDefinition(itemId, itemsSection.getConfigurationSection(itemId));
                if (definition != null) {
                    registeredItems.put(itemId, definition);
                    loadedCount++;
                    plugin.getLogger().info("Ítem cargado: " + itemId);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error al cargar el ítem: " + itemId, e);
            }
        }

        plugin.getLogger().info("Se cargaron " + loadedCount + " ítems personalizados");
    }

    /**
     * Carga una definición de ítem desde una sección de configuración
     */
    private ItemDefinition loadItemDefinition(String id, ConfigurationSection section) {
        if (section == null) {
            plugin.getLogger().warning("Sección de configuración nula para el ítem: " + id);
            return null;
        }

        try {
            // Información básica
            String displayName = section.getString("display-name", id);
            List<String> lore = section.getStringList("lore");
            String materialName = section.getString("material", "STONE");
            Material material = Material.matchMaterial(materialName);

            if (material == null) {
                plugin.getLogger().warning("Material inválido '" + materialName + "' para el ítem: " + id);
                material = Material.STONE;
            }

            // Propiedades del ítem
            int customModelData = section.getInt("custom-model-data", 0);
            boolean unbreakable = section.getBoolean("unbreakable", false);
            boolean glowing = section.getBoolean("glowing", false);

            // Cargar encantamientos
            Map<Enchantment, Integer> enchantments = loadEnchantments(section.getConfigurationSection("enchantments"));

            // Cargar triggers y acciones (por ahora vacío, se implementará en la siguiente fase)
            Map<TriggerType, List<Action>> triggers = new HashMap<>();

            // Configuración de mecánicas
            int cooldown = section.getInt("cooldown", 0);
            boolean consumable = section.getBoolean("consumable", false);

            // Configuración avanzada
            boolean stackable = section.getBoolean("stackable", true);
            int maxStackSize = section.getInt("max-stack-size", 64);
            String permission = section.getString("permission", null);
            boolean dropOnDeath = section.getBoolean("drop-on-death", true);

            return new ItemDefinition(id, displayName, lore, material, customModelData,
                    unbreakable, enchantments, glowing, triggers, cooldown,
                    consumable, stackable, maxStackSize, permission, dropOnDeath);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al procesar la configuración del ítem: " + id, e);
            return null;
        }
    }

    /**
     * Carga encantamientos desde una sección de configuración
     */
    private Map<Enchantment, Integer> loadEnchantments(ConfigurationSection section) {
        Map<Enchantment, Integer> enchantments = new HashMap<>();

        if (section == null) {
            return enchantments;
        }

        for (String enchantName : section.getKeys(false)) {
            Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantName.toLowerCase()));
            if (enchantment == null) {
                // Intentar con el nombre legacy
                try {
                    enchantment = Enchantment.getByName(enchantName.toUpperCase());
                } catch (Exception e) {
                    plugin.getLogger().warning("Encantamiento desconocido: " + enchantName);
                    continue;
                }
            }

            if (enchantment != null) {
                int level = section.getInt(enchantName, 1);
                enchantments.put(enchantment, level);
            }
        }

        return enchantments;
    }

    /**
     * Registra una nueva definición de ítem
     * @param definition La definición a registrar
     * @return true si se registró correctamente
     */
    public boolean registerItem(ItemDefinition definition) {
        if (definition == null || definition.getId() == null) {
            return false;
        }

        registeredItems.put(definition.getId(), definition);
        plugin.getLogger().info("Ítem registrado: " + definition.getId());
        return true;
    }

    /**
     * Desregistra un ítem
     * @param itemId El ID del ítem a desregistrar
     * @return true si se desregistró correctamente
     */
    public boolean unregisterItem(String itemId) {
        ItemDefinition removed = registeredItems.remove(itemId);
        if (removed != null) {
            plugin.getLogger().info("Ítem desregistrado: " + itemId);
            return true;
        }
        return false;
    }

    /**
     * Obtiene una definición de ítem por su ID
     * @param itemId El ID del ítem
     * @return La definición del ítem o null si no existe
     */
    public ItemDefinition getItemDefinition(String itemId) {
        return registeredItems.get(itemId);
    }

    /**
     * Verifica si un ítem está registrado
     * @param itemId El ID del ítem
     * @return true si el ítem está registrado
     */
    public boolean isRegistered(String itemId) {
        return registeredItems.containsKey(itemId);
    }

    /**
     * Obtiene todos los IDs de ítems registrados
     * @return Una copia del conjunto de IDs
     */
    public Set<String> getRegisteredItemIds() {
        return new HashSet<>(registeredItems.keySet());
    }

    /**
     * Obtiene todas las definiciones registradas
     * @return Una copia de la colección de definiciones
     */
    public Collection<ItemDefinition> getAllDefinitions() {
        return new ArrayList<>(registeredItems.values());
    }

    /**
     * Crea un ItemStack desde el ID de un ítem registrado
     * @param itemId El ID del ítem
     * @param amount La cantidad
     * @return El ItemStack creado o null si el ítem no existe
     */
    public ItemStack createItemStack(String itemId, int amount) {
        ItemDefinition definition = getItemDefinition(itemId);
        if (definition == null) {
            return null;
        }

        return itemFactory.createItemStack(definition, amount);
    }

    /**
     * Crea un ItemStack con cantidad 1
     */
    public ItemStack createItemStack(String itemId) {
        return createItemStack(itemId, 1);
    }

    /**
     * Obtiene la definición de un ItemStack personalizado
     * @param itemStack El ItemStack a verificar
     * @return La definición del ítem o null si no es un ítem personalizado
     */
    public ItemDefinition getDefinitionFromItemStack(ItemStack itemStack) {
        String itemId = itemFactory.getCustomItemId(itemStack);
        if (itemId == null) {
            return null;
        }

        return getItemDefinition(itemId);
    }

    /**
     * Verifica si un ItemStack es un ítem personalizado registrado
     * @param itemStack El ItemStack a verificar
     * @return true si es un ítem personalizado registrado
     */
    public boolean isCustomItem(ItemStack itemStack) {
        return getDefinitionFromItemStack(itemStack) != null;
    }

    /**
     * Recarga todos los ítems desde el archivo de configuración
     */
    public void reload() {
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        loadItems();
        plugin.getLogger().info("Registro de ítems recargado");
    }

    /**
     * Guarda todas las definiciones al archivo de configuración
     */
    public void save() {
        try {
            itemsConfig.save(itemsFile);
            plugin.getLogger().info("Configuración de ítems guardada");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al guardar items.yml", e);
        }
    }

    /**
     * Obtiene la fábrica de ítems
     * @return La instancia de ItemFactory
     */
    public ItemFactory getItemFactory() {
        return itemFactory;
    }

    /**
     * Obtiene estadísticas del registro
     * @return Un mapa con información estadística
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_items", registeredItems.size());
        stats.put("items_with_mechanics", registeredItems.values().stream()
                .mapToInt(def -> def.hasMechanics() ? 1 : 0).sum());
        stats.put("consumable_items", registeredItems.values().stream()
                .mapToInt(def -> def.isConsumable() ? 1 : 0).sum());
        stats.put("items_with_cooldown", registeredItems.values().stream()
                .mapToInt(def -> def.getCooldown() > 0 ? 1 : 0).sum());

        return stats;
    }
}