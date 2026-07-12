package io.github.haykam821.elytron.game.map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapChunk;
import xyz.nucleoid.plasmid.api.game.level.generator.GameChunkGenerator;

import java.util.concurrent.CompletableFuture;

public class ElytronMapGenerator extends GameChunkGenerator {
    private final ElytronMapConfig config;
    private final BlockBounds bounds;

    public ElytronMapGenerator(ElytronMapConfig config, MinecraftServer server) {
        super(createBiomeSource(server, Biomes.THE_VOID));
        this.config = config;
        this.bounds = BlockBounds.of(BlockPos.ZERO, new BlockPos(config.getX(), config.getY(), config.getZ()));
    }
    private BlockState getBlockState(BlockPos pos, WorldGenLevel level) {
        RandomSource random = level.getRandom();
        BlockBounds bounds = this.bounds;

        int layer = pos.getY() - bounds.min().getY();
        if (layer == 0) return config.getFloorProvider().getState(level, random, pos);
        if (layer == bounds.max().getY()) return config.getCeilingProvider().getState(level, random, pos);

        if (pos.getX() == bounds.min().getX() || pos.getX() == bounds.max().getX() || pos.getZ() == bounds.min().getZ() || pos.getZ() == bounds.max().getZ()) {
            return config.getWallProvider().getState(level, random, pos);
        }
        return null;
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureAccessor) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos min = new BlockPos(chunkPos.x() * 16, bounds.min().getY(), chunkPos.z() * 16);
        BlockPos max = new BlockPos(chunkPos.x() * 16 + 15, bounds.max().getY(), chunkPos.z() * 16 + 15);

        BlockBounds chunkBounds = new BlockBounds(min, max);
        if (bounds.intersects(chunkBounds)) {
            for (BlockPos pos : chunkBounds) {
                if (!bounds.contains(pos)) {
                    continue;
                }

                BlockState state = this.getBlockState(pos, level);
                if (state == null) {
                    continue;
                }

                chunk.setBlockState(pos, state);
            }
        }
    }
}
