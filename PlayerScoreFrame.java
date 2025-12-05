package termproject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class PlayerScoreFrame extends JFrame{
	private JPanel mainPanel, bottom, centerPanel;
	private JButton restartBtn, endBtn;
	private JLabel title, score;
	
	public PlayerScoreFrame() {
		// TODO 서버에 요청해서 플레이어 정보 가져오기 및 score에 설정
		setTitle("CatchMind - 출제자");
        setSize(900, 600);
        setLocationRelativeTo(null);
        
		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
		title = new JLabel("Score", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        
        score = new JLabel("P1 : 10");
        score.setFont(score.getFont().deriveFont(Font.BOLD, 20f));
        centerPanel = new JPanel();
        centerPanel.add(score);
        centerPanel.setBackground(Color.white);
        
        bottom = new JPanel();
        restartBtn = new JButton("다시하기");
        endBtn = new JButton("끝내기");
        //TODO 각각의 버튼이 눌렸을 떄 화면 이동, 종료 및 게임 정보 리셋 이벤트 추가
        
        bottom.add(restartBtn);
        bottom.add(endBtn);
        
        mainPanel.add(title, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(centerPanel), BorderLayout.CENTER);
        mainPanel.add(bottom, BorderLayout.SOUTH);
        
        add(mainPanel);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	public static void main(String[] args) {
		PlayerScoreFrame p = new PlayerScoreFrame();
	}
	
}
