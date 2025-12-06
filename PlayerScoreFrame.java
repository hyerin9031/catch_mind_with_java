import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class PlayerScoreFrame extends JFrame {

    private final Socket playerSocket;
    private final String scoreHtml;   // HOST에서 넘겨준 점수 HTML (playerLabel.getText())

    private JLabel titleLabel;
    private JLabel scoreLabel;
    private JButton closeBtn;

    public PlayerScoreFrame(Socket playerSocket, String scoreHtml) {
        this.playerSocket = playerSocket;
        this.scoreHtml = scoreHtml;

        setTitle("CatchMind - 최종 점수");
        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
        setVisible(true);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 제목
        titleLabel = new JLabel("최종 점수");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // 점수 표시 (HTML 그대로 사용)
        scoreLabel = new JLabel(scoreHtml);
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JScrollPane scrollPane = new JScrollPane(scoreLabel);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 닫기 버튼
        closeBtn = new JButton("닫기");
        closeBtn.addActionListener(e -> {
            try {
                if (playerSocket != null && !playerSocket.isClosed()) {
                    playerSocket.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        });

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(closeBtn);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }
}
