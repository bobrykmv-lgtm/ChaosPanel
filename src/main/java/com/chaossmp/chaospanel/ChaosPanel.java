package com.chaossmp.chaospanel;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ChaosPanelHolder implements InventoryHolder {

    public enum MenuType {
        MAIN,
        MONITORING,
        PLAYER_LIST,
        PLAYER_INSPECTOR,
        PLAYER_INVENTORY_VIEW,
        PLAYER_MANAGEMENT,
        CONFIRM_ACTION,
        WORLD_MANAGEMENT,
        HISTORY
    }

    public enum ConfirmActionType {
        KICK,
        BAN,
        UNBAN,
        MUTE,
        UNMUTE,
        OFFLINE_UNBAN
    }

    private Inventory inventory;
    private MenuType menuType;
    private Player targetPlayer;
    private String targetOfflineName;
    private int page = 0;
    private ConfirmActionType confirmActionType;

    public ChaosPanelHolder(MenuType menuType) {
        this.menuType = menuType;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }

    public void setTargetPlayer(Player targetPlayer) {
        this.targetPlayer = targetPlayer;
    }

    public String getTargetOfflineName() {
        return targetOfflineName;
    }

    public void setTargetOfflineName(String targetOfflineName) {
        this.targetOfflineName = targetOfflineName;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public ConfirmActionType getConfirmActionType() {
        return confirmActionType;
    }

    public void setConfirmActionType(ConfirmActionType confirmActionType) {
        this.confirmActionType = confirmActionType;
    }
}
