import javax.swing.*;
import java.awt.*;
import java.net.Socket;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class HostCustomFrame extends JFrame{
    private JPanel mainPanel, inputPanel;
    private JButton submitBtn;
    private List<JTextField> wordFields;
    private JLabel titleLabel;
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
        mainPanel.setLayout(new BorderLayout());

        titleLabel = new JLabel("10개의 문제를 입력해주세요", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        // 10개 문제 입력 패널
        inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(10, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        wordFields = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            JLabel label = new JLabel(i + "라운드:");
            label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));

            JTextField textField = new JTextField(20);
            textField.setFont(textField.getFont().deriveFont(14f));

            inputPanel.add(label);
            inputPanel.add(textField);
            wordFields.add(textField);
        }

        submitBtn = new JButton("출제 시작");
        submitBtn.setFont(submitBtn.getFont().deriveFont(Font.BOLD, 16f));
        submitBtn.setPreferredSize(new Dimension(150, 40));

        submitBtn.addActionListener(e -> {
            // 모든 필드가 입력되었는지 확인
            List<String> words = new ArrayList<>();
            for (int i = 0; i < wordFields.size(); i++) {
                String word = wordFields.get(i).getText().trim();
                if (word.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            (i + 1) + "라운드 문제를 입력해주세요.");
                    return;
                }
                words.add(word);
            }

            // 10개 문제를 서버로 전송
            StringBuilder allWords = new StringBuilder("CUSTOM_WORDS:");
            for (int i = 0; i < words.size(); i++) {
                allWords.append(words.get(i));
                if (i < words.size() - 1) {
                    allWords.append(",");
                }
            }
            out.println(allWords.toString());

            // HostDrawFrame으로 이동
            new HostDrawFrame(hostsocket);
            dispose();
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(submitBtn);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(inputPanel), BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}