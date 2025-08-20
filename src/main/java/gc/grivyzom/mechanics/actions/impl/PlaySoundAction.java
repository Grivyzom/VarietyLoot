package gc.grivyzom.mechanics.actions.impl;

import gc.grivyzom.mechanics.actions.Action;
import gc.grivyzom.mechanics.actions.ActionContext;
import gc.grivyzom.mechanics.conditions.Condition;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

public class PlaySoundAction extends Action {

    private final String soundName;
    private final float volume;
    private final float pitch;
    private final List<Condition> conditions;

    public PlaySoundAction(String soundName, float volume, float pitch, List<Condition> conditions, int delay) {
        super("play_sound", false, delay);
        this.soundName = soundName.toUpperCase();
        this.volume = volume;
        this.pitch = pitch;
        this.conditions = conditions;
    }

    @Override
    public boolean execute(ActionContext context) {
        try {
            Sound sound = Sound.valueOf(soundName);
            Player player = context.getPlayer();

            player.playSound(player.getLocation(), sound, volume, pitch);
            return true;
        } catch (Exception e) {
            // Si el sonido no existe, intentar con un sonido por defecto
            try {
                Player player = context.getPlayer();
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, volume, pitch);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }
}