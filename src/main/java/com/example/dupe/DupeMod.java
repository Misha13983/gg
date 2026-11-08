package com.example.dupe;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod("dupe_mod")
public class DupeMod {
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final int COOLDOWN_SECONDS = 2;

    public DupeMod() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("dupe")
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                
                if (!(source.getEntity() instanceof ServerPlayer)) {
                    source.sendFailure(Component.literal("Команда доступна только игрокам!"));
                    return 0;
                }
                
                ServerPlayer player = (ServerPlayer) source.getEntity();
                
                UUID playerId = player.getUUID();
                long currentTime = System.currentTimeMillis();
                
                if (cooldowns.containsKey(playerId)) {
                    long lastUse = cooldowns.get(playerId);
                    long timeLeft = (lastUse + (COOLDOWN_SECONDS * 1000L)) - currentTime;
                    
                    if (timeLeft > 0) {
                        source.sendFailure(Component.literal("Подождите " + (timeLeft / 1000 + 1) + " секунд!"));
                        return 0;
                    }
                }
                
                ItemStack itemInOffHand = player.getItemInHand(InteractionHand.OFF_HAND);
                
                if (itemInOffHand.isEmpty()) {
                    source.sendFailure(Component.literal("Возьмите предмет в левую руку!"));
                    return 0;
                }
                
                ItemStack duplicated = itemInOffHand.copy();
                
                boolean added = player.getInventory().add(duplicated);
                
                if (added) {
                    cooldowns.put(playerId, currentTime);
                    source.sendSuccess(Component.literal("Предмет продублирован: " + 
                        duplicated.getDisplayName().getString() + " x" + duplicated.getCount()), true);
                } else {
                    player.drop(duplicated, false);
                    source.sendSuccess(Component.literal("Инвентарь полон! Предмет выброшен."), true);
                }
                
                return 1;
            }));
    }
}
