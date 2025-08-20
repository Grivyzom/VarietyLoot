package gc.grivyzom.mechanics.actions.impl;

import gc.grivyzom.mechanics.actions.Action;
import gc.grivyzom.mechanics.actions.ActionContext;
import gc.grivyzom.mechanics.conditions.Condition;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.List;

public class SpawnParticlesAction extends Action {

    private final String particleName;
    private final int amount;
    private final List<Condition> conditions;

    public SpawnParticlesAction(String particleName, int amount, List<Condition> conditions, int delay) {
        super("spawn_particles", false, delay);
        this.particleName = particleName.toUpperCase();
        this.amount = amount;
        this.conditions = conditions;
    }

    @Override
    public boolean execute(ActionContext context) {
        try {
            Particle particle = Particle.valueOf(particleName);
            Player player = context.getPlayer();
            Location location = player.getLocation().add(0, 1, 0);

            player.getWorld().spawnParticle(particle, location, amount, 0.5, 0.5, 0.5, 0.1);
            return true;
        } catch (Exception e) {
            // Usar partícula por defecto si falla
            try {
                Player player = context.getPlayer();
                Location location = player.getLocation().add(0, 1, 0);
                player.getWorld().spawnParticle(Particle.SPELL_WITCH, location, amount, 0.5, 0.5, 0.5, 0.1);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }
}
