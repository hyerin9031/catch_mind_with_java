import javax.swing.*;
import java.awt.*;
import java.net.Socket;
import java.io.*;

public class HostCustomFrame extends JFrame{
    private JPanel mainPanel;
    private JButton submitBtn;
    private JTextField wordField;
    private JLabel label;
    Socket hostsocket = null;
    PrintWriter out = null;

    public HostCustomFrame(Socket hostsocket) {
        this.hostsocket = hostsocket;

        try {
            out = new PrintWriter(hostsocket.getOutputStream(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setTitle("CatchMind - 출제자");
        setSize(900, 600);
        setLocationRelativeTo(null);

        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        wordField = new JTextField(20);

        label = new JLabel("문제를 입력해주세요: ");
        submitBtn = new JButton("출제");

        submitBtn.addActionListener(e -> {
            String text = wordField.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(this, "단어를 입력하세요.");
                return;
            }

            // 입력된 단어 서버로 전달
            out.println("CUSTOM_WORD:" + text);

            // HostDrawFrame으로 이동
            new HostDrawFrame(hostsocket);
            dispose();
        });

        // Enter 키로도 제출 가능
        wordField.addActionListener(e -> submitBtn.doClick());

        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(label, gbc);
        gbc.gridx = 1;
        mainPanel.add(wordField, gbc);
        gbc.gridx = 2;
        mainPanel.add(submitBtn, gbc);

        add(mainPanel);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}