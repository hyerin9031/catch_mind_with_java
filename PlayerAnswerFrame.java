import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.InetAddress;

public class PlayerAnswerFrame extends JFrame {

    private JPanel mainPanel, topPanel, bottonPanel, answerPanel, roundPanel, playrtPanel, problemPanel;
    private JLabel problemLabel, playerLabel, roundLabel;
    private JTextField answerField;
    private JButton sendBtn;
    private JDialog messageDialog;  // 현재 떠 있는 메시지 창 (정답/안내)

    private final DrawingView drawingView;

    private DatagramSocket udpSocket;
    private Socket playerSocket;
    private BufferedReader in;
    private PrintWriter out;

    private String nickname;
    private String currentWord = ""; // 현재 문제 단어 (힌트용)

    public PlayerAnswerFrame(Socket playerSocket, String nickname) {
        this.playerSocket = playerSocket;
        this.nickname = nickname;

        try {
            in = new BufferedReader(new InputStreamReader(playerSocket.getInputStream()));
            out = new PrintWriter(playerSocket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("CatchMind - 참가자");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setResizable(false);

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

        try {
            udpSocket = new DatagramSocket(); // 포트 지정X → OS가 랜덤 포트 배정
            new Thread(new UdpReceiver()).start();

            // 닉네임이 있다면 JOIN 보내기
            String joinMsg = "JOIN:" + nickname;   // nickname은 네가 이미 가지고 있는 플레이어 이름
            byte[] data = joinMsg.getBytes(StandardCharsets.UTF_8);
            InetAddress serverAddr = InetAddress.getByName("localhost"); // DrawingServer가 있는 곳
            DatagramPacket p = new DatagramPacket(data, data.length, serverAddr, 9100);
            udpSocket.send(p);
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Thread(new TcpReceiver()).start(); // 서버 메시지 수신
    }

    private static class DrawingView extends JPanel {

        private BufferedImage canvas;
        private Graphics2D g2;

        public DrawingView() {
            setPreferredSize(new Dimension(600, 400));
            // 패널 리사이즈 시 캔버스도 재조정
            addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    ensureCanvasSize();
                    repaint();
                }
            });
        }

        private void ensureCanvasSize() {
            int w = Math.max(1, getWidth());
            int h = Math.max(1, getHeight());
            if (w <= 0 || h <= 0) return;

            if (canvas == null || canvas.getWidth() != w || canvas.getHeight() != h) {
                BufferedImage newImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gNew = newImg.createGraphics();
                gNew.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 배경 흰색
                gNew.setColor(Color.WHITE);
                gNew.fillRect(0, 0, w, h);

                // 기존 그림 복사
                if (canvas != null) {
                    gNew.drawImage(canvas, 0, 0, null);
                    g2.dispose();
                }

                canvas = newImg;
                g2 = gNew;
            }
        }

        public void drawFromHost(int x1, int y1, int x2, int y2,
                                 int r, int g, int b, int stroke) {

            ensureCanvasSize();

            if (g2 == null) return;
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
            try {
                byte[] buf = new byte[1024];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    udpSocket.receive(packet);

                    String msg = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);

                    if (msg.startsWith("DRAW")) {
                        String[] parts = msg.split(" ");
                        if (parts.length >= 9) {
                            int x1 = Integer.parseInt(parts[1]);
                            int y1 = Integer.parseInt(parts[2]);
                            int x2 = Integer.parseInt(parts[3]);
                            int y2 = Integer.parseInt(parts[4]);
                            int r  = Integer.parseInt(parts[5]);
                            int g  = Integer.parseInt(parts[6]);
                            int b  = Integer.parseInt(parts[7]);
                            int stroke = Integer.parseInt(parts[8]);

                            SwingUtilities.invokeLater(() ->
                                    drawingView.drawFromHost(x1, y1, x2, y2, r, g, b, stroke)
                            );
                        }
                    } else if ("CLEAR".equals(msg)) {
                        SwingUtilities.invokeLater(() -> drawingView.clear());
                    }
                }
            } catch (IOException e) {
                System.out.println("UDP 수신 종료: " + e.getMessage());
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
                            // 기존 팝업 있으면 먼저 닫기
                            if (messageDialog != null && messageDialog.isShowing()) {
                                messageDialog.dispose();
                            }

                            JOptionPane pane = new JOptionPane(
                                    "정답입니다!\n호스트가 다음 라운드를 시작하면 게임이 계속됩니다.",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                            messageDialog = pane.createDialog(PlayerAnswerFrame.this, "정답");
                            messageDialog.setModal(false);  // 모달 X, 그냥 정보 알림
                            messageDialog.setVisible(true);

                            answerField.setEnabled(false); // 정답 맞춘 사람 입력 비활성화
                        });
                    } else if (msg.startsWith("CORRECT_PLAYER:")) {
                        String[] parts = msg.substring(15).split(":");
                        if (parts.length >= 2) {
                            String correctPlayer = parts[0];
                            String correctAnswer = parts[1];
                            SwingUtilities.invokeLater(() -> {
                                if (messageDialog != null && messageDialog.isShowing()) {
                                    messageDialog.dispose();
                                }

                                String text =
                                        correctPlayer + "님이 정답을 맞췄습니다!\n" +
                                                "정답: " + correctAnswer + "\n" +
                                                "호스트가 다음 라운드를 시작하면 게임이 계속됩니다.";

                                JOptionPane pane = new JOptionPane(
                                        text,
                                        JOptionPane.INFORMATION_MESSAGE
                                );
                                messageDialog = pane.createDialog(PlayerAnswerFrame.this, "정답 안내");
                                messageDialog.setModal(false);
                                messageDialog.setVisible(true);
                            });
                        }
                    } else if (msg.startsWith("NEW_ROUND")) {
                        SwingUtilities.invokeLater(() -> {
                            // 떠 있는 안내창 있으면 강제로 닫기
                            if (messageDialog != null && messageDialog.isShowing()) {
                                messageDialog.dispose();
                                messageDialog = null;
                            }

                            answerField.setEnabled(true);
                            answerField.setText("");
                            drawingView.clear(); // 그림 지우기
                        });
                    }
                    else if (msg.equals("GAME_END")) {
                        // 최종 점수를 받을 시간을 주기 위해 약간 대기
                        Thread.sleep(500);
                        SwingUtilities.invokeLater(() -> {
                            new PlayerScoreFrame(playerSocket, playerLabel.getText());
                            dispose();
                        });
                    } else if (msg.equals("SHUTDOWN")) {
                        // 호스트가 게임 종료
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(PlayerAnswerFrame.this,
                                    "호스트가 게임을 종료했습니다.");
                            try {
                                if (playerSocket != null && !playerSocket.isClosed()) {
                                    playerSocket.close();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            System.exit(0);
                        });
                    }
                }
            } catch (Exception e) {
                System.out.println("[PLAYER] TCP 수신 종료");
            }
        }
    }
}