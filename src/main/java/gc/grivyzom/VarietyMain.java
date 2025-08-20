package gc.grivyzom;

import gc.grivyzom.commands.VarietyLootCommand;
import gc.grivyzom.util.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class VarietyMain extends JavaPlugin {

    private static VarietyMain instance;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        instance = this;
        messageManager = new MessageManager(this);
        messageManager.sendStartupMessage();


        registerCommands();
    }

    @Override
    public void onDisable() {
        if (messageManager != null) {
            messageManager.sendShutdownMessage();
        }
        instance = null;
    }

    public static VarietyMain getInstance() {
        return instance;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }


    private void registerCommands() {
        getCommand("varietyloot").setExecutor(new VarietyLootCommand(this));
    }
}