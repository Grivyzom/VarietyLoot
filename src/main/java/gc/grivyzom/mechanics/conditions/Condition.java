package gc.grivyzom.mechanics.conditions;

/**
 * Representa una condición individual que debe cumplirse para ejecutar una acción
 * Encapsula el tipo de condición, valor y parámetros adicionales
 */
public class Condition {

    private final String type;
    private final double value;
    private final String stringValue;
    private final boolean inverted;

    /**
     * Constructor principal para condiciones con valor numérico
     * @param type Tipo de condición (ej: "health_above", "level_below")
     * @param value Valor numérico para la comparación
     * @param inverted Si la condición debe invertirse (NOT)
     */
    public Condition(String type, double value, boolean inverted) {
        this.type = type.toLowerCase();
        this.value = value;
        this.stringValue = null;
        this.inverted = inverted;
    }

    /**
     * Constructor para condiciones con valor de texto
     * @param type Tipo de condición
     * @param stringValue Valor de texto para la comparación
     * @param inverted Si la condición debe invertirse
     */
    public Condition(String type, String stringValue, boolean inverted) {
        this.type = type.toLowerCase();
        this.value = 0.0;
        this.stringValue = stringValue;
        this.inverted = inverted;
    }

    /**
     * Constructor simplificado sin inversión
     */
    public Condition(String type, double value) {
        this(type, value, false);
    }

    /**
     * Constructor simplificado para valores de texto sin inversión
     */
    public Condition(String type, String stringValue) {
        this(type, stringValue, false);
    }

    /**
     * Constructor para condiciones booleanas simples
     */
    public Condition(String type, boolean inverted) {
        this.type = type.toLowerCase();
        this.value = 0.0;
        this.stringValue = null;
        this.inverted = inverted;
    }

    /**
     * Constructor más simple para condiciones booleanas
     */
    public Condition(String type) {
        this(type, false);
    }

    // Getters
    public String getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public String getStringValue() {
        return stringValue;
    }

    public boolean isInverted() {
        return inverted;
    }

    /**
     * Verifica si esta condición usa un valor numérico
     */
    public boolean hasNumericValue() {
        return stringValue == null;
    }

    /**
     * Verifica si esta condición usa un valor de texto
     */
    public boolean hasStringValue() {
        return stringValue != null;
    }

    /**
     * Crea una versión invertida de esta condición
     */
    public Condition invert() {
        if (hasStringValue()) {
            return new Condition(type, stringValue, !inverted);
        } else {
            return new Condition(type, value, !inverted);
        }
    }

    /**
     * Métodos de conveniencia para crear condiciones comunes
     */
    public static class Builder {

        // Condiciones de salud
        public static Condition healthAbove(double value) {
            return new Condition("health_above", value);
        }

        public static Condition healthBelow(double value) {
            return new Condition("health_below", value);
        }

        public static Condition healthPercentageAbove(double percentage) {
            return new Condition("health_percentage_above", percentage);
        }

        public static Condition healthPercentageBelow(double percentage) {
            return new Condition("health_percentage_below", percentage);
        }

        // Condiciones de hambre
        public static Condition hungerAbove(int value) {
            return new Condition("hunger_above", value);
        }

        public static Condition hungerBelow(int value) {
            return new Condition("hunger_below", value);
        }

        // Condiciones de nivel
        public static Condition levelAbove(int level) {
            return new Condition("level_above", level);
        }

        public static Condition levelBelow(int level) {
            return new Condition("level_below", level);
        }

        // Condiciones de efectos de poción
        public static Condition hasPotionEffect(String effectName) {
            return new Condition("has_potion_effect", effectName);
        }

        public static Condition missingPotionEffect(String effectName) {
            return new Condition("missing_potion_effect", effectName);
        }

        // Condiciones de inventario
        public static Condition hasItem(String material, int amount) {
            return new Condition("has_item", material + ":" + amount);
        }

        public static Condition missingItem(String material, int amount) {
            return new Condition("missing_item", material + ":" + amount);
        }

        // Condiciones de permisos
        public static Condition hasPermission(String permission) {
            return new Condition("has_permission", permission);
        }

        public static Condition missingPermission(String permission) {
            return new Condition("missing_permission", permission);
        }

        // Condiciones de tiempo
        public static Condition isDay() {
            return new Condition("is_day");
        }

        public static Condition isNight() {
            return new Condition("is_night");
        }

        public static Condition isRaining() {
            return new Condition("is_raining");
        }

        public static Condition isClear() {
            return new Condition("is_clear");
        }

        // Condiciones de ubicación
        public static Condition yAbove(double y) {
            return new Condition("y_above", y);
        }

        public static Condition yBelow(double y) {
            return new Condition("y_below", y);
        }

        public static Condition inBiome(String biome) {
            return new Condition("in_biome", biome);
        }

        // Condiciones de estado
        public static Condition isSneaking() {
            return new Condition("is_sneaking");
        }

        public static Condition isSprinting() {
            return new Condition("is_sprinting");
        }

        public static Condition isFlying() {
            return new Condition("is_flying");
        }

        public static Condition isInWater() {
            return new Condition("is_in_water");
        }

        public static Condition isOnGround() {
            return new Condition("is_on_ground");
        }

        // Condiciones de combate
        public static Condition isInCombat() {
            return new Condition("is_in_combat");
        }

        public static Condition targetIsPlayer() {
            return new Condition("target_is_player");
        }

        public static Condition targetHealthBelow(double health) {
            return new Condition("target_health_below", health);
        }

        // Condición personalizada
        public static Condition custom(String type) {
            return new Condition(type);
        }

        public static Condition custom(String type, double value) {
            return new Condition(type, value);
        }

        public static Condition custom(String type, String value) {
            return new Condition(type, value);
        }
    }

    /**
     * Parsea una condición desde texto de configuración
     * Formato: "tipo:valor" o "tipo:valor:invertido"
     */
    public static Condition parseFromString(String conditionString) {
        if (conditionString == null || conditionString.trim().isEmpty()) {
            return null;
        }

        String[] parts = conditionString.trim().split(":");
        if (parts.length < 1) {
            return null;
        }

        String type = parts[0].toLowerCase();
        boolean inverted = false;

        // Verificar si está invertido
        if (parts.length >= 3) {
            inverted = Boolean.parseBoolean(parts[2]);
        }

        if (parts.length >= 2) {
            String valueStr = parts[1];

            // Intentar parsear como número
            try {
                double numericValue = Double.parseDouble(valueStr);
                return new Condition(type, numericValue, inverted);
            } catch (NumberFormatException e) {
                // Si no es número, usar como string
                return new Condition(type, valueStr, inverted);
            }
        } else {
            // Condición sin valor (booleana)
            return new Condition(type, inverted);
        }
    }

    /**
     * Convierte la condición a formato de string para configuración
     */
    public String toConfigString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type);

        if (hasStringValue()) {
            sb.append(":").append(stringValue);
        } else if (value != 0.0) {
            sb.append(":").append(value);
        }

        if (inverted) {
            sb.append(":true");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (inverted) {
            sb.append("NOT ");
        }

        sb.append(type.toUpperCase());

        if (hasStringValue()) {
            sb.append(" '").append(stringValue).append("'");
        } else if (value != 0.0) {
            sb.append(" ").append(value);
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Condition condition = (Condition) obj;
        return Double.compare(condition.value, value) == 0 &&
                inverted == condition.inverted &&
                type.equals(condition.type) &&
                java.util.Objects.equals(stringValue, condition.stringValue);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, value, stringValue, inverted);
    }

    /**
     * Verifica si esta condición es válida
     */
    public boolean isValid() {
        return type != null && !type.trim().isEmpty();
    }

    /**
     * Obtiene una descripción legible de la condición
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();

        if (inverted) {
            desc.append("El jugador NO debe ");
        } else {
            desc.append("El jugador debe ");
        }

        switch (type) {
            case "health_above":
                desc.append("tener más de ").append(value).append(" puntos de vida");
                break;
            case "health_below":
                desc.append("tener menos de ").append(value).append(" puntos de vida");
                break;
            case "health_percentage_above":
                desc.append("tener más del ").append(value).append("% de vida");
                break;
            case "health_percentage_below":
                desc.append("tener menos del ").append(value).append("% de vida");
                break;
            case "level_above":
                desc.append("ser nivel ").append((int)value).append(" o superior");
                break;
            case "level_below":
                desc.append("ser nivel ").append((int)value).append(" o inferior");
                break;
            case "has_potion_effect":
                desc.append("tener el efecto ").append(stringValue);
                break;
            case "has_permission":
                desc.append("tener el permiso ").append(stringValue);
                break;
            case "is_sneaking":
                desc.append("estar agachado");
                break;
            case "is_sprinting":
                desc.append("estar corriendo");
                break;
            case "is_day":
                desc.append("que sea de día");
                break;
            case "is_night":
                desc.append("que sea de noche");
                break;
            default:
                desc.append("cumplir la condición ").append(type);
                if (hasStringValue()) {
                    desc.append(" con valor ").append(stringValue);
                } else if (value != 0.0) {
                    desc.append(" con valor ").append(value);
                }
                break;
        }

        return desc.toString();
    }
}