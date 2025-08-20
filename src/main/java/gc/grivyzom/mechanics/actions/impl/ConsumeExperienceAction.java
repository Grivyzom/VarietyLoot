package gc.grivyzom.mechanics.actions.impl;

import gc.grivyzom.mechanics.actions.Action;
import gc.grivyzom.mechanics.actions.ActionContext;
import gc.grivyzom.mechanics.conditions.Condition;
import org.bukkit.entity.Player;

import java.util.List;

public class ConsumeExperienceAction extends Action {

    private final int levels;
    private final List<Condition> conditions;

    public ConsumeExperienceAction(int levels, List<Condition> conditions, int delay) {
        super("consume_experience", false, delay);
        this.levels = levels;
        this.conditions = conditions;
    }

    @Override
    public boolean execute(ActionContext context) {
        Player player = context.getPlayer();
        int currentLevel = player.getLevel();

        if (currentLevel >= levels) {
            player.setLevel(currentLevel - levels);
            return true;
        }

        return false; // No tiene suficiente experiencia
    }
}