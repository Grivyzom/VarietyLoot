package gc.grivyzom.mechanics.actions.impl;

import gc.grivyzom.mechanics.actions.Action;
import gc.grivyzom.mechanics.actions.ActionContext;
import gc.grivyzom.mechanics.conditions.Condition;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class TeleportForwardAction extends Action {

    private final double distance;
    private final List<Condition> conditions;

    public TeleportForwardAction(double distance, List<Condition> conditions, int delay) {
        super("teleport_forward", false, delay);
        this.distance = distance;
        this.conditions = conditions;
    }

    @Override
    public boolean execute(ActionContext context) {
        Player player = context.getPlayer();
        Location currentLocation = player.getLocation();
        Vector direction = currentLocation.getDirection().normalize();

        // Calcular nueva ubicación
        Location newLocation = currentLocation.clone().add(direction.multiply(distance));

        // Verificar que la ubicación sea segura (no en bloques sólidos)
        newLocation.setY(newLocation.getY() + 1); // Subir un poco para evitar estar en el suelo

        // Buscar ubicación segura
        while (newLocation.getBlock().getType().isSolid() && newLocation.getY() < 256) {
            newLocation.setY(newLocation.getY() + 1);
        }

        player.teleport(newLocation);
        return true;
    }
}
