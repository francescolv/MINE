package io.github.francescolv.mine.classroom;

import io.github.francescolv.mine.MineMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * The class code on the network. While joining (before the world): the server asks a student's
 * MINE for the code ({@link Ask}), the student answers ({@link Answer}). In the game: a teacher's
 * MINE shows the code large, to put on the classroom screen ({@link Show}).
 */
public final class ClassCodePayloads {

	private ClassCodePayloads() {
	}

	/** Type the class code; {@code wrong}: the last one was wrong. */
	public record Ask(int triesLeft, boolean wrong) implements CustomPacketPayload {
		public static final Type<Ask> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "class_code_ask"));
		public static final StreamCodec<io.netty.buffer.ByteBuf, Ask> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Ask::triesLeft, ByteBufCodecs.BOOL, Ask::wrong, Ask::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/** The symbols clicked, in order (indexes in {@link ClassCode#SYMBOLS}); none: the student gave up. */
	public record Answer(List<Integer> symbols) implements CustomPacketPayload {
		public static final Type<Answer> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "class_code_answer"));
		public static final StreamCodec<io.netty.buffer.ByteBuf, Answer> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(ClassCode.LENGTH)), Answer::symbols, Answer::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	/** Shows the code to the teacher; {@code closesAt}: epoch milliseconds; {@code world}: "" for the main world. */
	public record Show(List<Integer> symbols, long closesAt, String world) implements CustomPacketPayload {
		public static final Type<Show> ID = new Type<>(Identifier.fromNamespaceAndPath(MineMod.MOD_ID, "class_code_show"));
		public static final StreamCodec<io.netty.buffer.ByteBuf, Show> CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(ClassCode.LENGTH)), Show::symbols,
				ByteBufCodecs.VAR_LONG, Show::closesAt,
				ByteBufCodecs.stringUtf8(16), Show::world, Show::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	static void register() {
		PayloadTypeRegistry.clientboundConfiguration().register(Ask.ID, Ask.CODEC);
		PayloadTypeRegistry.serverboundConfiguration().register(Answer.ID, Answer.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Show.ID, Show.CODEC);
	}
}
