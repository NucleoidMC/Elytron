package io.github.haykam821.elytron.game.map;

import io.github.haykam821.elytron.game.ElytronConfig;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;

public class ElytronMapBuilder {
	private final ElytronConfig config;

	public ElytronMapBuilder(ElytronConfig config) {
		this.config = config;
	}

	public ElytronMap create(Random random) {
		MapTemplate template = MapTemplate.createEmpty();
		ElytronMapConfig mapConfig = this.config.getMapConfig();

		BlockBounds bounds = BlockBounds.of(BlockPos.ORIGIN, new BlockPos(mapConfig.getX(), mapConfig.getY(), mapConfig.getZ()));
		this.build(bounds, template, mapConfig, random);

		return new ElytronMap(template, bounds);
	}

	private BlockState getBlockState(BlockPos pos, BlockBounds bounds, ElytronMapConfig mapConfig, Random random) {
		int layer = pos.getY() - bounds.min().getY();
		if (layer == 0) return mapConfig.getFloorProvider().get(random, pos);
		if (layer == bounds.max().getY()) return mapConfig.getCeilingProvider().get(random, pos);

		if (pos.getX() == bounds.min().getX() || pos.getX() == bounds.max().getX() || pos.getZ() == bounds.min().getZ() || pos.getZ() == bounds.max().getZ()) {
			return mapConfig.getWallProvider().get(random, pos);
		}
		return null;
	}

	public void build(BlockBounds bounds, MapTemplate template, ElytronMapConfig mapConfig, Random random) {
		for (BlockPos pos : bounds) {
			BlockState state = this.getBlockState(pos, bounds, mapConfig, random);
			if (state != null) {
				template.setBlockState(pos, state);
			}
		}
	}
}