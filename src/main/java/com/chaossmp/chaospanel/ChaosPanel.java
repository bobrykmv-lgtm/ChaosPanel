package com.chaossmp.chaospanel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.regex.Pattern;

public final class ChaosPanel extends JavaPlugin implements Listener {

    private String prefix;
    private LogManager logManager;
    private BukkitTask updateTask;
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    
    // Зберігання гравців, які очікують введення ніку для офлайн-розбану
    private final Set<UUID> awaitingOfflineUnban = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String configuredPrefix = getConfig().getString(
                "settings.prefix",
                "&8[&bChaosPanel&8] "
        );
        prefix = ChatColor.translateAlternateColorCodes('&', configuredPrefix);
        logManager = new LogManager(this);

        Bukkit.getPluginManager().registerEvents(this, this);

        long interval = getConfig().getLong("settings.monitoring-update-interval", 40);
        updateTask = Bukkit.getScheduler().runTaskTimer(this, this::updateOpenInventories, 0L, interval);

        getLogger().info("ChaosPanel успешно запущен!");
        getLogger().info("Версия: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        if (logManager != null) {
            logManager.shutdown();
        }
        getLogger().info("ChaosPanel выключен.");
    }

    public LogManager getLogManager() {
        return logManager;
    }

    private boolean checkAccess(CommandSender sender, String moduleKey, String permission) {
        if (!getConfig().getBoolean("modules." + moduleKey, true)) {
            sender.sendMessage(prefix + ChatColor.RED + "Цей модуль вимкнено в конфігурації.");
            return false;
        }
        if (!sender.hasPermission("chaospanel.admin") && !sender.hasPermission(permission)) {
            sender.sendMessage(prefix + ChatColor.RED + "Недостаточно прав (" + permission + ").");
            return false;
        }
        return true;
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

        if (!sender.hasPermission("chaospanel.admin") && !sender.hasPermission("chaospanel.use") &&
            !sender.hasPermission("chaospanel.monitor") && !sender.hasPermission("chaospanel.inspect") &&
            !sender.hasPermission("chaospanel.teleport") && !sender.hasPermission("chaospanel.punish") &&
            !sender.hasPermission("chaospanel.heal") && !sender.hasPermission("chaospanel.world") &&
            !sender.hasPermission("chaospanel.history")) {
            sender.sendMessage(prefix + ChatColor.RED + "У тебя нет прав на использование этой команды.");
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("help")) {
                sendHelp(sender);
                return true;
            } else if (args[0].equalsIgnoreCase("status")) {
                if (!checkAccess(sender, "monitoring", "chaospanel.monitor")) return true;
                sendStatus(sender);
                return true;
            }
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду можно использовать только в игре.");
            return true;
        }

        openMainMenu(player);
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

    private void openMainMenu(Player player) {
        ChaosPanelHolder holder = new ChaosPanelHolder(ChaosPanelHolder.MenuType.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "ChaosPanel — Главная");
        holder.setInventory(inv);

        if (getConfig().getBoolean("modules.monitoring", true)) {
            addItem(inv, 10, Material.COMPASS, "&bМониторинг", "&7Статус, TPS и память");
        }
        if (getConfig().getBoolean("modules.player-inspector", true)) {
            addItem(inv, 12, Material.PLAYER_HEAD, "&aИгроки", "&7Список онлайн игроков");
        }
        if (getConfig().getBoolean("modules.world-management", true)) {
            addItem(inv, 14, Material.SUNFLOWER, "&eУправление миром", "&7Погода, время и миры");
        }
        if (getConfig().getBoolean("modules.history", true)) {
            addItem(inv, 16, Material.BOOK, "&dИстория действий", "&7Логи администраторов");
        }
        addItem(inv, 22, Material.BARRIER, "&cЗакрыть", "&7Закрыть панель");

        player.openInventory(inv);
    }

    private void openMonitoringMenu(Player player) {
        if (!checkAccess(player, "monitoring", "chaospanel.monitor")) return;

        ChaosPanelHolder holder = new ChaosPanelHolder(ChaosPanelHolder.MenuType.MONITORING);
        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Мониторинг сервера");
        holder.setInventory(inv);

        updateMonitoringItems(inv);
        addItem(inv, 22, Material.ARROW, "&eНазад", "&7Вернуться в главное меню");

        player.openInventory(inv);
    }

    private void updateMonitoringItems(Inventory inv) {
        double[] tpsArray = Bukkit.getTPS();
        double tps = tpsArray[0];
        long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        int online = Bukkit.getOnlinePlayers().size();

        double mspt = 0.0;
        try {
            mspt = Bukkit.getAverageTickTime();
        } catch (Throwable ignored) {
            mspt = -1.0;
        }

        addItem(inv, 10, Material.CLOCK, "&bTPS Сервера", "&7Текущий TPS: &f" + String.format("%.2f", tps));
        addItem(inv, 12, Material.COMPARATOR, "&bMSPT (Тек. задержка)", mspt >= 0 ? "&7Среднее время тика: &f" + String.format("%.2f ms", mspt) : "&7MSPT недоступно");
        addItem(inv, 14, Material.NETHER_STAR, "&bПамять JVM", "&7Использовано: &f" + usedMemory + " / " + maxMemory + " MB");
        addItem(inv, 16, Material.PLAYER_HEAD, "&bИгроки онлайн", "&7Всего в сети: &f" + online);
    }

    private void openPlayerListMenu(Player player, int page) {
        if (!checkAccess(player, "player-inspector", "chaospanel.inspect")) return;

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        int limit = 45;
        int maxPages = Math.max(1, (int) Math.ceil((double) players.size() / limit));
        if (page < 0) page = 0;
        if (page >= maxPages) page = maxPages - 1;

        ChaosPanelHolder holder = new ChaosPanelHolder(ChaosPanelHolder.MenuType.PLAYER_LIST);
        holder.setPage(page);
        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Игроки онлайн (Стр. " + (page + 1) + "/" + maxPages + ")");
        holder.setInventory(inv);

        int start = page * limit;
        int end = Math.min(start + limit, players.size());

        for (int i = start; i < end; i++) {
            Player target = players.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.setDisplayName(ChatColor.GREEN + target.getName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Здоровье: " + ChatColor.WHITE + (int) target.getHealth() + "/" + (int) target.getMaxHealth());
                lore.add(ChatColor.GRAY + "Пинг: " + ChatColor.WHITE + target.getPing() + " ms");
                lore.add(ChatColor.YELLOW + "Нажмите для инспектора");
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inv.addItem(head);
        }

        addItem(inv, 48, Material.ARROW, "&eНазад", "&7В главное меню");
        addItem(inv, 46, Material.WRITABLE_BOOK, "&6Разбанить офлайн", "&7Ввести ник для разбана");
        if (page > 0) {
            addItem(inv, 47, Material.SPECTRAL_ARROW, "&eПредыдущая страница", "&7Назад");
        }
        if (end < players.size()) {
            addItem(inv, 50, Material.SPECTRAL_ARROW, "&eСледующая страница", "&7Вперед");
        }

        player.openInventory(inv);
    }

    private void openPlayerInspector(Player admin, Player target) {
        if (!checkAccess(admin, "player-inspector", "chaospanel.inspect")) return;

        if (target == null || !target.isOnline()) {
            admin.sendMessage(prefix + ChatColor.RED + "Игрок не в сети.");
            return;
        }

        ChaosPanelHolder holder = new ChaosPanelHolder(ChaosPanelHolder.MenuType.PLAYER_INSPECTOR);
        holder.setTargetPlayer(target);
        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_AQUA + "Инспектор: " + target.getName());
        holder.setInventory(inv);

        addItem(inv, 10, Material.APPLE, "&aЗдоровье и Голод", "&7Здоровье: " + (int)target.getHealth(), "&7Голод: " + target.getFoodLevel());
        addItem(inv, 12, Material.COMPASS, "&eКоординаты", "&7Мир: " + target.getWorld().getName(), "&7X: " + target.getLocation().getBlockX(), "&7Y: " + target.getLocation().getBlockY(), "&7Z: " + target.getLocation().getBlockZ());
        addItem(inv, 14, Material.BOOK, "&bРежим игры", "&7Текущий режим: " + target.getGameMode().name());
        addItem(inv, 16, Material.CHEST, "&6Просмотр инвентаря", "&7Только для чтения");
        addItem(inv, 20, Material.DIAMOND_SWORD, "&cУправление игроком", "&7Наказания и телепорт");
        addItem(inv, 22, Material.ARROW, "&eНазад", "&7К списку игроков");

        admin.openInventory(inv);
    }

    private void openReadOnlyInventory(Player admin, Player target) {
        if (!checkAccess(admin, "player-inspector", "chaospanel.inspect")) return;
        if (target == null || !target.isOnline()) {
            admin.sendMessage(prefix + ChatColor.RED + "Игрок не в сети.");
            return;
        }

        ChaosPanelHolder holder = new ChaosPanelHolder(ChaosPanelHolder.MenuType.PLAYER_INVENTORY_VIEW);
        holder.setTargetPlayer(target);
        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.DARK_AQUA + "Инвентарь: " + target.getName());
        holder.setInventory(inv);

        for (int i = 0; i < 36; i++) {
            ItemStack item = target.getInventory().getItem(i);
            if (item != null) {
                inv.setItem(i, item.clone());
            }
        }

        ItemStack[] armor = target.getInventory().getArmorContents();
        if (armor != null) {
            if (armor.length > 3 && armor[3] != null) inv.setItem(45, armor[3].clone());
            if (armor.length > 2 && armor[2] != null) inv.setItem(46, armor[2].clone());
            if (armor.length > 1 && armor[1] != null) inv.setItem(47, armor[1].clone());
            if (armor.length > 0 && armor[0] != null) inv.setItem(48, armor[0].clone());
        }
        ItemStack offHand = target.getInventory().getItemInOffHand();
        if (offHand != null) {
            inv.setItem(50, offHand.clone());
        }

        addItem(inv, 53, Material.ARROW, "&eНазад", "&7К инспектору");

        admin.openInventory(inv);
        logManager.logAction(admin.getName(), target.getName(), "Перегляд інвентарю", null);
    }

    private void openPlayerManagement(Player admin, Player target) {
        if (!checkAccess(admin, "player-management", "chaospanel.punish")) return;
        if (target == null || !target.isOnline()) {
            admin.sendMessage(prefix + ChatColor.RED + "Игрок не в сети.");
            return;
        }

        ChaosPanelHolder holder = new ChaosPanelHolder(ChaosPanelHolder.MenuType.PLAYER_MANAGEMENT);
        holder.setTargetPlayer(target);
        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_RED + "Действия: " + target.getName());
        holder.setInventory(inv);

        addItem(inv, 10, Material.ENDER_PEARL, "&bТелепортироваться", "&7Переместиться к игроку");
        addItem(inv, 11, Material.ENDER_EYE, "&bТелепортировать к себе", "&7Забрать к себе");
        addItem(inv, 12, Material.GOLDEN_APPLE, "&aВосстановить здоровье", "&7Здоровье и голод");
        addItem(inv, 14, Material.IRON_BOOTS, "&eКикнуть", "&7С подтверждением");
        addItem(inv, 15, Material.RED_DYE, "&4Забанить", "&7Через команду из конфига");
        addItem(inv, 18, Material.WRITABLE_BOOK, "&6Выдать мут", "&7Через команду из конфига");
        addItem(inv, 19, Material.BOOK, "&eСнять мут", "&7Через команду из конфига");
        addItem(inv, 22, Material.ARROW, "&eНазад", "&7К инспектору");

        admin.openInventory(inv);
    }

    private void openConfirmationMenu(Player admin, Player target, ChaosPanelHolder.ConfirmActionType actionType) {
        ChaosPanelHolder holder = new ChaosPanelHolder(ChaosPanelHolder.MenuType.CONFIRM_ACTION);
        holder.setTargetPlayer(target);
        holder.setConfirmActionType(actionType);

        String title = switch (actionType) {
            case KICK -> "Подтвердите кик: ";
            case BAN -> "Подтвердите бан: ";
            case UNBAN -> "Подтвердите разбан: ";
            case MUTE -> "Подтвердите мут: ";
            case UNMUTE -> "Подтвердите снятие мута: ";
            case OFFLINE_UNBAN -> "Разбан офлайн-игрока";
            default -> "Подтвердите действие: ";
        };

        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_RED + title + (target != null ? target.getName() : ""));
        holder.setInventory(inv);

        addItem(inv, 11, Material.LIME_WOOL, "&aПодтвердить", "&7Выполнить действие");
        addItem(inv, 15, Material.RED_WOOL, "&cОтмена", "&7Вернуться назад");

        admin.openInventory(inv);
    }

    private void promptForOfflineUnban(Player player) {
        if (!checkAccess(player, "player-management", "chaospanel.punish")) return;
        player.closeInventory();
        awaitingOfflineUnban.add(player.getUniqueId());
        player.sendMessage(ChatColor.YELLOW + "Введите ник игрока для разбана в чат (или 'cancel' для отмены):");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingOfflineUnban.contains(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        awaitingOfflineUnban.remove(player.getUniqueId());

        String input = event.getMessage().trim();

        Bukkit.getScheduler().runTask(this, () -> {
            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "Отменено.");
                openPlayerListMenu(player, 0);
                return;
            }

            if (!NICKNAME_PATTERN.matcher(input).matches()) {
                player.sendMessage(ChatColor.RED + "Некорректный формат никнейма (только буквы, цифры и _ от 3 до 16 символов).");
                openPlayerListMenu(player, 0);
                return;
            }

            ChaosPanelHolder holder = new ChaosPanelHolder(ChaosPanelHolder.MenuType.CONFIRM_ACTION);
            holder.setTargetOfflineName(input);
            holder.setConfirmActionType(ChaosPanelHolder.ConfirmActionType.OFFLINE_UNBAN);

            Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_RED + "Разбан: " + input);
            holder.setInventory(inv);

            addItem(inv, 11, Material.LIME_WOOL, "&aПодтвердить", "&7Разбанить " + input);
            addItem(inv, 15, Material.RED_WOOL, "&cОтмена", "&7Назад");

            player.openInventory(inv);
        });
    }

    private void openWorldManagement(Player player) {
        if (!checkAccess(player, "world-management", "chaospanel.world")) return;

        ChaosPanelHolder holder = new ChaosPanelHolder(ChaosPanelHolder.MenuType.WORLD_MANAGEMENT);
        Inventory inv = Bukkit.createInventory(holder, 27, ChatColor.DARK_AQUA + "Управление миром");
        holder.setInventory(inv);

        addItem(inv, 10, Material.CLOCK, "&bУстановить День", "&7Время 1000");
        addItem(inv, 12, Material.ENDER_PEARL, "&bУстановить Ночь", "&7Время 13000");
        addItem(inv, 14, Material.WATER_BUCKET, "&bОчистить погоду", "&7Ясно");
        addItem(inv, 16, Material.POTION, "&bВключить дождь", "&7Осадки");
        addItem(inv, 22, Material.ARROW, "&eНазад", "&7В главное меню");

        player.openInventory(inv);
    }

    private void openHistoryMenu(Player player) {
        if (!checkAccess(player, "history", "chaospanel.history")) return;

        ChaosPanelHolder holder = new ChaosPanelHolder(ChaosPanelHolder.MenuType.HISTORY);
        Inventory inv = Bukkit.createInventory(holder, 54, ChatColor.DARK_AQUA + "История действий (Логи)");
        holder.setInventory(inv);

        List<Map<?, ?>> logs = logManager.getLogs();
        int count = 0;
        for (int i = logs.size() - 1; i >= 0 && count < 45; i--) {
            Map<?, ?> entry = logs.get(i);
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + String.valueOf(entry.get("action")));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Время: " + ChatColor.WHITE + entry.get("timestamp"));
                lore.add(ChatColor.GRAY + "Админ: " + ChatColor.WHITE + entry.get("admin"));
                lore.add(ChatColor.GRAY + "Цель: " + ChatColor.WHITE + entry.get("target"));
                lore.add(ChatColor.GRAY + "Причина: " + ChatColor.WHITE + entry.get("reason"));
                meta.setLore(lore);
                paper.setItemMeta(meta);
            }
            inv.addItem(paper);
            count++;
        }

        addItem(inv, 49, Material.ARROW, "&eНазад", "&7В главное меню");
        player.openInventory(inv);
    }

    private void addItem(Inventory inv, int slot, Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        inv.setItem(slot, item);
    }

    private void updateOpenInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof ChaosPanelHolder holder) {
                if (holder.getMenuType() == ChaosPanelHolder.MenuType.MONITORING) {
                    updateMonitoringItems(holder.getInventory());
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder() instanceof ChaosPanelHolder holder)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        switch (holder.getMenuType()) {
            case MAIN -> handleMainClick(player, clicked);
            case MONITORING -> handleMonitoringClick(player, clicked);
            case PLAYER_LIST -> handlePlayerListClick(player, clicked, holder);
            case PLAYER_INSPECTOR -> handleInspectorClick(player, clicked, holder);
            case PLAYER_INVENTORY_VIEW -> {
                if (clicked.getType() == Material.ARROW) {
                    if (holder.getTargetPlayer() != null && holder.getTargetPlayer().isOnline()) {
                        openPlayerInspector(player, holder.getTargetPlayer());
                    } else {
                        openPlayerListMenu(player, 0);
                    }
                }
            }
            case PLAYER_MANAGEMENT -> handleManagementClick(player, clicked, holder);
            case CONFIRM_ACTION -> handleConfirmClick(player, clicked, holder);
            case WORLD_MANAGEMENT -> handleWorldClick(player, clicked);
            case HISTORY -> handleHistoryClick(player, clicked);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ChaosPanelHolder) {
            event.setCancelled(true);
        }
    }

    private void handleMainClick(Player player, ItemStack clicked) {
        switch (clicked.getType()) {
            case COMPASS -> {
                if (getConfig().getBoolean("modules.monitoring", true)) openMonitoringMenu(player);
            }
            case PLAYER_HEAD -> {
                if (getConfig().getBoolean("modules.player-inspector", true)) openPlayerListMenu(player, 0);
            }
            case SUNFLOWER -> {
                if (getConfig().getBoolean("modules.world-management", true)) openWorldManagement(player);
            }
            case BOOK -> {
                if (getConfig().getBoolean("modules.history", true)) openHistoryMenu(player);
            }
            case BARRIER -> player.closeInventory();
            default -> {}
        }
    }

    private void handleMonitoringClick(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.ARROW) {
            openMainMenu(player);
        }
    }

    private void handlePlayerListClick(Player player, ItemStack clicked, ChaosPanelHolder holder) {
        if (clicked.getType() == Material.ARROW) {
            openMainMenu(player);
        } else if (clicked.getType() == Material.WRITABLE_BOOK) {
            if (!checkAccess(player, "player-management", "chaospanel.punish")) return;
            promptForOfflineUnban(player);
        } else if (clicked.getType() == Material.SPECTRAL_ARROW) {
            if (clicked.hasItemMeta() && clicked.getItemMeta().getDisplayName().contains("Предыдущая")) {
                openPlayerListMenu(player, holder.getPage() - 1);
            } else {
                openPlayerListMenu(player, holder.getPage() + 1);
            }
        } else if (clicked.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            if (meta != null && meta.getOwningPlayer() != null) {
                Player target = meta.getOwningPlayer().getPlayer();
                if (target != null && target.isOnline()) {
                    openPlayerInspector(player, target);
                } else {
                    player.sendMessage(prefix + ChatColor.RED + "Игрок не в сети.");
                }
            }
        }
    }

    private void handleInspectorClick(Player player, ItemStack clicked, ChaosPanelHolder holder) {
        Player target = holder.getTargetPlayer();
        switch (clicked.getType()) {
            case CHEST -> {
                if (target != null && target.isOnline()) {
                    openReadOnlyInventory(player, target);
                } else {
                    player.sendMessage(prefix + ChatColor.RED + "Игрок не в сети.");
                }
            }
            case DIAMOND_SWORD -> {
                if (target != null && target.isOnline()) {
                    openPlayerManagement(player, target);
                } else {
                    player.sendMessage(prefix + ChatColor.RED + "Игрок не в сети.");
                }
            }
            case ARROW -> openPlayerListMenu(player, 0);
            default -> {}
        }
    }

    private void handleManagementClick(Player player, ItemStack clicked, ChaosPanelHolder holder) {
        if (!checkAccess(player, "player-management", "chaospanel.punish")) {
            player.closeInventory();
            return;
        }

        Player target = holder.getTargetPlayer();
        if (target == null || !target.isOnline()) {
            player.sendMessage(prefix + ChatColor.RED + "Цель не найдена или не в сети.");
            player.closeInventory();
            return;
        }

        switch (clicked.getType()) {
            case ENDER_PEARL -> {
                if (!player.hasPermission("chaospanel.admin") && !player.hasPermission("chaospanel.teleport")) {
                    player.sendMessage(prefix + ChatColor.RED + "Недостаточно прав (chaospanel.teleport).");
                    return;
                }
                player.teleport(target.getLocation());
                player.sendMessage(prefix + ChatColor.GREEN + "Вы телепортировались к " + target.getName());
                logManager.logAction(player.getName(), target.getName(), "Телепортація до гравця", null);
            }
            case ENDER_EYE -> {
                if (!player.hasPermission("chaospanel.admin") && !player.hasPermission("chaospanel.teleport")) {
                    player.sendMessage(prefix + ChatColor.RED + "Недостаточно прав (chaospanel.teleport).");
                    return;
                }
                target.teleport(player.getLocation());
                player.sendMessage(prefix + ChatColor.GREEN + "Игрок телепортирован к вам.");
                logManager.logAction(player.getName(), target.getName(), "Телепортація гравця до себе", null);
            }
            case GOLDEN_APPLE -> {
                if (!player.hasPermission("chaospanel.admin") && !player.hasPermission("chaospanel.heal")) {
                    player.sendMessage(prefix + ChatColor.RED + "Недостаточно прав (chaospanel.heal).");
                    return;
                }
                if (target.isOnline()) {
                    AttributeInstance maxHealthAttr = target.getAttribute(Attribute.MAX_HEALTH);
                    double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
                    target.setHealth(maxHealth);
                    target.setFoodLevel(20);
                    player.sendMessage(prefix + ChatColor.GREEN + "Здоровье и голод игрока восстановлены.");
                    logManager.logAction(player.getName(), target.getName(), "Відновлення здоров'я", null);
                } else {
                    player.sendMessage(prefix + ChatColor.RED + "Гравець більше не в мережі.");
                }
            }
            case IRON_BOOTS -> openConfirmationMenu(player, target, ChaosPanelHolder.ConfirmActionType.KICK);
            case RED_DYE -> openConfirmationMenu(player, target, ChaosPanelHolder.ConfirmActionType.BAN);
            case WRITABLE_BOOK -> openConfirmationMenu(player, target, ChaosPanelHolder.ConfirmActionType.MUTE);
            case BOOK -> openConfirmationMenu(player, target, ChaosPanelHolder.ConfirmActionType.UNMUTE);
            case ARROW -> openPlayerInspector(player, target);
            default -> {}
        }
    }

    private void handleConfirmClick(Player player, ItemStack clicked, ChaosPanelHolder holder) {
        Player target = holder.getTargetPlayer();
        String offlineName = holder.getTargetOfflineName();
        ChaosPanelHolder.ConfirmActionType actionType = holder.getConfirmActionType();

        if (clicked.getType() == Material.RED_WOOL) {
            if (actionType == ChaosPanelHolder.ConfirmActionType.OFFLINE_UNBAN) {
                openPlayerListMenu(player, 0);
            } else if (target != null && target.isOnline()) {
                openPlayerManagement(player, target);
            } else {
                openMainMenu(player);
            }
            return;
        }

        if (clicked.getType() == Material.LIME_WOOL) {
            if (!checkAccess(player, "player-management", "chaospanel.punish")) return;

            if (actionType == ChaosPanelHolder.ConfirmActionType.OFFLINE_UNBAN) {
                String unbanCmdTmpl = getConfig().getString("integration.unban-command", "pardon {player}");
                String unbanCmd = unbanCmdTmpl.replace("{player}", offlineName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), unbanCmd);
                player.sendMessage(prefix + ChatColor.GREEN + "Запит на разбан надіслано для " + offlineName);
                logManager.logAction(player.getName(), offlineName, "Розбан офлайн-гравця", "Через інтеграцію");
                player.closeInventory();
                return;
            }

            if (target == null || !target.isOnline()) {
                player.sendMessage(prefix + ChatColor.RED + "Цель недоступна.");
                player.closeInventory();
                return;
            }

            switch (actionType) {
                case KICK -> {
                    target.kickPlayer("Вы были исключены администратором " + player.getName());
                    player.sendMessage(prefix + ChatColor.GREEN + "Игрок кикнут.");
                    logManager.logAction(player.getName(), target.getName(), "Кік гравця", "Підтверджено");
                }
                case BAN -> {
                    String banCmdTmpl = getConfig().getString("integration.ban-command", "ban {player} {reason} -s");
                    String banCmd = banCmdTmpl.replace("{player}", target.getName()).replace("{reason}", "Нарушение правил");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), banCmd);
                    player.sendMessage(prefix + ChatColor.YELLOW + "Запит на бан передано у консоль.");
                    logManager.logAction(player.getName(), target.getName(), "Бан гравця", "Через інтеграцію");
                }
                case MUTE -> {
                    String muteCmdTmpl = getConfig().getString("integration.mute-command", "mute {player} {reason}");
                    String muteCmd = muteCmdTmpl.replace("{player}", target.getName()).replace("{reason}", "Нарушение чата");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), muteCmd);
                    player.sendMessage(prefix + ChatColor.YELLOW + "Запит на мут передано у консоль.");
                    logManager.logAction(player.getName(), target.getName(), "Мут гравця", "Через інтеграцію");
                }
                case UNMUTE -> {
                    String unmuteCmdTmpl = getConfig().getString("integration.unmute-command", "unmute {player}");
                    String unmuteCmd = unmuteCmdTmpl.replace("{player}", target.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), unmuteCmd);
                    player.sendMessage(prefix + ChatColor.YELLOW + "Запит на зняття муту передано у консоль.");
                    logManager.logAction(player.getName(), target.getName(), "Зняття муту", "Через інтеграцію");
                }
            }
            player.closeInventory();
        }
    }

    private void handleWorldClick(Player player, ItemStack clicked) {
        if (!checkAccess(player, "world-management", "chaospanel.world")) return;
        World world = player.getWorld();
        switch (clicked.getType()) {
            case CLOCK -> {
                world.setTime(1000);
                player.sendMessage(prefix + ChatColor.AQUA + "Время установлено на день (1000).");
                logManager.logAction(player.getName(), null, "Встановлено день", world.getName());
            }
            case ENDER_PEARL -> {
                world.setTime(13000);
                player.sendMessage(prefix + ChatColor.AQUA + "Время установлено на ночь (13000).");
                logManager.logAction(player.getName(), null, "Встановлено ніч", world.getName());
            }
            case WATER_BUCKET -> {
                world.setStorm(false);
                world.setThundering(false);
                player.sendMessage(prefix + ChatColor.AQUA + "Погода очищена.");
                logManager.logAction(player.getName(), null, "Очищено погоду", world.getName());
            }
            case POTION -> {
                world.setStorm(true);
                player.sendMessage(prefix + ChatColor.AQUA + "Включен дождь.");
                logManager.logAction(player.getName(), null, "Увімкнено дощ", world.getName());
            }
            case ARROW -> openMainMenu(player);
            default -> {}
        }
    }

    private void handleHistoryClick(Player player, ItemStack clicked) {
        if (!checkAccess(player, "history", "chaospanel.history")) return;
        if (clicked.getType() == Material.ARROW) {
            openMainMenu(player);
        }
    }
}
