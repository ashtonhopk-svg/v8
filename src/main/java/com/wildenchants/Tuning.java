package com.wildenchants;

/**
 * Behaviour switches in one place. Radii / cost / weight are in the enchantment JSON files under
 * data/wild_enchants/enchantment/ (so they can also be changed with a datapack).
 */
public final class Tuning {
	private Tuning() {}

	/** Exploding: the blast breaks blocks like a TNT explosion. (false = only hurts/knocks back entities) */
	public static final boolean EXPLOSION_BREAKS_BLOCKS = true;

	/** Exploding: the person holding the weapon takes no damage from their own blast. */
	public static final boolean EXPLOSION_SPARES_WIELDER = true;

	/**
	 * Thunderstrike / Lightning: the person holding the weapon is not zapped by their own bolt.
	 * (With true the bolt is "visual only" and shocks everything nearby except the wielder; the
	 * vanilla ground fire of a real bolt is therefore not created.)
	 */
	public static final boolean LIGHTNING_SPARES_WIELDER = true;

	/** Tumble: only triggers on a real mace smash attack (falling attack), not on every hit with the mace. */
	public static final boolean TUMBLE_REQUIRES_SMASH = true;

	/** Tumble + Wind Burst: blocks are launched. Horizontal speed = upward speed * this ratio, outwards from the target. */
	public static final double TUMBLE_LAUNCH_HORIZONTAL_RATIO = 0.4D;

	/** Tumble + Wind Burst: each block's launch speed varies by up to +/- this fraction (0.25 = 25%). */
	public static final double TUMBLE_LAUNCH_RANDOMNESS = 0.25D;

	/** Tumble: safety cap on how many blocks can be turned into falling blocks by one hit (nearest first). */
	public static final int TUMBLE_MAX_BLOCKS = 3000;

	/** Flight: the chestplate loses this much durability every FLIGHT_DURABILITY_INTERVAL_TICKS while you are actually flying (survival/adventure only). */
	public static final int FLIGHT_DURABILITY_COST = 1;
	public static final int FLIGHT_DURABILITY_INTERVAL_TICKS = 20;

	/** Radius: entities tamed by the wielder (dogs, cats, ...) are not hit by the repeated hits. */
	public static final boolean RADIUS_SPARES_OWN_PETS = true;

	/** Air Jump: upward speed of an air jump (a normal jump is 0.42). */
	public static final double AIR_JUMP_VELOCITY = 0.42D;

	/** Dash / Fallback: two presses of the key within this many ticks (20 ticks = 1 second) count as a double-tap. */
	public static final int DOUBLE_TAP_WINDOW_TICKS = 7;

	/** Dash push (blocks per tick) = base + perLevel * level -> 0.5 / 0.8 / 1.1, roughly 5 / 9 / 12 blocks of travel. */
	public static final double DASH_BASE_STRENGTH = 0.2D;
	public static final double DASH_STRENGTH_PER_LEVEL = 0.3D;

	/** Fallback push, same idea as Dash but backwards. */
	public static final double FALLBACK_BASE_STRENGTH = 0.2D;
	public static final double FALLBACK_STRENGTH_PER_LEVEL = 0.3D;

	/** Fallback: also allow the backwards push while standing on the ground (Dash is always air-only). */
	public static final boolean FALLBACK_WORKS_ON_GROUND = false;

	/** Falling blocks that would break and drop when landing awkwardly instead land on the nearest valid spot. */
	public static final boolean LANDING_FIX_ALL_FALLING_BLOCKS = true; // false = only blocks made by Tumble
	public static final int LANDING_SEARCH_RADIUS = 3;

	/** Phase: how fast you slip through a phaseable block while the key is held (blocks/tick). */
	public static final double PHASE_SPEED = 0.2D;

	/** Grapple: how far (blocks) it can pull you to, and how hard the initial yank is. */
	public static final double GRAPPLE_MAX_RANGE = 24.0D;
	public static final double GRAPPLE_PULL_STRENGTH = 1.6D;

	/** Frostbite: how many ticks one stack of slow lasts before expiring, and the freeze threshold. */
	public static final int FROSTBITE_STACK_DURATION_TICKS = 60;
	public static final int FROSTBITE_STACKS_TO_FREEZE = 5;
	public static final int FROSTBITE_FREEZE_DURATION_TICKS = 60;
	public static final double FROSTBITE_ICE_RADIUS = 2.0D;

	/** Backstab: victim must be facing away from the attacker by at least this many degrees to count as "not looking at you". */
	public static final double BACKSTAB_ANGLE_THRESHOLD_DEGREES = 100.0D;
	public static final float BACKSTAB_BONUS_BASE = 1.0F;
	public static final float BACKSTAB_BONUS_PER_LEVEL_ABOVE_FIRST = 0.5F;

	/** Timber: safety cap on how many connected log blocks one chop can break. */
	public static final int TIMBER_MAX_LOGS = 256;

	/** Telekinesis: max lock-on range, and how quickly the held block/entity catches up to your crosshair (0-1 per tick). */
	public static final double TELEKINESIS_MAX_RANGE = 32.0D;
	public static final double TELEKINESIS_FOLLOW_SPEED = 0.35D;

	/** Magnet: how far (blocks) it reaches, and the pull strength per level at 1 block away (falls off with distance). */
	public static final double MAGNET_MAX_RANGE = 6.0D;
	public static final double MAGNET_STRENGTH_PER_LEVEL = 0.10D;

	/** Afterlife: how long the ghost's invisibility effect lasts before being silently refreshed (keeps it permanent). */
	public static final int AFTERLIFE_EFFECT_REFRESH_TICKS = 200;

	/** Afterlife: same movement-speed cap as Phase, used for ghost phasing while flying. */
	public static final double AFTERLIFE_GHOST_PHASE_SPEED = 0.2D;

	/** Cloning: how many copies each kill creates, and their size relative to the killed entity. */
	public static final int CLONES_PER_KILL = 2;
	public static final double CLONE_SCALE_FACTOR = 0.5D;
}
