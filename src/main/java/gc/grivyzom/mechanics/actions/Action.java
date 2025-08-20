package gc.grivyzom.mechanics.actions;

/**
 * Clase abstracta base para todas las acciones que pueden ejecutar los ítems
 * Cada acción específica debe extender esta clase e implementar el método execute
 */
public abstract class Action {

    protected final String type;
    protected final boolean requiresTarget;
    protected final int delay; // Retraso en ticks antes de ejecutar (20 ticks = 1 segundo)

    /**
     * Constructor base para todas las acciones
     * @param type Tipo de acción (para identificación)
     * @param requiresTarget Si la acción requiere un objetivo
     * @param delay Retraso en ticks antes de ejecutar la acción
     */
    public Action(String type, boolean requiresTarget, int delay) {
        this.type = type;
        this.requiresTarget = requiresTarget;
        this.delay = delay;
    }

    /**
     * Constructor simplificado sin retraso
     */
    public Action(String type, boolean requiresTarget) {
        this(type, requiresTarget, 0);
    }

    /**
     * Ejecuta la acción con el contexto proporcionado
     * @param context El contexto de ejecución con toda la información necesaria
     * @return true si la acción se ejecutó correctamente, false en caso contrario
     */
    public abstract boolean execute(ActionContext context);

    /**
     * Valida si la acción puede ejecutarse con el contexto dado
     * @param context El contexto a validar
     * @return true si la acción puede ejecutarse, false en caso contrario
     */
    public boolean canExecute(ActionContext context) {
        // Validaciones básicas
        if (context == null) {
            return false;
        }

        if (requiresTarget && context.getTarget() == null) {
            return false;
        }

        // Las subclases pueden sobrescribir este método para validaciones específicas
        return true;
    }

    /**
     * Obtiene una descripción de la acción para debugging
     */
    public String getDescription() {
        return String.format("%s (target: %s, delay: %d)",
                type, requiresTarget ? "required" : "optional", delay);
    }

    // Getters
    public String getType() { return type; }
    public boolean requiresTarget() { return requiresTarget; }
    public int getDelay() { return delay; }

    @Override
    public String toString() {
        return "Action{" +
                "type='" + type + '\'' +
                ", requiresTarget=" + requiresTarget +
                ", delay=" + delay +
                '}';
    }
}