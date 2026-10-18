package io.github.francescolv.mine.classroom;

import io.github.francescolv.mine.MineMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The school server announces itself on the school network as an "Open to LAN" world does, so it
 * appears by itself in the students' Multiplayer list: nobody has to type its address. Only for a
 * dedicated server; the same message and address Minecraft uses (multicast 224.0.2.60:4445).
 *
 * <p>The same message also goes to each network's broadcast address, on the same port: the game
 * listens there for any packet, and a broadcast arrives where multicast often doesn't (Wi-Fi
 * routers, Windows PCs that listen for multicast on a virtual network card, e.g. a VPN's).
 */
public final class LanAnnouncer {

	private static final String GROUP = "224.0.2.60";
	private static final int PORT = 4445;
	private static final long EVERY_MS = 1500;

	private static Thread thread;

	private LanAnnouncer() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(LanAnnouncer::start);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> stop());
	}

	private static synchronized void start(MinecraftServer server) {
		// Only a server students can join: not the test server, which has no port.
		if (!server.isDedicatedServer() || server.getPort() <= 0 || thread != null) {
			return;
		}
		thread = new Thread(() -> announce(server), "MINE LAN announcer");
		thread.setDaemon(true);
		thread.start();
		MineMod.LOGGER.info("[MINE] Announcing the server on the local network: it appears in the students' Multiplayer list");
	}

	private static synchronized void stop() {
		if (thread != null) {
			thread.interrupt();
			thread = null;
		}
	}

	private static void announce(MinecraftServer server) {
		try (DatagramSocket socket = new DatagramSocket()) {
			socket.setBroadcast(true);
			InetAddress group = InetAddress.getByName(GROUP);
			while (!Thread.currentThread().isInterrupted()) {
				byte[] message = ("[MOTD]" + server.getMotd() + "[/MOTD][AD]" + server.getPort() + "[/AD]")
						.getBytes(StandardCharsets.UTF_8);
				List<InetAddress> to = new ArrayList<>(broadcasts());
				to.add(group);
				for (InetAddress address : to) {
					try {
						socket.send(new DatagramPacket(message, message.length, address, PORT));
					} catch (IOException e) {
						// No network for now (e.g. a cable unplugged): try again at the next round.
					}
				}
				Thread.sleep(EVERY_MS);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			MineMod.LOGGER.warn("[MINE] Can't announce the server on the local network: {}", e.getMessage());
		}
	}

	/** The broadcast address of each network the server is on now (Wi-Fi and cable can come and go). */
	private static List<InetAddress> broadcasts() {
		List<InetAddress> addresses = new ArrayList<>();
		try {
			for (NetworkInterface network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				if (!network.isUp() || network.isLoopback()) {
					continue;
				}
				for (InterfaceAddress address : network.getInterfaceAddresses()) {
					if (address.getBroadcast() != null) {
						addresses.add(address.getBroadcast());
					}
				}
			}
		} catch (SocketException e) {
			// Only multicast this round.
		}
		return addresses;
	}
}
