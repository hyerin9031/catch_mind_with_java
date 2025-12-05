import java.util.*;
import java.net.*;
import java.io.*;

//animal, food, object 문제 출제
class Problem {
	private List<String> wordList = new ArrayList<>();
	private String animalFile = "src/data/catchmind_words_animal.txt";
	private String foodFile = "src/data/catchmind_words_food.txt";
	private String objectFile = "src/data/catchmind_words_object.txt";
	private String word;

	public Problem(String category) {
		try {
			if ("animal".equals(category)) readFile(animalFile);
			else if ("food".equals(category)) readFile(foodFile);
			else if ("object".equals(category)) readFile(objectFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readFile(String filePath) throws IOException {
		try (BufferedReader in = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = in.readLine()) != null) {
				if (!line.trim().isEmpty()) {
					wordList.add(line.trim());
				}
			}
		}
	}

	public String randomWord() {
		if (wordList.isEmpty()) return "단어없음";
		int index = (int) (Math.random() * wordList.size());
		word = wordList.get(index);
		return word;
	}
}

public class MainServer {

	private static final int HOST_PORT = 8000;
	private static final int PLAYER_PORT = 7400;
	private static Socket hostSocket = null;
	private static GameInfo gameInfo = new GameInfo();
	private static List<PrintWriter> playerWriters = new ArrayList<>();
	private static Map<Socket, String> playerNames = new HashMap<>();
	private static Timer roundTimer; // 라운드 타이머
	private static List<String> customWords = new ArrayList<>(); // 사용자 직접 출제할 때의 단어 목록 -> 위대하신 혜린님의 아이디어
	private static int customWordIndex = 0; // 현재 사용 중인 custom 단어 인덱스

	public static void main(String[] args) {
		// Host TCP 서버
		Thread hostThread = new Thread(() -> {
			try (ServerSocket hostServer = new ServerSocket(HOST_PORT)) {
				System.out.println("[SERVER] Host 대기중... PORT=" + HOST_PORT);
				hostSocket = hostServer.accept();
				System.out.println("[SERVER] Host 연결됨");
				new Thread(new HostHandler(hostSocket)).start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		hostThread.start();

		// Player TCP 서버
		Thread playerThread = new Thread(() -> {
			try (ServerSocket playerServer = new ServerSocket(PLAYER_PORT)) {
				System.out.println("[SERVER] Player 대기중... PORT=" + PLAYER_PORT);
				while (true) {
					Socket playerSocket = playerServer.accept();
					System.out.println("[SERVER] Player 연결됨: " + playerSocket.getInetAddress());
					new Thread(new PlayerHandler(playerSocket)).start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		playerThread.start();
	}

	static class HostHandler implements Runnable {
		private BufferedReader in;
		private PrintWriter out;

		public HostHandler(Socket socket) {
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);
				out.println("HOST_OK");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				String msg;
				while ((msg = in.readLine()) != null) {
					System.out.println("[SERVER] HOST MSG: " + msg);

					if (msg.equals("animal") || msg.equals("food") || msg.equals("object")) {
						gameInfo.setCategory(msg);
						Problem problem = new Problem(gameInfo.getCategory());
						String newWord = problem.randomWord();
						gameInfo.setWord(newWord);
						out.println("WORD:" + newWord);
						out.println("ROUND:" + gameInfo.getCurrentRound());

						// 플레이어들에게 힌트 전송
						broadcastToPlayers("HINT:" + gameInfo.getHint());
						broadcastToPlayers("ROUND:" + gameInfo.getCurrentRound());
						broadcastToPlayers("NEW_ROUND");

						// 60초 타이머 시작 (자동 다음 라운드)
						startRoundTimer();

					} else if (msg.equals("custom")) {
						gameInfo.setCategory(msg);
						// custom은 HostCustomFrame에서 단어 목록을 받음

					} else if (msg.startsWith("CUSTOM_WORDS:")) {
						// 사용자가 입력한 10개 단어를 받음
						String wordsStr = msg.substring(13);
						customWords.clear();
						customWordIndex = 0;

						String[] words = wordsStr.split(",");
						for (String word : words) {
							customWords.add(word.trim());
						}

						// 첫 번째 단어로 시작
						if (!customWords.isEmpty()) {
							String firstWord = customWords.get(customWordIndex);
							gameInfo.setWord(firstWord);
							out.println("WORD:" + firstWord);
							out.println("ROUND:" + gameInfo.getCurrentRound());

							broadcastToPlayers("HINT:" + gameInfo.getHint());
							broadcastToPlayers("ROUND:" + gameInfo.getCurrentRound());
							broadcastToPlayers("NEW_ROUND");

							startRoundTimer();
						}

					} else if (msg.startsWith("CUSTOM_WORD:")) {
						// 사용자 직접 출제
						String customWord = msg.substring(12);
						gameInfo.setWord(customWord);
						out.println("WORD:" + customWord);
						out.println("ROUND:" + gameInfo.getCurrentRound());

						broadcastToPlayers("HINT:" + gameInfo.getHint());
						broadcastToPlayers("ROUND:" + gameInfo.getCurrentRound());
						broadcastToPlayers("NEW_ROUND");

						startRoundTimer();

					} else if (msg.equals("Next")) {
						cancelRoundTimer(); // 기존 타이머 취소

						if (gameInfo.nextRound()) {
							if ("custom".equals(gameInfo.getCategory())) {
								// custom 모드면 다음 단어 사용
								customWordIndex++;
								if (customWordIndex < customWords.size()) {
									String nextWord = customWords.get(customWordIndex);
									gameInfo.setWord(nextWord);
									out.println("WORD:" + nextWord);
									out.println("ROUND:" + gameInfo.getCurrentRound());

									broadcastToPlayers("HINT:" + gameInfo.getHint());
									broadcastToPlayers("ROUND:" + gameInfo.getCurrentRound());
									broadcastToPlayers("NEW_ROUND");

									startRoundTimer();
								} else {
									// 단어 목록이 끝남
									out.println("GAME_END");
									broadcastScores();
									broadcastToPlayers("GAME_END");
								}
							} else {
								Problem problem = new Problem(gameInfo.getCategory());
								String newWord = problem.randomWord();
								gameInfo.setWord(newWord);
								out.println("WORD:" + newWord);
								out.println("ROUND:" + gameInfo.getCurrentRound());

								broadcastToPlayers("HINT:" + gameInfo.getHint());
								broadcastToPlayers("ROUND:" + gameInfo.getCurrentRound());
								broadcastToPlayers("NEW_ROUND");

								startRoundTimer();
							}
						} else {
							out.println("GAME_END");
							broadcastScores(); // 최종 점수 전송
							broadcastToPlayers("GAME_END");
						}
					} else if (msg.equals("RESTART")) {
						// 게임 재시작 신호를 모든 플레이어에게 브로드캐스트
						cancelRoundTimer();
						broadcastToPlayers("RESTART");
						System.out.println("[SERVER] 게임 재시작");
					}
				}
			} catch (Exception e) {
				System.out.println("[SERVER] Host 연결 종료");
			}
		}
	}

	static class PlayerHandler implements Runnable {
		private BufferedReader in;
		private PrintWriter out;
		private Socket socket;
		private String playerName;

		public PlayerHandler(Socket socket) {
			this.socket = socket;
			try {
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);

				synchronized (playerWriters) {
					playerWriters.add(out);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				String msg;
				while ((msg = in.readLine()) != null) {
					System.out.println("[PLAYER] MSG: " + msg);

					if (msg.startsWith("NICK:")) {
						playerName = msg.substring(5).trim();
						gameInfo.addPlayer(playerName);
						playerNames.put(socket, playerName);
						System.out.println("[PLAYER] 닉네임 등록: " + playerName);

						// 현재 게임 상태 전송
						out.println("HINT:" + gameInfo.getHint());
						out.println("ROUND:" + gameInfo.getCurrentRound());
						broadcastScores();

					} else if (msg.startsWith("ANSWER:")) {
						String answer = msg.substring(7).trim();

						if (playerName != null) {
							boolean correct = gameInfo.checkAnswer(playerName, answer);

							if (correct) {
								out.println("CORRECT");
								System.out.println("[PLAYER] " + playerName + " 정답!");

								// 모든 플레이어에게 점수 업데이트 브로드캐스트
								broadcastScores();

								// 모든 플레이어가 정답을 맞췄으면 다음 라운드로
								if (gameInfo.allPlayersAnswered()) {
									System.out.println("[SERVER] 모든 플레이어 정답! 다음 라운드로...");

									// 3초 대기 후 다음 라운드
									new Timer().schedule(new TimerTask() {
										@Override
										public void run() {
											nextRound();
										}
									}, 3000);
								}
							}
							// WRONG 메시지 제거 - 틀려도 계속 도전 가능
						}
					}
				}
			} catch (Exception e) {
				System.out.println("[SERVER] Player 연결 종료: " + playerName);
			} finally {
				synchronized (playerWriters) {
					playerWriters.remove(out);
				}
				if (playerName != null) {
					gameInfo.rmPlayer(playerName);
					playerNames.remove(socket);
				}
			}
		}
	}

	// 모든 플레이어에게 메시지 브로드캐스트
	private static void broadcastToPlayers(String msg) {
		synchronized (playerWriters) {
			for (PrintWriter writer : playerWriters) {
				writer.println(msg);
			}
		}
	}

	// 점수 브로드캐스트
	private static void broadcastScores() {
		StringBuilder scores = new StringBuilder("SCORES:");
		for (Map.Entry<String, Integer> entry : gameInfo.getGameState().entrySet()) {
			scores.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
		}
		if (scores.charAt(scores.length() - 1) == ',') {
			scores.deleteCharAt(scores.length() - 1);
		}

		String scoreMsg = scores.toString();

		// 플레이어들에게 전송
		broadcastToPlayers(scoreMsg);

		// Host에게도 전송
		sendToHost(scoreMsg);

		// 라운드 정보도 함께 전송
		broadcastToPlayers("ROUND:" + gameInfo.getCurrentRound());
		sendToHost("ROUND:" + gameInfo.getCurrentRound());
	}

	// Host에게 메시지 전송
	private static void sendToHost(String msg) {
		try {
			if (hostSocket != null && !hostSocket.isClosed()) {
				PrintWriter hostOut = new PrintWriter(hostSocket.getOutputStream(), true);
				hostOut.println(msg);
			}
		} catch (Exception e) {
			System.out.println("[SERVER] Host 메시지 전송 실패: " + e.getMessage());
		}
	}

	// 라운드 타이머 시작 (60초)
	private static void startRoundTimer() {
		cancelRoundTimer();
		roundTimer = new Timer();
		roundTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				System.out.println("[SERVER] 시간 종료! 다음 라운드로...");
				nextRound();
			}
		}, 60000); // 60초
	}

	// 타이머 취소
	private static void cancelRoundTimer() {
		if (roundTimer != null) {
			roundTimer.cancel();
			roundTimer = null;
		}
	}

	// 다음 라운드로 진행
	private static void nextRound() {
		cancelRoundTimer();

		if (gameInfo.nextRound()) {
			if ("custom".equals(gameInfo.getCategory())) {
				// custom 모드면 다음 단어 사용
				customWordIndex++;
				if (customWordIndex < customWords.size()) {
					String nextWord = customWords.get(customWordIndex);
					gameInfo.setWord(nextWord);

					sendToHost("WORD:" + nextWord);
					sendToHost("ROUND:" + gameInfo.getCurrentRound());

					broadcastToPlayers("HINT:" + gameInfo.getHint());
					broadcastToPlayers("ROUND:" + gameInfo.getCurrentRound());
					broadcastToPlayers("NEW_ROUND");

					startRoundTimer();
				} else {
					// 단어 목록이 끝남
					sendToHost("GAME_END");
					broadcastScores();
					broadcastToPlayers("GAME_END");
				}
			} else {
				Problem problem = new Problem(gameInfo.getCategory());
				String newWord = problem.randomWord();
				gameInfo.setWord(newWord);

				sendToHost("WORD:" + newWord);
				sendToHost("ROUND:" + gameInfo.getCurrentRound());

				broadcastToPlayers("HINT:" + gameInfo.getHint());
				broadcastToPlayers("ROUND:" + gameInfo.getCurrentRound());
				broadcastToPlayers("NEW_ROUND");

				startRoundTimer();
			}
		} else {
			// 게임 종료
			sendToHost("GAME_END");

			// 최종 점수 브로드캐스트
			broadcastScores();
			broadcastToPlayers("GAME_END");
		}
	}

	public static GameInfo getGameInfo() {
		return gameInfo;
	}
}