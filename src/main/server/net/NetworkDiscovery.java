package main.server.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public final class NetworkDiscovery {
    public static final int DISCOVERY_PORT = 5556;
    private static final String PING = "AUCTION_PING";
    private static final String PONG = "AUCTION_PONG";
    private static final int TIMEOUT_MS = 2000;

    private NetworkDiscovery() {}

    /**
     * Gửi UDP broadcast để tìm server đang chạy trên LAN.
     * Trả về IP của server nếu tìm thấy, null nếu không tìm thấy.
     */
    public static String findServerOnNetwork() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setSoTimeout(TIMEOUT_MS);
            byte[] sendData = PING.getBytes(StandardCharsets.UTF_8);

            // Broadcast tới 255.255.255.255
            try {
                DatagramPacket packet = new DatagramPacket(
                        sendData, sendData.length,
                        InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT);
                socket.send(packet);
            } catch (IOException ignored) {}

            // Broadcast tới từng địa chỉ broadcast của các network interface
            try {
                for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    if (ni == null || !ni.isUp() || ni.isLoopback()) continue;
                    for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        InetAddress broadcast = ia.getBroadcast();
                        if (broadcast == null) continue;
                        try {
                            DatagramPacket packet = new DatagramPacket(
                                    sendData, sendData.length, broadcast, DISCOVERY_PORT);
                            socket.send(packet);
                        } catch (IOException ignored) {}
                    }
                }
            } catch (IOException ignored) {}

            // Chờ phản hồi
            byte[] buffer = new byte[64];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            String msg = new String(response.getData(), 0, response.getLength(), StandardCharsets.UTF_8);
            if (PONG.equals(msg)) {
                String serverIp = response.getAddress().getHostAddress();
                System.out.println("[Discovery] Tim thay server tai: " + serverIp);
                return serverIp;
            }
        } catch (IOException ignored) {
            // Hết timeout hoặc lỗi mạng → không tìm thấy server
        }
        System.out.println("[Discovery] Khong tim thay server tren mang, se khoi dong server cuc bo.");
        return null;
    }

    /**
     * Khởi động UDP responder để client trên LAN có thể tìm thấy server này.
     * Server sẽ tự động trả lời PONG khi nhận được PING.
     */
    public static Thread startDiscoveryResponder() {
        Thread thread = new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT)) {
                System.out.println("[Discovery] Discovery responder da san sang tren UDP port " + DISCOVERY_PORT);
                byte[] buffer = new byte[64];
                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    if (PING.equals(msg)) {
                        byte[] responseData = PONG.getBytes(StandardCharsets.UTF_8);
                        socket.send(new DatagramPacket(
                                responseData, responseData.length,
                                packet.getAddress(), packet.getPort()));
                    }
                }
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    System.err.println("[Discovery] Discovery responder da dung: " + e.getMessage());
                }
            }
        }, "auction-discovery-responder");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}
