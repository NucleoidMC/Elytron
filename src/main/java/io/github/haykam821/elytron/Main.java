package io.github.haykam821.elytron;

import java.util.ArrayList;
import java.util.List;

import io.github.haykam821.elytron.game.ElytronConfig;
import io.github.haykam821.elytron.game.phase.ElytronWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;

public class Main implements ModInitializer {
	private static final String MOD_ID = "elytron";

	private static final List<Block> TRAIL_BLOCKS = new ArrayList<>();

	private static final Identifier ELYTRON_ID = Main.identifier("elytron");
	public static final GameType<ElytronConfig> ELYTRON_TYPE = GameTypes.register(ELYTRON_ID, ElytronConfig.CODEC, ElytronWaitingPhase::open);

	@Override
	public void onInitialize() {
		return;
	}

	public static boolean isTrailBlock(Block block) {
		return TRAIL_BLOCKS.contains(block);
	}

	public static Block getTrailBlock(int index) {
		return TRAIL_BLOCKS.get(index % TRAIL_BLOCKS.size());
	}

	public static Identifier identifier(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	static {
		TRAIL_BLOCKS.add(Blocks.STAINED_GLASS.red());
		TRAIL_BLOCKS.add(Blocks.STAINED_GLASS.blue());
		TRAIL_BLOCKS.add(Blocks.STAINED_GLASS.yellow());
		TRAIL_BLOCKS.add(Blocks.STAINED_GLASS.lime());
		TRAIL_BLOCKS.add(Blocks.STAINED_GLASS.purple());
		TRAIL_BLOCKS.add(Blocks.STAINED_GLASS.orange());
		TRAIL_BLOCKS.add(Blocks.STAINED_GLASS.blue());
		TRAIL_BLOCKS.add(Blocks.STAINED_GLASS.pink());
		TRAIL_BLOCKS.add(Blocks.STAINED_GLASS.magenta());
	}
}