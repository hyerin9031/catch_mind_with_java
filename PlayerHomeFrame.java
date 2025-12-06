import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;

public class PlayerHomeFrame extends JFrame {
    private JPanel mainPanel;
    private JButton submitBtn;
    private JTextField nameField;
    private JLabel label;

    private Socket playerSocket;
    private PrintWriter out;

    public PlayerHomeFrame(Socket playerSocket) {
        this.playerSocket = playerSocket;

        try {
            out = new PrintWriter(playerSocket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("CatchMind - 참가자");
        setSize(900, 600);
        setLocationRelativeTo(null);

        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        nameField = new JTextField(20);

        label = new JLabel("닉네임을 입력해주세요: ");
        submitBtn = new JButton("완료");

        submitBtn.addActionListener(e -> {
            String text = nameField.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(this, "닉네임을 입력하세요.");
                return;
            }

            // 1) 서버로 닉네임 전송 (TCP)
            out.println("NICK:" + text);

            // 2) 닉네임을 함께 넘겨서 PlayerAnswerFrame 생성
            new PlayerAnswerFrame(playerSocket, text);

            // 3) 현재 닉네임 입력 창 닫기
            dispose();
        });

        // Enter 키로도 제출 가능
        nameField.addActionListener(e -> submitBtn.doClick());

        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(label, gbc);
        gbc.gridx = 1;
        mainPanel.add(nameField, gbc);
        gbc.gridx = 2;
        mainPanel.add(submitBtn, gbc);

        add(mainPanel);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
