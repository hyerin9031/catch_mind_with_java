//TODO Host가 그림을 그렸을 때 실시간으로 플레이어가 볼 수 있게 UDP 방식으로 송수신

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DrawingServer {

    private static final int PORT = 9100;
    private static final int BUF_SIZE = 1024;

    // 그림을 받아야 하는 플레이어들의 주소 목록
    private static final Set<InetSocketAddress> clients =
            Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("[DRAW] DrawingServer 시작, PORT=" + PORT);

            byte[] buf = new byte[BUF_SIZE];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                InetSocketAddress senderAddr = new InetSocketAddress(packet.getAddress(), packet.getPort());

                if (msg.startsWith("JOIN:")) {
                    String nick = msg.substring(5).trim();
                    clients.add(senderAddr);
                    System.out.println("[DRAW] JOIN: " + nick + " / " + senderAddr);
                    continue;
                }

                if (msg.startsWith("LEAVE:")) {
                    String nick = msg.substring(6).trim();
                    clients.remove(senderAddr);
                    System.out.println("[DRAW] LEAVE: " + nick + " / " + senderAddr);
                    continue;
                }

                // 나머지는 모두 그림 관련 메시지라고 보고 그대로 브로드캐스트
                broadcast(socket, msg, senderAddr);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void broadcast(DatagramSocket socket, String msg, InetSocketAddress from) throws IOException {
        byte[] data = msg.getBytes(StandardCharsets.UTF_8);

        synchronized (clients) {
            for (InetSocketAddress addr : clients) {
                // 필요하면 자신(from)에게는 안 보내게 할 수도 있음 (지금은 전부에게 전송)
                DatagramPacket out = new DatagramPacket(data, data.length, addr.getAddress(), addr.getPort());
                socket.send(out);
            }
        }
    }
}
