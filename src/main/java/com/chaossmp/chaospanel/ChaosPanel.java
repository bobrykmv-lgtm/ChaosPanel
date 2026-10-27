package com.chaossmp.chaospanel;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChaosPanel extends JavaPlugin {

private String prefix;

@Override
public void onEnable() {
    saveDefaultConfig();

    String configuredPrefix = getConfig().getString(
            "settings.prefix",
            "&8[&bChaosPanel&8] "
    );

    prefix = ChatColor.translateAlternateColorCodes('&', configuredPrefix);

    getLogger().info("ChaosPanel успешно запущен!");
    getLogger().info("Версия: " + getDescription().getVersion());
}

@Override
public void onDisable() {
    getLogger().info("ChaosPanel выключен.");
}

@Override
public boolean onCommand(
        CommandSender sender,
        Command command,
        String label,
        String[] args
) {
    if (!command.getName().equalsIgnoreCase("ownerpanel")) {
        return false;
    }

    if (!sender.hasPermission("chaospanel.use")) {
        sender.sendMessage(prefix + ChatColor.RED
                + "У вас нет доступа к этой команде.");
        return true;
    }

    if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
        sender.sendMessage(prefix + ChatColor.AQUA
                + "ChaosPanel v" + getDescription().getVersion());
        sender.sendMessage(ChatColor.GRAY + "/co help "
                + ChatColor.WHITE + "- помощь");
        sender.sendMessage(ChatColor.GRAY + "/co status "
                + ChatColor.WHITE + "- состояние плагина");
        sender.sendMessage(ChatColor.YELLOW
                + "GUI и остальные функции добавим следующим этапом.");
        return true;
    }

    if (args[0].equalsIgnoreCase("status")) {
        sender.sendMessage(prefix + ChatColor.GREEN
                + "ChaosPanel работает.");
        sender.sendMessage(ChatColor.GRAY
                + "Версия: " + getDescription().getVersion());
        sender.sendMessage(ChatColor.GRAY
                + "Сервер: " + getServer().getName());
        return true;
    }

    sender.sendMessage(prefix + ChatColor.RED
            + "Неизвестная команда. Используйте /co help.");
    return true;
}

}
