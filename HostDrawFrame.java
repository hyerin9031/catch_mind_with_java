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
	private JDialog correctDialog;   // (지금은 안 써도 되지만 남겨둠)

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

			sender = new UdpSender("localhost", 9100);
		} catch (Exception e) {
			e.printStackTrace();
		}

		setTitle("CatchMind - 출제자");
		setSize(900, 600);
		setLocationRelativeTo(null);
		setResizable(false); // ★ 화면 크기 고정

		mainPanel = new JPanel(new BorderLayout());

		// 상단 패널: 라운드, 단어
		topPanel = new JPanel(new GridLayout(2,1));
		roundLabel = new JLabel("", SwingConstants.CENTER);
		wordLabel = new JLabel("", SwingConstants.CENTER);
		wordLabel.setFont(wordLabel.getFont().deriveFont(Font.BOLD, 22f));
		topPanel.add(roundLabel);
		topPanel.add(wordLabel);
		mainPanel.add(topPanel, BorderLayout.NORTH);

		// 왼쪽 패널: 플레이어 점수
		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.setBorder(BorderFactory.createTitledBorder("플레이어 점수"));
		leftPanel.setPreferredSize(new Dimension(180, 0));

		playerLabel = new JLabel("", SwingConstants.LEFT);
		playerLabel.setFont(playerLabel.getFont().deriveFont(14f));
		playerLabel.setVerticalAlignment(SwingConstants.TOP);
		JScrollPane playerScroll = new JScrollPane(playerLabel);
		playerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		leftPanel.add(playerScroll, BorderLayout.CENTER);
		mainPanel.add(leftPanel, BorderLayout.WEST);

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
		nextBtn.addActionListener(e -> proceedNextRound()); // ★ 공통 함수 호출
		bottomPanel.add(nextBtn);
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);

		add(mainPanel);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		initTurn();
		startReceiver();
	}

	// ★ 호스트가 다음 라운드로 넘길 때 공통 동작
	private void proceedNextRound() {
		out.println("Next");
		drawingPanel.clear();
		sender.send("CLEAR");
	}

	private void initTurn() {
		// 라운드/단어만 호스트 쪽 GameInfo로 갱신
		roundLabel.setText(gameInfo.getCurrentRound() + " / 10");
		wordLabel.setText(gameInfo.getWord());
		drawingPanel.clear();
	}

	private void startReceiver() {
		new Thread(() -> {
			try {
				String msg;
				String lastScores = ""; // 최종 점수 저장
				while ((msg = in.readLine()) != null) {
					System.out.println("[HOST] 수신: " + msg);

					if (msg.startsWith("WORD:")) {
						gameInfo.setWord(msg.substring(5));
						SwingUtilities.invokeLater(this::initTurn);

					} else if (msg.startsWith("ROUND:")) {
						// 라운드 업데이트
						String round = msg.substring(6);
						SwingUtilities.invokeLater(() ->
								roundLabel.setText(round + " / 10")
						);

					} else if (msg.startsWith("SCORES:")) {
						// 점수 업데이트
						String scores = msg.substring(7);
						lastScores = scores; // 최종 점수 저장
						SwingUtilities.invokeLater(() -> {
							StringBuilder sb = new StringBuilder("<html>");
							for (String entry : scores.split(",")) {
								sb.append(entry.replace(":", ": ")).append("<br>");
							}
							sb.append("</html>");
							playerLabel.setText(sb.toString());
						});

					} else if (msg.startsWith("HOST_CORRECT_PLAYER:")) {
						// ★ 정답자 정보 수신 → 호스트에게 팝업 띄우기
						String[] parts = msg.substring("HOST_CORRECT_PLAYER:".length()).split(":");
						if (parts.length >= 2) {
							String correctPlayer = parts[0];
							String correctAnswer = parts[1];

							SwingUtilities.invokeLater(() -> {
								String text =
										correctPlayer + "님이 정답을 맞췄습니다!\n" +
												"정답: " + correctAnswer + "\n\n" +
												"확인을 누르면 다음 라운드로 넘어갑니다.";

								int result = JOptionPane.showConfirmDialog(
										HostDrawFrame.this,
										text,
										"정답 알림",
										JOptionPane.OK_CANCEL_OPTION
								);
								if (result == JOptionPane.OK_OPTION) {
									proceedNextRound();
								}
							});
						}

					} else if (msg.equals("GAME_END")) {
						String finalScores = lastScores; // 최종 점수 저장
						SwingUtilities.invokeLater(() -> {
							JOptionPane.showMessageDialog(this, "게임 종료!");
							new HostScoreFrame(socket, finalScores);
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
		if (c != null) drawingPanel.setCurrentColor(c);
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
			setPreferredSize(new Dimension(600, 400)); // ★ 그림판 크기 고정

			// 패널 크기 바뀌면 캔버스도 맞춰주기
			addComponentListener(new ComponentAdapter() {
				@Override
				public void componentResized(ComponentEvent e) {
					ensureCanvasSize();
					repaint();
				}
			});

			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					ensureCanvasSize();
					lastX = e.getX();
					lastY = e.getY();
				}
			});

			addMouseMotionListener(new MouseMotionAdapter() {
				@Override
				public void mouseDragged(MouseEvent e) {
					ensureCanvasSize();

					int x = e.getX();
					int y = e.getY();

					int strokeWidth;
					Color drawColor;

					if (eraser) {
						strokeWidth = 20;
						drawColor = Color.WHITE;
					} else {
						strokeWidth = 3;
						drawColor = currentColor;
					}

					g2.setStroke(new BasicStroke(strokeWidth));
					g2.setColor(drawColor);
					g2.drawLine(lastX, lastY, x, y);

					// UDP sender 활용 -> DrawingServer로 전송
					String msg = String.format(
							"DRAW %d %d %d %d %d %d %d %d",
							lastX, lastY, x, y,
							drawColor.getRed(), drawColor.getGreen(), drawColor.getBlue(),
							strokeWidth
					);
					sender.send(msg);

					lastX = x;
					lastY = y;
					repaint();
				}
			});
		}

		private void ensureCanvasSize() {
			int w = Math.max(1, getWidth());
			int h = Math.max(1, getHeight());
			if (w == 0 || h == 0) return;

			if (canvas == null || canvas.getWidth() != w || canvas.getHeight() != h) {
				BufferedImage newImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
				Graphics2D gNew = newImg.createGraphics();
				gNew.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				gNew.setColor(Color.WHITE);
				gNew.fillRect(0, 0, w, h);

				if (canvas != null) {
					gNew.drawImage(canvas, 0, 0, null);  // 기존 그림 복사
					g2.dispose();
				}

				canvas = newImg;
				g2 = gNew;
			}
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			ensureCanvasSize();
			if (canvas != null) {
				g.drawImage(canvas, 0, 0, null);
			}
		}

		public void clear() {
			ensureCanvasSize();
			if (canvas != null && g2 != null) {
				g2.setColor(Color.WHITE);
				g2.fillRect(0, 0, getWidth(), getHeight());
				repaint();
			}
		}

		public void setEraser(boolean e) { this.eraser = e; }
		public void setCurrentColor(Color c) { this.currentColor = c; }

		public Color getCurrentColor() { return currentColor; }
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
