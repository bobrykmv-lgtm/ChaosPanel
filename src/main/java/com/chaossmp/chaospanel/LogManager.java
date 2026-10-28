package com.chaossmp.chaospanel;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LogManager {
    private final JavaPlugin plugin;
    private File file;
    private FileConfiguration config;
    private final ReentrantLock lock = new ReentrantLock();
    private final BlockingQueue<Map<String, Object>> writeQueue = new LinkedBlockingQueue<>();
    private final Thread writerThread;
    private volatile boolean running = true;

    public LogManager(JavaPlugin plugin) {
        this.plugin = plugin;
        createLogFile();

        writerThread = new Thread(this::processQueue, "ChaosPanel-LogWriter");
        writerThread.start();
    }

    private void createLogFile() {
        lock.lock();
        try {
            String fileName = plugin.getConfig().getString("settings.logs-file-name", "logs.yml");
            file = new File(plugin.getDataFolder(), fileName);
            if (!file.exists()) {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            }
            config = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            plugin.getLogger().severe("Не удалось создать файл логов: " + e.getMessage());
            config = new YamlConfiguration();
        } finally {
            lock.unlock();
        }
    }

    public void logAction(String adminName, String targetName, String action, String reason) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("timestamp", timestamp);
        entry.put("admin", adminName);
        entry.put("target", targetName != null ? targetName : "Немає");
        entry.put("action", action);
        entry.put("reason", reason != null ? reason : "Без причини");

        lock.lock();
        try {
            List<Map<?, ?>> logs = config.getMapList("history");
            logs = logs != null ? new ArrayList<>(logs) : new ArrayList<>();
            logs.add(entry);
            config.set("history", logs);
        } finally {
            lock.unlock();
        }

        writeQueue.offer(entry);
    }

    private void processQueue() {
        while (running || !writeQueue.isEmpty()) {
            try {
                Map<String, Object> entry = writeQueue.poll(200, TimeUnit.MILLISECONDS);
                if (entry != null) {
                    flushQueueToDisk();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        flushQueueToDisk();
    }

    private void flushQueueToDisk() {
        List<Map<String, Object>> drainList = new ArrayList<>();
        writeQueue.drainTo(drainList);

        lock.lock();
        try {
            if (config != null && file != null) {
                config.save(file);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Ошибка при сохранении лога на диск: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        running = false;
        try {
            writerThread.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        flushQueueToDisk();
    }

    public List<Map<?, ?>> getLogs() {
        lock.lock();
        try {
            if (config == null) return new ArrayList<>();
            List<Map<?, ?>> logs = config.getMapList("history");
            return logs != null ? new ArrayList<>(logs) : new ArrayList<>();
        } finally {
            lock.unlock();
        }
    }
}
