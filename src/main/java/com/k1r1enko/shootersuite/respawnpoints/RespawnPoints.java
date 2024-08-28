package com.k1r1enko.shootersuite.respawnpoints;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.PersistentStateManager;

import java.util.Random;
import java.util.Arrays;

public class RespawnPoints implements ModInitializer {
	private static final Map<String, BlockPos[]> groups = new HashMap<>();
	private static String activeGroup = "";
	private static final String STATE_ID = "respawn_points";
	private static final String DATA_NAME = "respawn_points";


	@Override
	public void onInitialize() {

			ServerLifecycleEvents.SERVER_STARTED.register(server -> {
				for (ServerWorld world : server.getWorlds()) {
					loadData(world);
				}
			});

			ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
				for (ServerWorld world : server.getWorlds()) {
					saveData(world);
				}
			});

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(CommandManager.literal("rp")
					.then(CommandManager.literal("setpoint")
							.executes(context -> {
								ServerCommandSource source = context.getSource();
								Vec3d pos = source.getPosition();
								BlockPos blockPos = new BlockPos(
										(int) Math.floor(pos.getX()),
										(int) Math.floor(pos.getY()),
										(int) Math.floor(pos.getZ())
								);
								groups.putIfAbsent("default", new BlockPos[0]);
								BlockPos[] points = groups.get("default");
								points = Arrays.copyOf(points, points.length + 1);
								points[points.length - 1] = blockPos;
								groups.put("default", points);
								source.sendMessage(Text.of("Respawn point set at " + blockPos.toShortString()));
								System.out.println("[RespawnPoints] Setpoint command executed: " + blockPos.toShortString());
								return Command.SINGLE_SUCCESS;
							})
					)
					.then(CommandManager.literal("addtogroup")
							.then(CommandManager.argument("group", StringArgumentType.string())
									.executes(context -> {
										ServerCommandSource source = context.getSource();
										String group = StringArgumentType.getString(context, "group");
										Vec3d pos = source.getPosition();
										BlockPos blockPos = new BlockPos(
												(int) Math.floor(pos.getX()),
												(int) Math.floor(pos.getY()),
												(int) Math.floor(pos.getZ())
										);
										groups.putIfAbsent(group, new BlockPos[0]);
										BlockPos[] points = groups.get(group);
										points = Arrays.copyOf(points, points.length + 1);
										points[points.length - 1] = blockPos;
										groups.put(group, points);
										source.sendMessage(Text.of("Added point to group " + group));
										System.out.println("[RespawnPoints] Added point to group: " + group + " at " + blockPos.toShortString());
										return Command.SINGLE_SUCCESS;
									})
							)
					)
					.then(CommandManager.literal("setactivegroup")
							.then(CommandManager.argument("group", StringArgumentType.string())
									.executes(context -> {
										ServerCommandSource source = context.getSource();
										activeGroup = StringArgumentType.getString(context, "group");
										System.out.println("[RespawnPoints] Active group set to: " + activeGroup);
										return Command.SINGLE_SUCCESS;
									})
							)
					)
					.then(CommandManager.literal("respawn")
							.executes(context -> {
								ServerCommandSource source = context.getSource();
								if (source.getEntity() instanceof ServerPlayerEntity player) {
									BlockPos respawnPoint = getRandomPointFromGroup();
									if (respawnPoint != null) {
										respawnPlayer(player, respawnPoint);
										System.out.println("[RespawnPoints] Player respawned at: " + respawnPoint.toShortString());
									} else {
										System.out.println("[RespawnPoints] No respawn points available in active group.");
									}
								} else {
									System.out.println("[RespawnPoints] Command executed by non-player entity.");
								}
								return Command.SINGLE_SUCCESS;
							})
					)
					.then(CommandManager.literal("creategroup")
							.then(CommandManager.argument("group", StringArgumentType.string())
									.executes(context -> {
										String group = StringArgumentType.getString(context, "group");
										if (!groups.containsKey(group)) {
											groups.put(group, new BlockPos[0]);
											context.getSource().sendMessage(Text.of("Group " + group + " created."));
										} else {
											context.getSource().sendMessage(Text.of("Group " + group + " already exists."));
										}
										return Command.SINGLE_SUCCESS;
									})
							)
					)
					.then(CommandManager.literal("deletegroup")
							.then(CommandManager.argument("group", StringArgumentType.string())
									.executes(context -> {
										String group = StringArgumentType.getString(context, "group");
										if (groups.remove(group) != null) {
											context.getSource().sendMessage(Text.of("Group " + group + " deleted."));
										} else {
											context.getSource().sendMessage(Text.of("Group " + group + " not found."));
										}
										return Command.SINGLE_SUCCESS;
									})
							)
					)
					.then(CommandManager.literal("listgroups")
							.executes(context -> {
								ServerCommandSource source = context.getSource();
								if (groups.isEmpty()) {
									source.sendMessage(Text.of("No groups available."));
								} else {
									source.sendMessage(Text.of("Groups: " + String.join(", ", groups.keySet())));
								}
								return Command.SINGLE_SUCCESS;
							})
					)
			);
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((player, world, alive) -> {
			BlockPos respawnPoint = getRandomPointFromGroup();
			if (respawnPoint != null) {
				respawnPlayer(player, respawnPoint);
				System.out.println("[RespawnPoints] Respawning player " + player.getName().getString() + " at " + respawnPoint.toShortString());

				ServerCommandSource commandSource = player.getCommandSource();
				CommandDispatcher<ServerCommandSource> dispatcher = commandSource.getServer().getCommandManager().getDispatcher();

				try {
					dispatcher.execute("rp respawn", commandSource);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("[RespawnPoints] Executed command: /rp respawn");
			}
		});
	}


	public static void respawnPlayer(ServerPlayerEntity player, BlockPos respawnPoint) {
		ServerWorld world = (ServerWorld) player.getWorld();
		player.teleport(world, respawnPoint.getX() + 0.5, respawnPoint.getY(), respawnPoint.getZ() + 0.5, player.getYaw(), player.getPitch());
		System.out.println("[RespawnPoints] Teleporting player to: " + respawnPoint.toShortString());
	}

	private static BlockPos getRandomPointFromGroup() {
		BlockPos[] points = groups.get(activeGroup);
		if (points != null && points.length > 0) {
			Random random = new Random();
			return points[random.nextInt(points.length)];
		}
		return null;
	}

	private void saveData(ServerWorld world) {
		PersistentStateManager persistentStateManager = world.getPersistentStateManager();
		RespawnPointsData data = persistentStateManager.getOrCreate(
				RespawnPointsData::fromNbt, RespawnPointsData::new, DATA_NAME);
		data.setGroups(groups);
		data.markDirty();
		System.out.println("[RespawnPoints] Data saved for world: " + world.getRegistryKey().getValue());
	}

	private void loadData(ServerWorld world) {
		PersistentStateManager persistentStateManager = world.getPersistentStateManager();
		RespawnPointsData data = persistentStateManager.getOrCreate(
				RespawnPointsData::fromNbt, RespawnPointsData::new, DATA_NAME);

		groups.clear();
		groups.putAll(data.getGroups());
		System.out.println("[RespawnPoints] Data loaded for world: " + world.getRegistryKey().getValue());
	}
}
