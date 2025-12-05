package termproject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class PlayerAnswerFrame extends JFrame {

    private JPanel mainPanel, topPanel, bottonPanel, answerPanel, roundPanel, playrtPanel, problemPanel;
    private JLabel problemLabel, playerLabel, roundLabel;
    private JTextField answerField;
    private JButton sendBtn;

    private final DrawingView drawingView; // 수정된 그리기 패널

    public PlayerAnswerFrame() {
        setTitle("CatchMind - 참가자");
        setSize(900, 600);
        setLocationRelativeTo(null);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        // 상단 정보 영역
        topPanel = new JPanel(new GridLayout(2, 1));

        roundLabel = new JLabel("1/10");
        problemLabel = new JLabel("_ _");
        playerLabel = new JLabel("P1 : 10");

        problemLabel.setHorizontalAlignment(SwingConstants.CENTER);
        problemLabel.setFont(problemLabel.getFont().deriveFont(Font.BOLD, 22f));

        playrtPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        problemPanel = new JPanel();

        playrtPanel.add(playerLabel);
        problemPanel.add(problemLabel);

        topPanel.add(playrtPanel);
        topPanel.add(problemPanel);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // ---------- 🎨 출제자 그림 수신 + 렌더링 화면 ----------
        drawingView = new DrawingView();
        drawingView.setBackground(Color.WHITE);
        mainPanel.add(drawingView, BorderLayout.CENTER);

        // ---------- 정답 입력 영역 ----------
        bottonPanel = new JPanel();
        bottonPanel.setLayout(new BoxLayout(bottonPanel, BoxLayout.Y_AXIS));

        roundPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        roundPanel.add(roundLabel);

        answerPanel = new JPanel();
        answerField = new JTextField(30);
        sendBtn = new JButton("정답");

        sendBtn.addActionListener(e -> {
            String answer = answerField.getText().trim();
            if (answer.isEmpty()) {
                JOptionPane.showMessageDialog(this, "정답을 입력해주세요.");
                return;
            }
            // TODO 서버로 정답 전송
        });

        answerField.addActionListener(e -> sendBtn.doClick());

        answerPanel.add(answerField);
        answerPanel.add(sendBtn);

        bottonPanel.add(roundPanel);
        bottonPanel.add(answerPanel);

        mainPanel.add(bottonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 🎉 출제자 그림 수신 스레드 시작
        new Thread(new UdpReceiver()).start();
    }


    // ============================================
    //  🎨 출제자로부터 받은 그림 데이터를 표시하는 Panel
    // ============================================
    private static class DrawingView extends JPanel {

        private BufferedImage canvas;
        private Graphics2D g2;

        public DrawingView() {}

        private void initCanvas() {
            if (canvas == null) {
                canvas = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                g2 = canvas.createGraphics();
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
        }

        public void drawFromHost(int x1, int y1, int x2, int y2,
                                 int r, int g, int b, int stroke) {

            if (g2 == null) initCanvas();

            g2.setStroke(new BasicStroke(stroke));
            g2.setColor(new Color(r, g, b));
            g2.drawLine(x1, y1, x2, y2);

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (canvas != null) {
                g.drawImage(canvas, 0, 0, null);
            }
        }
    }


    // ===========================================================
    //  📡 출제자(HostDrawFrame) → 참가자(PlayerAnswerFrame)
    //     UDP 패킷을 받아서 그림을 그리는 스레드
    // ===========================================================
    private class UdpReceiver implements Runnable {

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket(9001)) {
                byte[] buf = new byte[1024];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);

                    String msg = new String(packet.getData(), 0, packet.getLength());

                    // 패킷 형식:
                    // DRAW x1 y1 x2 y2 r g b stroke
                    if (msg.startsWith("DRAW")) {
                        String[] parts = msg.split(" ");

                        int x1 = Integer.parseInt(parts[1]);
                        int y1 = Integer.parseInt(parts[2]);
                        int x2 = Integer.parseInt(parts[3]);
                        int y2 = Integer.parseInt(parts[4]);
                        int r = Integer.parseInt(parts[5]);
                        int g = Integer.parseInt(parts[6]);
                        int b = Integer.parseInt(parts[7]);
                        int stroke = Integer.parseInt(parts[8]);

                        drawingView.drawFromHost(x1, y1, x2, y2, r, g, b, stroke);
                    }
                }

            } catch (Exception e) {
                System.out.println("UDP 수신 오류: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        new PlayerAnswerFrame();
    }
}
