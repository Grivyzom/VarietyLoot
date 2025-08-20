package gc.grivyzom.mechanics.actions.impl;

import gc.grivyzom.mechanics.actions.Action;
import gc.grivyzom.mechanics.actions.ActionContext;
import gc.grivyzom.mechanics.conditions.Condition;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class RemovePotionAction extends Action {

    private final String effectName;
    private final List<Condition> conditions;

    public RemovePotionAction(String effectName, List<Condition> conditions, int delay) {
        super("remove_potion", false, delay);
        this.effectName = effectName.toUpperCase();
        this.conditions = conditions;
    }

    @Override
    public boolean execute(ActionContext context) {
        try {
            PotionEffectType effectType = PotionEffectType.getByName(effectName);
            if (effectType == null) {
                return false;
            }

            Player player = context.getPlayer();
            player.removePotionEffect(effectType);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
