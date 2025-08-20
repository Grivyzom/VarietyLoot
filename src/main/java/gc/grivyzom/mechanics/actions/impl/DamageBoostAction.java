package gc.grivyzom.mechanics.actions.impl;

import gc.grivyzom.mechanics.actions.Action;
import gc.grivyzom.mechanics.actions.ActionContext;
import gc.grivyzom.mechanics.conditions.Condition;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class DamageBoostAction extends Action {

    private final double boostAmount;
    private final List<Condition> conditions;

    public DamageBoostAction(double boostAmount, List<Condition> conditions, int delay) {
        super("damage_boost", false, delay);
        this.boostAmount = boostAmount;
        this.conditions = conditions;
    }

    @Override
    public boolean execute(ActionContext context) {
        // Para implementación completa, necesitarías un sistema que trackee
        // el próximo daño del jugador y lo modifique
        // Por simplicidad, aplicamos fuerza temporal
        Player player = context.getPlayer();
        PotionEffect strength = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 60, (int)(boostAmount / 3));
        player.addPotionEffect(strength, true);
        return true;
    }
}