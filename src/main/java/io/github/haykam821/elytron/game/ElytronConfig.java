package io.github.haykam821.elytron.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.elytron.game.map.ElytronMapConfig;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;

public class ElytronConfig {
	public static final Codec<ElytronConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			ElytronMapConfig.CODEC.fieldOf("map").forGetter(ElytronConfig::getMapConfig),
			PlayerConfig.CODEC.fieldOf("players").forGetter(ElytronConfig::getPlayerConfig),
			Codec.INT.optionalFieldOf("height", 2).forGetter(ElytronConfig::getHeight),
			Codec.INT.optionalFieldOf("delay", 15).forGetter(ElytronConfig::getDelay),
			Codec.INT.optionalFieldOf("decay", 200).forGetter(ElytronConfig::getDecay)
		).apply(instance, ElytronConfig::new);
	});

	private final ElytronMapConfig mapConfig;
	private final PlayerConfig playerConfig;
	private final int height;
	private final int delay;
	private final int decay;

	public ElytronConfig(ElytronMapConfig mapConfig, PlayerConfig playerConfig, int height, int delay, int decay) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
		this.height = height;
		this.delay = delay;
		this.decay = decay;
	}

	public ElytronMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public int getHeight() {
		return this.height;
	}

	public int getDelay() {
		return this.delay;
	}

	public int getDecay() {
		return this.decay;
	}
}