import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.io.*;

public class PlayerAnswerFrame extends JFrame {

    private JPanel mainPanel, topPanel, bottonPanel, answerPanel, roundPanel, playrtPanel, problemPanel;
    private JLabel problemLabel, playerLabel, roundLabel;
    private JTextField answerField;
    private JButton sendBtn;

    private final DrawingView drawingView;

    private Socket playerSocket;
    private BufferedReader in;
    private PrintWriter out;

    private String currentWord = ""; // 현재 문제 단어 (힌트용)

    public PlayerAnswerFrame(Socket playerSocket) {
        this.playerSocket = playerSocket;

        try {
            in = new BufferedReader(new InputStreamReader(playerSocket.getInputStream()));
            out = new PrintWriter(playerSocket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("CatchMind - 참가자");
        setSize(900, 600);
        setLocationRelativeTo(null);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        topPanel = new JPanel(new GridLayout(2, 1));

        roundLabel = new JLabel("1/10");
        problemLabel = new JLabel("_ _");
        playerLabel = new JLabel("");

        problemLabel.setHorizontalAlignment(SwingConstants.CENTER);
        problemLabel.setFont(problemLabel.getFont().deriveFont(Font.BOLD, 22f));

        playrtPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        problemPanel = new JPanel();

        playrtPanel.add(playerLabel);
        problemPanel.add(problemLabel);

        topPanel.add(playrtPanel);
        topPanel.add(problemPanel);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        drawingView = new DrawingView();
        drawingView.setBackground(Color.WHITE);
        mainPanel.add(drawingView, BorderLayout.CENTER);

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
            // 서버로 정답 전송
            out.println("ANSWER:" + answer);
            answerField.setText(""); // 입력창 초기화
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

        new Thread(new UdpReceiver()).start();
        new Thread(new TcpReceiver()).start(); // 서버 메시지 수신
    }

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

        public void clear() {
            if (canvas != null && g2 != null) {
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (canvas != null) {
                g.drawImage(canvas, 0, 0, null);
            }
        }
    }

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
                    }else if (msg.equals("CLEAR")) {
                        // 캔버스 초기화
                        SwingUtilities.invokeLater(() -> drawingView.clear());
                    }
                }

            } catch (Exception e) {
                System.out.println("UDP 수신 오류: " + e.getMessage());
            }
        }
    }

    // TCP 메시지 수신 (서버로부터 게임 상태 업데이트)
    private class TcpReceiver implements Runnable {
        @Override
        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("[PLAYER] 수신: " + msg);

                    if (msg.startsWith("HINT:")) {
                        // 힌트 업데이트 (예: "HINT:_ _ _")
                        String hint = msg.substring(5);
                        SwingUtilities.invokeLater(() -> problemLabel.setText(hint));
                    } else if (msg.startsWith("ROUND:")) {
                        // 라운드 업데이트 (예: "ROUND:3")
                        String round = msg.substring(6);
                        SwingUtilities.invokeLater(() -> roundLabel.setText(round + "/10"));
                    } else if (msg.startsWith("SCORES:")) {
                        // 점수 업데이트 (예: "SCORES:Player1:10,Player2:5")
                        String scores = msg.substring(7);
                        SwingUtilities.invokeLater(() -> {
                            StringBuilder sb = new StringBuilder("<html>");
                            for (String entry : scores.split(",")) {
                                sb.append(entry.replace(":", ": ")).append("<br>");
                            }
                            sb.append("</html>");
                            playerLabel.setText(sb.toString());
                        });
                    } else if (msg.equals("CORRECT")) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(PlayerAnswerFrame.this, "정답입니다!");
                            answerField.setEnabled(false); // 정답 맞춘 사람만 입력 비활성화
                        });
                    } else if (msg.startsWith("NEW_ROUND")) {
                        // 새 라운드 시작 시 입력창 다시 활성화 및 그림 지우기
                        SwingUtilities.invokeLater(() -> {
                            answerField.setEnabled(true);
                            answerField.setText("");
                            drawingView.clear(); // 그림 지우기
                        });
                    } else if (msg.equals("GAME_END")) {
                        // 최종 점수를 받을 시간을 주기 위해 약간 대기
                        Thread.sleep(500);
                        SwingUtilities.invokeLater(() -> {
                            new PlayerScoreFrame(playerSocket, playerLabel.getText());
                            dispose();
                        });
                    }
                }
            } catch (Exception e) {
                System.out.println("[PLAYER] TCP 수신 종료");
            }
        }
    }

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 7400);
            new PlayerAnswerFrame(socket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}