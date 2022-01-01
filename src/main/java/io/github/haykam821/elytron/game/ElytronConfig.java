package io.github.haykam821.elytron.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.elytron.game.map.ElytronMapConfig;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public record ElytronConfig(ElytronMapConfig mapConfig,
							PlayerConfig playerConfig, int height, int delay,
							int decay) {
	public static final Codec<ElytronConfig> CODEC = RecordCodecBuilder.create(instance ->
		instance.group(
				ElytronMapConfig.CODEC.fieldOf("map").forGetter(ElytronConfig::mapConfig),
				PlayerConfig.CODEC.fieldOf("players").forGetter(ElytronConfig::playerConfig),
				Codec.INT.optionalFieldOf("height", 2).forGetter(ElytronConfig::height),
				Codec.INT.optionalFieldOf("delay", 15).forGetter(ElytronConfig::delay),
				Codec.INT.optionalFieldOf("decay", 200).forGetter(ElytronConfig::decay)
		).apply(instance, ElytronConfig::new)
	);
}