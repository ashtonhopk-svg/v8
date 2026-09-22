package com.wildenchants;

import net.minecraft.resources.Identifier;
import com.wildenchants.net.ModNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WildEnchants implements ModInitializer {
	public static final String MOD_ID = "wild_enchants";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModEffects.register();
		FlightHandler.register();
		RadiusTracker.register();
		ModNetworking.registerCommon();
		FrostbiteTracker.register();
		TimberHandler.register();
		MagnetHandler.register();
		AfterlifeHandler.register();
		TelekinesisHandler.register();
		FallingBlockLanding.register();
		LOGGER.info("Wild Enchants loaded: Exploding, Surge, Thunderstrike, Lightning, Carrot, Tumble, Cloning, Flight, Radius, Air Jump, Dash, Fallback, Phase, Grapple, Frostbite, Backstab, Timber, Telekinesis, Reach, Magnet, Afterlife");
	}
}
