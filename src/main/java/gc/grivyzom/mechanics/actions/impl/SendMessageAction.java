package gc.grivyzom.mechanics.actions.impl;

import gc.grivyzom.mechanics.actions.Action;
import gc.grivyzom.mechanics.actions.ActionContext;
import gc.grivyzom.mechanics.conditions.Condition;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class SendMessageAction extends Action {

    private final String message;
    private final List<Condition> conditions;

    public SendMessageAction(String message, List<Condition> conditions, int delay) {
        super("send_message", false, delay);
        this.message = message;
        this.conditions = conditions;
    }

    @Override
    public boolean execute(ActionContext context) {
        Player player = context.getPlayer();
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        player.sendMessage(coloredMessage);
        return true;
    }
}
