package io.github.haykam821.elytron.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.Blocks;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import net.minecraft.world.gen.stateprovider.SimpleBlockStateProvider;

public record ElytronMapConfig(int x, int y, int z,
							   BlockStateProvider floorProvider,
							   BlockStateProvider wallProvider,
							   BlockStateProvider ceilingProvider) {
	public static final Codec<ElytronMapConfig> CODEC = RecordCodecBuilder.create(instance ->
		instance.group(
				Codec.INT.fieldOf("x").forGetter(ElytronMapConfig::x),
				Codec.INT.fieldOf("y").forGetter(ElytronMapConfig::y),
				Codec.INT.fieldOf("z").forGetter(ElytronMapConfig::z),
				BlockStateProvider.TYPE_CODEC.optionalFieldOf("floor_provider", new SimpleBlockStateProvider(Blocks.SPRUCE_PLANKS.getDefaultState())).forGetter(ElytronMapConfig::floorProvider),
				BlockStateProvider.TYPE_CODEC.optionalFieldOf("wall_provider", new SimpleBlockStateProvider(Blocks.STRIPPED_DARK_OAK_LOG.getDefaultState())).forGetter(ElytronMapConfig::wallProvider),
				BlockStateProvider.TYPE_CODEC.optionalFieldOf("ceiling_provider", new SimpleBlockStateProvider(Blocks.WHITE_STAINED_GLASS.getDefaultState())).forGetter(ElytronMapConfig::ceilingProvider)
		).apply(instance, ElytronMapConfig::new)
	);
}