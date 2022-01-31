package io.github.haykam821.elytron.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.Blocks;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public class ElytronMapConfig {
	public static final Codec<ElytronMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.INT.fieldOf("x").forGetter(ElytronMapConfig::getX),
			Codec.INT.fieldOf("y").forGetter(ElytronMapConfig::getY),
			Codec.INT.fieldOf("z").forGetter(ElytronMapConfig::getZ),
			BlockStateProvider.TYPE_CODEC.optionalFieldOf("floor_provider", BlockStateProvider.of(Blocks.SPRUCE_PLANKS)).forGetter(ElytronMapConfig::getFloorProvider),
			BlockStateProvider.TYPE_CODEC.optionalFieldOf("wall_provider", BlockStateProvider.of(Blocks.STRIPPED_DARK_OAK_LOG)).forGetter(ElytronMapConfig::getWallProvider),
			BlockStateProvider.TYPE_CODEC.optionalFieldOf("ceiling_provider", BlockStateProvider.of(Blocks.WHITE_STAINED_GLASS)).forGetter(ElytronMapConfig::getCeilingProvider)
		).apply(instance, ElytronMapConfig::new);
	});

	private final int x;
	private final int y;
	private final int z;
	private final BlockStateProvider floorProvider;
	private final BlockStateProvider wallProvider;
	private final BlockStateProvider ceilingProvider;

	public ElytronMapConfig(int x, int y, int z, BlockStateProvider floorProvider, BlockStateProvider wallProvider, BlockStateProvider ceilingProvider) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.floorProvider = floorProvider;
		this.wallProvider = wallProvider;
		this.ceilingProvider = ceilingProvider;
	}

	public int getX() {
		return this.x;
	}
	
	public int getY() {
		return this.y;
	}

	public int getZ() {
		return this.z;
	}

	public BlockStateProvider getFloorProvider() {
		return this.floorProvider;
	}

	public BlockStateProvider getWallProvider() {
		return this.wallProvider;
	}

	public BlockStateProvider getCeilingProvider() {
		return this.ceilingProvider;
	}
}