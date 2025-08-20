package gc.grivyzom.mechanics.actions.impl;

import gc.grivyzom.mechanics.actions.Action;
import gc.grivyzom.mechanics.actions.ActionContext;
import gc.grivyzom.mechanics.conditions.Condition;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class ApplyPotionAction extends Action {

    private final String effectName;
    private final int duration;
    private final int amplifier;
    private final List<Condition> conditions;

    public ApplyPotionAction(String effectName, int duration, int amplifier, List<Condition> conditions, int delay) {
        super("apply_potion", false, delay);
        this.effectName = effectName.toUpperCase();
        this.duration = duration;
        this.amplifier = amplifier;
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
            int durationTicks = duration == -1 ? Integer.MAX_VALUE : duration * 20;

            PotionEffect effect = new PotionEffect(effectType, durationTicks, amplifier, false, true);
            player.addPotionEffect(effect, true);

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

