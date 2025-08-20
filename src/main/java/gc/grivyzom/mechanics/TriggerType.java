package gc.grivyzom.mechanics;

/**
 * Enum que define todos los tipos de activación posibles para las mecánicas de ítems
 * Cada trigger representa un momento específico donde un ítem puede ejecutar sus acciones
 */
public enum TriggerType {

    // Triggers de interacción básica
    RIGHT_CLICK("right_click", "Clic derecho"),
    LEFT_CLICK("left_click", "Clic izquierdo"),
    SHIFT_RIGHT_CLICK("shift_right_click", "Shift + Clic derecho"),
    SHIFT_LEFT_CLICK("shift_left_click", "Shift + Clic izquierdo"),

    // Triggers de combate
    ATTACK_ENTITY("attack_entity", "Atacar entidad"),
    KILL_ENTITY("kill_entity", "Matar entidad"),
    DAMAGE_TAKEN("damage_taken", "Recibir daño"),
    CRITICAL_HIT("critical_hit", "Golpe crítico"),

    // Triggers de consumo
    CONSUME("consume", "Consumir ítem"),
    EAT("eat", "Comer"),
    DRINK("drink", "Beber"),

    // Triggers de movimiento
    SPRINT("sprint", "Correr"),
    SNEAK("sneak", "Agacharse"),
    JUMP("jump", "Saltar"),
    FALL("fall", "Caer"),

    // Triggers de ambiente
    ENTER_WATER("enter_water", "Entrar al agua"),
    EXIT_WATER("exit_water", "Salir del agua"),
    LIGHTNING_STRUCK("lightning_struck", "Ser golpeado por rayo"),

    // Triggers de tiempo
    ON_EQUIP("on_equip", "Al equipar"),
    ON_UNEQUIP("on_unequip", "Al desequipar"),
    WHILE_HELD("while_held", "Mientras se sostiene"),
    PERIODIC("periodic", "Periódicamente"),

    // Triggers de proyectiles
    SHOOT_BOW("shoot_bow", "Disparar arco"),
    THROW_ITEM("throw_item", "Lanzar ítem"),
    HIT_TARGET("hit_target", "Golpear objetivo"),

    // Triggers especiales
    BREAK_BLOCK("break_block", "Romper bloque"),
    PLACE_BLOCK("place_block", "Colocar bloque"),
    INTERACT_BLOCK("interact_block", "Interactuar con bloque"),
    DEATH("death", "Al morir"),
    RESPAWN("respawn", "Al reaparecer"),

    // Triggers de condición
    LOW_HEALTH("low_health", "Vida baja"),
    FULL_HEALTH("full_health", "Vida completa"),
    LOW_HUNGER("low_hunger", "Hambre baja"),

    // Triggers sociales
    CHAT_MESSAGE("chat_message", "Mensaje en chat"),
    PLAYER_JOIN("player_join", "Jugador se une"),
    PLAYER_QUIT("player_quit", "Jugador se va");

    private final String configKey;
    private final String displayName;

    TriggerType(String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }

    /**
     * Obtiene la clave usada en la configuración YAML
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * Obtiene el nombre legible para mostrar al usuario
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Busca un TriggerType por su clave de configuración
     * @param configKey La clave del trigger en el YAML
     * @return El TriggerType correspondiente o null si no existe
     */
    public static TriggerType fromConfigKey(String configKey) {
        for (TriggerType trigger : values()) {
            if (trigger.getConfigKey().equalsIgnoreCase(configKey)) {
                return trigger;
            }
        }
        return null;
    }

    /**
     * Verifica si este trigger requiere que el ítem esté en la mano
     */
    public boolean requiresInHand() {
        switch (this) {
            case RIGHT_CLICK:
            case LEFT_CLICK:
            case SHIFT_RIGHT_CLICK:
            case SHIFT_LEFT_CLICK:
            case ATTACK_ENTITY:
            case BREAK_BLOCK:
            case PLACE_BLOCK:
            case SHOOT_BOW:
            case THROW_ITEM:
                return true;
            default:
                return false;
        }
    }

    /**
     * Verifica si este trigger es de tipo periódico (se ejecuta continuamente)
     */
    public boolean isPeriodic() {
        return this == WHILE_HELD || this == PERIODIC;
    }
}