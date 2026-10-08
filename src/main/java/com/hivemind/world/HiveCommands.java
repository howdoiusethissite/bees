package com.hivemind.world;

import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** {@code /hivemind raid} starts a raid in the hive you're standing in; {@code /hivemind leave} takes you out. */
public final class HiveCommands {
	private HiveCommands() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> dispatcher.register(
			Commands.literal("hivemind")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("raid").executes(c -> raid(c.getSource())))
				.then(Commands.literal("leave").executes(c -> {
					HiveTravel.leaveHive(c.getSource().getPlayerOrException());
					return 1;
				}))
		));
	}

	private static int raid(final CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		int index = HiveLayout.isHiveLevel(player.level()) ? HiveLayout.indexAt(player.position()) : -1;
		if (index < 0) {
			source.sendFailure(Component.translatable("commands.hivemind.not_in_hive"));
			return 0;
		}
		if (!HiveRaids.forceRaid(player.level(), index, List.of(player))) {
			source.sendFailure(Component.translatable("commands.hivemind.raid_failed"));
			return 0;
		}
		return 1;
	}
}
