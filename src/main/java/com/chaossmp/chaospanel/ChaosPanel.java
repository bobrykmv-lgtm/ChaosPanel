package com.chaossmp.chaospanel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class ChaosPanel extends JavaPlugin implements Listener {

    private String prefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String configuredPrefix = getConfig().getString(
                "settings.prefix",
                "&8[&bChaosPanel&8] "
        );
        prefix = ChatColor.translateAlternateColorCodes('&', configuredPrefix);

        Bukkit.getPluginManager().registerEvents(this, this);

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
            sender.sendMessage(prefix + ChatColor.RED + "У тебя нет прав на использование этой команды.");
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("help")) {
                sendHelp(sender);
                return true;
            } else if (args[0].equalsIgnoreCase("status")) {
                sendStatus(sender);
                return true;
            }
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду можно использовать только в игре.");
            return true;
        }

        if (!player.hasPermission("chaospanel.admin")) {
            player.sendMessage(prefix + ChatColor.RED + "Доступ разрешён только главным администраторам.");
            return true;
        }

        openPanel(player);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(prefix + ChatColor.YELLOW + "Доступные команды:");
        sender.sendMessage(ChatColor.GRAY + " - /ownerpanel " + ChatColor.WHITE + "- Открыть админ-панель");
        sender.sendMessage(ChatColor.GRAY + " - /ownerpanel help " + ChatColor.WHITE + "- Показать эту справку");
        sender.sendMessage(ChatColor.GRAY + " - /ownerpanel status " + ChatColor.WHITE + "- Статус сервера");
    }

    private void sendStatus(CommandSender sender) {
        double tps = Bukkit.getTPS()[0];
        long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;

        sender.sendMessage(prefix + ChatColor.AQUA + "Статус сервера ChaosSMP:");
        sender.sendMessage(ChatColor.GRAY + " TPS: " + ChatColor.WHITE + String.format("%.2f", tps));
        sender.sendMessage(ChatColor.GRAY + " Онлайн: " + ChatColor.WHITE + Bukkit.getOnlinePlayers().size());
        sender.sendMessage(ChatColor.GRAY + " Память JVM: " + ChatColor.WHITE + usedMemory + " / " + maxMemory + " MB");
    }

    private void openPanel(Player player) {
        ChaosPanelHolder holder = new ChaosPanelHolder();

        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "ChaosPanel"
        );

        holder.setInventory(inventory);

        addItem(inventory, 10, Material.COMPASS,
                "&bМониторинг сервера",
                "&7TPS, MSPT и память сервера");

        addItem(inventory, 12, Material.PLAYER_HEAD,
                "&aИгроки онлайн",
                "&7Посмотреть список игроков");

        addItem(inventory, 14, Material.BOOK,
                "&eИнспектор игроков",
                "&7[В разработке] Информация об игроках");

        addItem(inventory, 16, Material.CLOCK,
                "&6Информация о сервере",
                "&7Версия и количество игроков");

        addItem(inventory, 29, Material.PAPER,
                "&dИстория",
                "&7[В разработке] Логи и история");

        addItem(inventory, 31, Material.ENDER_PEARL,
                "&bТелепортация",
                "&7[В разработке] Телепортация к игрокам");

        addItem(inventory, 49, Material.BARRIER,
                "&cЗакрыть",
                "&7Закрыть панель");

        player.openInventory(inventory);
    }

    private void addItem(
            Inventory inventory,
            int slot,
            Material material,
            String name,
            String... lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(Arrays.stream(lore)
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .toList());
            item.setItemMeta(meta);
        }

        inventory.setItem(slot, item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Перевіряємо саме верхній інвентар (TopInventory), щоб заблокувати будь-які взаємодії як в GUI, так і Shift-кліки з інвентарю гравця
        if (!(event.getView().getTopInventory().getHolder() instanceof ChaosPanelHolder)) {
            return;
        }

        // Повністю скасовуємо будь-які кліки, переміщення та shift-кліки всередині або в бік панелі
        event.setCancelled(true);

        if (!player.hasPermission("chaospanel.admin")) {
            player.closeInventory();
            player.sendMessage(prefix + ChatColor.RED + "Недостаточно прав.");
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        switch (clicked.getType()) {
            case BARRIER -> player.closeInventory();

            case COMPASS -> {
                double tps = Bukkit.getTPS()[0];
                long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
                long usedMemory = (Runtime.getRuntime().totalMemory()
                        - Runtime.getRuntime().freeMemory()) / 1024 / 1024;

                player.sendMessage(prefix + ChatColor.AQUA
                        + "TPS: " + String.format("%.2f", tps));
                player.sendMessage(prefix + ChatColor.AQUA
                        + "Память JVM: " + usedMemory + " / "
                        + maxMemory + " MB");
            }

            case PLAYER_HEAD -> {
                player.sendMessage(prefix + ChatColor.GREEN
                        + "Игроки онлайн: " + Bukkit.getOnlinePlayers().size());

                for (Player online : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(ChatColor.GRAY + "- "
                            + ChatColor.WHITE + online.getName()
                            + ChatColor.GRAY + " | Ping: "
                            + online.getPing() + " ms");
                }
            }

            case BOOK -> {
                player.sendMessage(prefix + ChatColor.YELLOW
                        + "Инспектор игроков находится в разработке.");
            }

            case CLOCK -> {
                player.sendMessage(prefix + ChatColor.AQUA
                        + "Сервер: " + Bukkit.getName());
                player.sendMessage(prefix + ChatColor.AQUA
                        + "Онлайн: " + Bukkit.getOnlinePlayers().size());
                player.sendMessage(prefix + ChatColor.AQUA
                        + "Версия: " + Bukkit.getVersion());
            }

            case PAPER -> player.sendMessage(prefix + ChatColor.YELLOW
                    + "История событий находится в разработке.");

            case ENDER_PEARL -> {
                player.sendMessage(prefix + ChatColor.YELLOW
                        + "Модуль телепортации находится в разработке.");
            }

            default -> {
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ChaosPanelHolder) {
            event.setCancelled(true);
        }
    }
}
