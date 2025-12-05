import javax.swing.*;
import java.awt.*;
import java.net.Socket;
import java.io.*;

public class PlayerScoreFrame extends JFrame{
    private JPanel mainPanel, bottom, centerPanel;
    private JButton exitBtn;
    private JLabel title;
    private JTextArea scoreArea;

    private Socket playerSocket;
    private BufferedReader in;
    private boolean restart = false;

    public PlayerScoreFrame(Socket playerSocket, String finalScores) {
        this.playerSocket = playerSocket;

        setTitle("CatchMind - 최종 점수");
        setSize(900, 600);
        setLocationRelativeTo(null);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        title = new JLabel("최종 점수", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));

        scoreArea = new JTextArea();
        scoreArea.setFont(scoreArea.getFont().deriveFont(Font.BOLD, 20f));
        scoreArea.setEditable(false);

        // HTML 태그 제거하고 점수 표시
        String cleanScores = finalScores.replaceAll("<html>", "")
                .replaceAll("</html>", "")
                .replaceAll("<br>", "\n")
                .trim();

        if (cleanScores.isEmpty()) {
            scoreArea.setText("점수 정보가 없습니다.");
        } else {
            scoreArea.setText(cleanScores);
        }

        centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(scoreArea, BorderLayout.CENTER);
        centerPanel.setBackground(Color.white);

        bottom = new JPanel();
        exitBtn = new JButton("종료");

        exitBtn.addActionListener(e -> {
            try {
                if (playerSocket != null && !playerSocket.isClosed()) {
                    playerSocket.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });

        bottom.add(exitBtn);

        mainPanel.add(title, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(centerPanel), BorderLayout.CENTER);
        mainPanel.add(bottom, BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // RESTART 메시지 대기
        listenForRestart();
    }

    private void listenForRestart() {
        new Thread(() -> {
            try {
                in = new BufferedReader(new InputStreamReader(playerSocket.getInputStream()));
                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.equals("RESTART")) {
                        // 게임 재시작 - PlayerAnswerFrame으로 돌아가기
                        SwingUtilities.invokeLater(() -> {
                            new PlayerAnswerFrame(playerSocket);
                            dispose();
                        });
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("[PLAYER] RESTART 수신 실패");
            }
        }).start();
    }

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 7400);
            PlayerScoreFrame p = new PlayerScoreFrame(socket, "Player1: 10\nPlayer2: 5");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}