package gc.grivyzom.mechanics.actions.impl;

import gc.grivyzom.mechanics.actions.Action;
import gc.grivyzom.mechanics.actions.ActionContext;
import gc.grivyzom.mechanics.conditions.Condition;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public class SetFireAction extends Action {

    private final int duration;
    private final List<Condition> conditions;

    public SetFireAction(int duration, List<Condition> conditions, int delay) {
        super("set_fire", true, delay); // Requiere objetivo
        this.duration = duration;
        this.conditions = conditions;
    }

    @Override
    public boolean execute(ActionContext context) {
        if (context.getTarget() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) context.getTarget();
            target.setFireTicks(duration * 20); // Convertir a ticks
            return true;
        }
        return false;
    }
}
