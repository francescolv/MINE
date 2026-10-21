package io.github.francescolv.mine.board;

import net.minecraft.world.phys.Vec3;

/**
 * Where a look meets a board's text, as Minecraft draws a text display: centred on its position
 * and growing upwards, facing where the display looks, {@value #PIXEL} blocks per pixel of text
 * times the board's size. MINE's client uses it to click a board wherever its text is, not only
 * on the thin box in the middle.
 */
public final class BoardHit {

	/** Blocks per pixel of text, at size 1: as Minecraft draws text displays. */
	static final float PIXEL = 0.025f;
	private static final int LINE_HEIGHT = 9;

	private BoardHit() {
	}

	/**
	 * How far along {@code look} (a unit vector from {@code eye}) the text is hit, or {@code -1}
	 * if it isn't, or is farther than {@code reach}.
	 *
	 * @param yaw        the display's yaw, in degrees
	 * @param widthPx    the width of the widest line, in pixels of text
	 * @param lines      how many lines the text takes
	 */
	public static double distance(Vec3 eye, Vec3 look, Vec3 origin, float yaw, float size, int widthPx, int lines, double reach) {
		double radians = Math.toRadians(yaw);
		Vec3 normal = new Vec3(-Math.sin(radians), 0, Math.cos(radians));
		Vec3 right = new Vec3(Math.cos(radians), 0, Math.sin(radians));
		double towards = look.dot(normal);
		if (Math.abs(towards) < 1e-6) {
			return -1;
		}
		double along = origin.subtract(eye).dot(normal) / towards;
		if (along <= 0 || along > reach) {
			return -1;
		}
		Vec3 hit = eye.add(look.scale(along)).subtract(origin);
		double pixel = PIXEL * size;
		// The background reaches one pixel around the text.
		double across = Math.abs(hit.dot(right)) / pixel;
		double up = hit.y / pixel;
		return across <= widthPx / 2.0 + 1 && up >= -1 && up <= lines * LINE_HEIGHT + 1 ? along : -1;
	}
}
