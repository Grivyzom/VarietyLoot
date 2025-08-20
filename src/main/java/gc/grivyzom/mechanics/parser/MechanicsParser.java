package gc.grivyzom.mechanics.parser;

import gc.grivyzom.VarietyMain;
import gc.grivyzom.mechanics.TriggerType;
import gc.grivyzom.mechanics.actions.Action;
import gc.grivyzom.mechanics.actions.impl.*;
import gc.grivyzom.mechanics.conditions.Condition;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Parser que convierte la configuración YAML en objetos de mecánicas funcionales
 * Se encarga de leer triggers, condiciones y acciones desde items.yml
 */
public class MechanicsParser {

    private final VarietyMain plugin;

    public MechanicsParser(VarietyMain plugin) {
        this.plugin = plugin;
    }

    /**
     * Parsea las mecánicas desde una sección de configuración de un ítem
     * @param mechanicsSection La sección "mechanics" del ítem en YAML
     * @return Map con triggers y sus acciones asociadas
     */
    public Map<TriggerType, List<Action>> parseMechanics(ConfigurationSection mechanicsSection) {
        Map<TriggerType, List<Action>> mechanics = new HashMap<>();

        if (mechanicsSection == null) {
            return mechanics;
        }

        // Iterar por cada trigger en la sección mechanics
        for (String triggerKey : mechanicsSection.getKeys(false)) {
            TriggerType trigger = TriggerType.fromConfigKey(triggerKey);

            if (trigger == null) {
                plugin.getLogger().warning("Trigger desconocido: " + triggerKey);
                continue;
            }

            ConfigurationSection triggerSection = mechanicsSection.getConfigurationSection(triggerKey);
            if (triggerSection == null) {
                continue;
            }

            // Parsear condiciones para este trigger
            List<Condition> conditions = parseConditions(triggerSection.getStringList("conditions"));

            // Parsear acciones para este trigger
            List<Action> actions = parseActions(triggerSection.getConfigurationSection("actions"), conditions);

            if (!actions.isEmpty()) {
                mechanics.put(trigger, actions);
                plugin.getLogger().info("Trigger " + trigger.name() + " cargado con " + actions.size() + " acciones");
            }
        }

        return mechanics;
    }

    /**
     * Parsea una lista de condiciones desde strings de configuración
     */
    private List<Condition> parseConditions(List<String> conditionStrings) {
        List<Condition> conditions = new ArrayList<>();

        if (conditionStrings == null) {
            return conditions;
        }

        for (String conditionString : conditionStrings) {
            Condition condition = Condition.parseFromString(conditionString);
            if (condition != null && condition.isValid()) {
                conditions.add(condition);
            } else {
                plugin.getLogger().warning("Condición inválida: " + conditionString);
            }
        }

        return conditions;
    }

    /**
     * Parsea acciones desde una sección de configuración
     */
    private List<Action> parseActions(ConfigurationSection actionsSection, List<Condition> conditions) {
        List<Action> actions = new ArrayList<>();

        if (actionsSection == null) {
            return actions;
        }

        // Las acciones pueden estar como lista o como sección
        if (actionsSection.isList("")) {
            // Formato de lista
            List<Map<?, ?>> actionList = actionsSection.getMapList("");
            for (Map<?, ?> actionMap : actionList) {
                Action action = parseAction(actionMap, conditions);
                if (action != null) {
                    actions.add(action);
                }
            }
        } else {
            // Formato de sección individual
            for (String actionKey : actionsSection.getKeys(false)) {
                ConfigurationSection actionSection = actionsSection.getConfigurationSection(actionKey);
                if (actionSection != null) {
                    Action action = parseAction(actionSection.getValues(false), conditions);
                    if (action != null) {
                        actions.add(action);
                    }
                }
            }
        }

        return actions;
    }

    /**
     * Parsea una acción individual desde un mapa de configuración
     */
    private Action parseAction(Map<?, ?> actionData, List<Condition> conditions) {
        try {
            String type = (String) actionData.get("type");
            if (type == null) {
                plugin.getLogger().warning("Acción sin tipo especificado");
                return null;
            }

            int delay = actionData.containsKey("delay") ?
                    ((Number) actionData.get("delay")).intValue() : 0;

            // Crear la acción específica según el tipo
            return createAction(type.toLowerCase(), actionData, conditions, delay);

        } catch (Exception e) {
            plugin.getLogger().warning("Error parseando acción: " + e.getMessage());
            return null;
        }
    }

    /**
     * Crea una instancia de Action específica según el tipo
     */
    private Action createAction(String type, Map<?, ?> data, List<Condition> conditions, int delay) {
        switch (type) {
            case "heal_player":
                double healAmount = getDoubleValue(data, "value", 1.0);
                return new HealPlayerAction(healAmount, conditions, delay);

            case "damage_boost":
                double damageBoost = getDoubleValue(data, "value", 1.0);
                return new DamageBoostAction(damageBoost, conditions, delay);

            case "apply_potion":
                String effect = getStringValue(data, "effect", "speed");
                int duration = getIntValue(data, "duration", 60);
                int amplifier = getIntValue(data, "amplifier", 0);
                return new ApplyPotionAction(effect, duration, amplifier, conditions, delay);

            case "remove_potion":
                String removeEffect = getStringValue(data, "effect", "speed");
                return new RemovePotionAction(removeEffect, conditions, delay);

            case "play_sound":
                String sound = getStringValue(data, "sound", "ENTITY_PLAYER_LEVELUP");
                float volume = getFloatValue(data, "volume", 1.0f);
                float pitch = getFloatValue(data, "pitch", 1.0f);
                return new PlaySoundAction(sound, volume, pitch, conditions, delay);

            case "spawn_particles":
                String particle = getStringValue(data, "particle", "FLAME");
                int amount = getIntValue(data, "amount", 10);
                return new SpawnParticlesAction(particle, amount, conditions, delay);

            case "send_message":
                String message = getStringValue(data, "message", "");
                return new SendMessageAction(message, conditions, delay);

            case "teleport_forward":
                double distance = getDoubleValue(data, "distance", 5.0);
                return new TeleportForwardAction(distance, conditions, delay);

            case "launch_player":
                double power = getDoubleValue(data, "power", 1.0);
                return new LaunchPlayerAction(power, conditions, delay);

            case "consume_experience":
                int levels = getIntValue(data, "levels", 1);
                return new ConsumeExperienceAction(levels, conditions, delay);

            case "set_fire":
                int fireDuration = getIntValue(data, "duration", 5);
                return new SetFireAction(fireDuration, conditions, delay);

            default:
                plugin.getLogger().warning("Tipo de acción desconocido: " + type);
                return null;
        }
    }

    // Métodos auxiliares para obtener valores con defaults
    private double getDoubleValue(Map<?, ?> data, String key, double defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private int getIntValue(Map<?, ?> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private float getFloatValue(Map<?, ?> data, String key, float defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }

    private String getStringValue(Map<?, ?> data, String key, String defaultValue) {
        Object value = data.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}