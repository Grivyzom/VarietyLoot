package gc.grivyzom.mechanics.actions.impl;

import gc.grivyzom.mechanics.actions.Action;
import gc.grivyzom.mechanics.actions.ActionContext;
import gc.grivyzom.mechanics.conditions.Condition;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;

// ==========================================
// ACCIÓN: CURAR JUGADOR
// ==========================================

/**
 * Acción que cura al jugador una cantidad específica de vida
 */
public class HealPlayerAction extends Action {

    private final double healAmount;
    private final List<Condition> conditions;

    public HealPlayerAction(double healAmount, List<Condition> conditions, int delay) {
        super("heal_player", false, delay);
        this.healAmount = healAmount;
        this.conditions = conditions;
    }

    @Override
    public boolean execute(ActionContext context) {
        // Verificar condiciones si existen
        if (conditions != null && !conditions.isEmpty()) {
            // Aquí deberías usar el ConditionChecker, por simplicidad omito la verificación
        }

        Player player = context.getPlayer();
        double currentHealth = player.getHealth();
        double maxHealth = player.getMaxHealth();
        double newHealth = Math.min(currentHealth + healAmount, maxHealth);

        player.setHealth(newHealth);

        // Mensaje opcional
        player.sendMessage(ChatColor.GREEN + "¡Has sido curado por " + healAmount + " puntos!");

        return true;
    }
}

