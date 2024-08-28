package com.k1r1enko.shootersuite.respawnpoints;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;

public class RespawnPointsData extends PersistentState {
    private final Map<String, BlockPos[]> groups = new HashMap<>();

    public Map<String, BlockPos[]> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, BlockPos[]> groups) {
        this.groups.clear();
        this.groups.putAll(groups);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        for (Map.Entry<String, BlockPos[]> entry : groups.entrySet()) {
            NbtCompound groupTag = new NbtCompound();
            BlockPos[] positions = entry.getValue();
            for (int i = 0; i < positions.length; i++) {
                groupTag.putIntArray("pos" + i, new int[]{positions[i].getX(), positions[i].getY(), positions[i].getZ()});
            }
            nbt.put(entry.getKey(), groupTag);
        }
        return nbt;
    }

    public static RespawnPointsData fromNbt(NbtCompound nbt) {
        RespawnPointsData data = new RespawnPointsData();
        for (String groupName : nbt.getKeys()) {
            NbtCompound groupTag = nbt.getCompound(groupName);
            int size = groupTag.getKeys().size();
            BlockPos[] positions = new BlockPos[size];
            for (int i = 0; i < size; i++) {
                int[] posArray = groupTag.getIntArray("pos" + i);
                positions[i] = new BlockPos(posArray[0], posArray[1], posArray[2]);
            }
            data.groups.put(groupName, positions);
        }
        return data;
    }
}
