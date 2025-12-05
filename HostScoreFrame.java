import javax.swing.*;
import java.awt.*;
import java.net.Socket;
import java.io.*;

public class HostScoreFrame extends JFrame{
    private JPanel mainPanel, bottom, centerPanel;
    private JButton restartBtn, endBtn;
    private JLabel title;
    private JTextArea scoreArea;
    Socket hostsocket = null;
    PrintWriter out = null;

    public HostScoreFrame(Socket hostsocket, String finalScores) {
        this.hostsocket = hostsocket;

        try {
            out = new PrintWriter(hostsocket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        // 점수 표시
        if (finalScores == null || finalScores.isEmpty()) {
            scoreArea.setText("플레이어가 없습니다.");
        } else {
            StringBuilder sb = new StringBuilder();
            String[] entries = finalScores.split(",");
            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    sb.append(parts[0]).append(": ").append(parts[1]).append("점\n");
                }
            }
            scoreArea.setText(sb.toString());
        }

        centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(scoreArea, BorderLayout.CENTER);
        centerPanel.setBackground(Color.white);

        bottom = new JPanel();
        restartBtn = new JButton("다시하기");
        endBtn = new JButton("끝내기");

        restartBtn.addActionListener(e -> {
            // 게임 리셋
            GameInfo gameInfo = MainServer.getGameInfo();
            gameInfo.reset();

            // 플레이어들에게 재시작 신호 전송
            try {
                out.println("RESTART");
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // HostHomeFrame으로 돌아가기
            new HostHomeFrame(hostsocket);
            dispose();
        });

        endBtn.addActionListener(e -> {
            try {
                if (hostsocket != null && !hostsocket.isClosed()) {
                    hostsocket.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });

        bottom.add(restartBtn);
        bottom.add(endBtn);

        mainPanel.add(title, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(centerPanel), BorderLayout.CENTER);
        mainPanel.add(bottom, BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}