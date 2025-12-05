import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Map;

public class HostDrawFrame extends JFrame {

	private JPanel mainPanel, topPanel, sidePanel, bottomPanel;
	private JButton penBtn, eraseBtn, colorBtn, clearBtn, nextBtn;
	private JLabel wordLabel, playerLabel, roundLabel;

	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;

	private DrawingPanel drawingPanel;
	private UdpSender sender;

	private GameInfo gameInfo;

	public HostDrawFrame(Socket socket) {
		this.socket = socket;
		this.gameInfo = MainServer.getGameInfo();

		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);

			sender = new UdpSender("255.255.255.255", 9001);
		} catch (Exception e) {
			e.printStackTrace();
		}

		setTitle("CatchMind - 출제자");
		setSize(900, 600);
		setLocationRelativeTo(null);

		mainPanel = new JPanel(new BorderLayout());

		// 상단 패널: 라운드, 단어, 플레이어 점수
		topPanel = new JPanel(new GridLayout(3,1));
		roundLabel = new JLabel("", SwingConstants.CENTER);
		wordLabel = new JLabel("", SwingConstants.CENTER);
		wordLabel.setFont(wordLabel.getFont().deriveFont(Font.BOLD, 22f));
		playerLabel = new JLabel("", SwingConstants.CENTER);
		topPanel.add(roundLabel);
		topPanel.add(wordLabel);
		topPanel.add(playerLabel);
		mainPanel.add(topPanel, BorderLayout.NORTH);

		// 그림판
		drawingPanel = new DrawingPanel(sender);
		mainPanel.add(drawingPanel, BorderLayout.CENTER);

		// 사이드 버튼
		sidePanel = new JPanel();
		sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));

		penBtn = new JButton("펜");
		eraseBtn = new JButton("지우개");
		colorBtn = new JButton("색상");
		clearBtn = new JButton("모두 지우기");

		penBtn.addActionListener(e -> drawingPanel.setEraser(false));
		eraseBtn.addActionListener(e -> drawingPanel.setEraser(true));
		colorBtn.addActionListener(this::changeColor);
		clearBtn.addActionListener(e -> {
			drawingPanel.clear();
			sender.send("CLEAR");
		});

		sidePanel.add(penBtn);
		sidePanel.add(Box.createVerticalStrut(10));
		sidePanel.add(eraseBtn);
		sidePanel.add(Box.createVerticalStrut(10));
		sidePanel.add(colorBtn);
		sidePanel.add(Box.createVerticalStrut(10));
		sidePanel.add(clearBtn);

		mainPanel.add(sidePanel, BorderLayout.EAST);

		// 하단 버튼 (다음 라운드)
		bottomPanel = new JPanel();
		nextBtn = new JButton("다음 라운드");
		nextBtn.addActionListener(e -> {
			out.println("Next");
			drawingPanel.clear();
			sender.send("CLEAR");
		});
		bottomPanel.add(nextBtn);
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);

		add(mainPanel);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		initTurn();
		startReceiver();
	}

	private void initTurn() {
		roundLabel.setText(gameInfo.getCurrentRound() + " / 10");
		wordLabel.setText(gameInfo.getWord());

		StringBuilder sb = new StringBuilder("<html>");
		for (Map.Entry<String, Integer> entry : gameInfo.getGameState().entrySet()) {
			sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("<br>");
		}
		sb.append("</html>");
		playerLabel.setText(sb.toString());

		drawingPanel.clear();
	}

	private void startReceiver() {
		new Thread(() -> {
			try {
				String msg;
				while ((msg = in.readLine()) != null) {
					System.out.println("[HOST] 수신: " + msg);

					if(msg.startsWith("WORD:")) {
						gameInfo.setWord(msg.substring(5));
						SwingUtilities.invokeLater(() -> initTurn());
					} else if(msg.startsWith("ROUND:")) {
						// 라운드 업데이트
						SwingUtilities.invokeLater(() -> {
							roundLabel.setText(gameInfo.getCurrentRound() + " / 10");
						});
					} else if(msg.equals("GAME_END")) {
						SwingUtilities.invokeLater(() -> {
							JOptionPane.showMessageDialog(this, "게임 종료!");
							new HostScoreFrame(socket);
							dispose();
						});
					} else if(msg.equals("REQUEST_WORD")) {
						// custom 모드에서 새 단어 요청
						SwingUtilities.invokeLater(() -> {
							new HostCustomFrame(socket);
							dispose();
						});
					}
				}
			} catch (Exception e) {
				System.out.println("[HOST] 서버 연결 종료");
			}
		}).start();
	}

	private void changeColor(ActionEvent e) {
		Color c = JColorChooser.showDialog(this, "색상 선택", drawingPanel.getCurrentColor());
		if(c != null) drawingPanel.setCurrentColor(c);
	}

	private static class DrawingPanel extends JPanel {
		private BufferedImage canvas;
		private Graphics2D g2;
		private int lastX, lastY;
		private boolean eraser = false;
		private Color currentColor = Color.BLACK;

		private final UdpSender sender;

		public DrawingPanel(UdpSender sender) {
			this.sender = sender;

			setBackground(Color.WHITE);
			setPreferredSize(new Dimension(600,400));

			addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					initGraphics();
					lastX = e.getX();
					lastY = e.getY();
				}
			});

			addMouseMotionListener(new MouseAdapter() {
				public void mouseDragged(MouseEvent e) {
					if(g2 == null) return;

					int x = e.getX();
					int y = e.getY();

					int strokeWidth;
					Color drawColor;

					if(eraser) {
						strokeWidth = 20;
						drawColor = Color.WHITE;
					} else {
						strokeWidth = 3;
						drawColor = currentColor;
					}

					g2.setStroke(new BasicStroke(strokeWidth));
					g2.setColor(drawColor);
					g2.drawLine(lastX, lastY, x, y);

					String msg = String.format("DRAW %d %d %d %d %d %d %d %d",
							lastX, lastY, x, y,
							drawColor.getRed(), drawColor.getGreen(), drawColor.getBlue(),
							strokeWidth);
					sender.send(msg);

					lastX = x;
					lastY = y;
					repaint();
				}
			});
		}

		private void initGraphics() {
			if(canvas == null) {
				canvas = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
				g2 = canvas.createGraphics();
				g2.setColor(Color.WHITE);
				g2.fillRect(0,0,getWidth(),getHeight());
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			}
		}

		public void clear() {
			if(canvas != null && g2 != null) {
				g2.setColor(Color.WHITE);
				g2.fillRect(0,0,getWidth(),getHeight());
				repaint();
			}
		}

		public void setEraser(boolean e) { this.eraser = e; }
		public void setCurrentColor(Color c) { this.currentColor = c; }
		public Color getCurrentColor() { return currentColor; }

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if(canvas != null) g.drawImage(canvas,0,0,null);
		}
	}

	public static class UdpSender {
		private final DatagramSocket socket;
		private final InetAddress address;
		private final int port;

		public UdpSender(String host, int port) throws Exception {
			this.socket = new DatagramSocket();
			this.address = InetAddress.getByName(host);
			this.port = port;
		}

		public void send(String msg) {
			try {
				byte[] buf = msg.getBytes();
				DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
				socket.send(packet);
			} catch (Exception e) {
				System.out.println("UDP 전송 오류: " + e.getMessage());
			}
		}
	}
}