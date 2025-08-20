package gc.grivyzom.mechanics.actions.impl;

import gc.grivyzom.mechanics.actions.Action;
import gc.grivyzom.mechanics.actions.ActionContext;
import gc.grivyzom.mechanics.conditions.Condition;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class LaunchPlayerAction extends Action {

    private final double power;
    private final List<Condition> conditions;

    public LaunchPlayerAction(double power, List<Condition> conditions, int delay) {
        super("launch_player", false, delay);
        this.power = power;
        this.conditions = conditions;
    }

    @Override
    public boolean execute(ActionContext context) {
        Player player = context.getPlayer();
        Vector velocity = new Vector(0, power, 0);
        player.setVelocity(velocity);
        return true;
    }
}
