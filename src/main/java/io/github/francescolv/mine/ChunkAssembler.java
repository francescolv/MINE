package io.github.francescolv.mine;

import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * A file arriving in pieces (a photo from the camera, an export for the teacher): the pieces must
 * come in order, all saying the same number of pieces, and together no bigger than {@code max}.
 * Anything else drops what has arrived so far.
 */
public final class ChunkAssembler {

	private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
	private final int total;
	private int next;

	private ChunkAssembler(int total) {
		this.total = total;
	}

	/**
	 * Adds piece {@code index} of {@code total} to the file kept in {@code arriving} under
	 * {@code key}: returns the whole file once its last piece has arrived, {@code null} until then
	 * (or if the piece doesn't fit, which drops the file).
	 */
	@Nullable
	public static <K> byte[] receive(Map<K, ChunkAssembler> arriving, K key, int index, int total, byte[] piece, int max) {
		ChunkAssembler file = index == 0 ? new ChunkAssembler(total) : arriving.get(key);
		if (file == null || total < 1 || index != file.next || total != file.total || file.bytes.size() + piece.length > max) {
			arriving.remove(key);
			return null;
		}
		file.bytes.writeBytes(piece);
		file.next++;
		if (file.next < total) {
			arriving.put(key, file);
			return null;
		}
		arriving.remove(key);
		return file.bytes.toByteArray();
	}
}
