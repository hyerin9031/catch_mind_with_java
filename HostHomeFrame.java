package termproject;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class HostHomeFrame extends JFrame{
	private JPanel mainPanel, textPanel, buttonPanel;
	private JLabel title;
	private JButton animalBtn, foodBtn, objectBtn, customBtn;
	Socket hostsocket = null;
	//DatagramSocket drawsokcet = null;
	PrintWriter out = null;
	
	public HostHomeFrame(Socket hostsocket) throws IOException{
		this.hostsocket = hostsocket;
		//this.drawsokcet = drawsokcet;
		out = new PrintWriter(hostsocket.getOutputStream(), true);
		
		setTitle("CatchMind - 출제자");
        setSize(900, 600);
        setLocationRelativeTo(null);
		
		mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		
		title = new JLabel("캐치마인드에 오신 것을 환영합니다~!", SwingConstants.CENTER);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
		mainPanel.add(title);
		
		buttonPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        animalBtn = new JButton("동물");
        foodBtn = new JButton("음식");
        objectBtn = new JButton("사물");
        customBtn = new JButton("사용자 직접 출제");
        
        //TODO 각 버튼 클릭시 각 카테고리 정보를 서버로 전송하고 다음 페이지로 넘어가는 액션 이벤트 추가
        animalBtn.addActionListener(e -> {
        	out.println("animal");
            new HostDrawFrame(hostsocket);
            dispose();
        });
        
        foodBtn.addActionListener(e -> {
        	out.println("food");
            new HostDrawFrame(hostsocket);
            dispose();
        });
        
        objectBtn.addActionListener(e -> {
        	out.println("object");
            new HostDrawFrame(hostsocket);
            dispose();
        });
        
        customBtn.addActionListener(e -> {
        	out.println("custom");
            new HostCustomFrame(hostsocket);
            dispose();
        });
        
        buttonPanel.add(animalBtn);
        buttonPanel.add(foodBtn);
        buttonPanel.add(objectBtn);
        buttonPanel.add(customBtn);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        add(mainPanel);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	/*public static void main(String[] args) {
		try {
			HostHomeFrame h = new HostHomeFrame(new Socket("localhost", 5000));
		}catch(Exception e) {
			e.printStackTrace();
		}
	}*/
}
