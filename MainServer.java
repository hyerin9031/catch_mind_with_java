import java.util.*;
import java.net.*;
import java.io.*;

//animal, food, object 문제 출제
class Problem {
	private List<String> wordList = new ArrayList<>();
	private List<String> usedWords = new ArrayList<>(); // 이미 사용한 단어 목록
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

		// 사용 가능한 단어 목록 (아직 사용하지 않은 단어)
		List<String> availableWords = new ArrayList<>();
		for (String w : wordList) {
			if (!usedWords.contains(w)) {
				availableWords.add(w);
			}
		}

		// 모든 단어를 사용했으면 usedWords 초기화
		if (availableWords.isEmpty()) {
			usedWords.clear();
			availableWords = new ArrayList<>(wordList);
		}

		int index = (int) (Math.random() * availableWords.size());
		word = availableWords.get(index);
		usedWords.add(word); // 사용한 단어 목록에 추가
		return word;
	}

	// 게임 재시작 시 사용한 단어 목록 초기화
	public void reset() {
		usedWords.clear();
	}
}

public class MainServer {

	private static final int HOST_PORT = 8000;
	private static final int PLAYER_PORT = 7400;
	private static Socket hostSocket = null;
	private static GameInfo gameInfo = new GameInfo();
	private static List<PrintWriter> playerWriters = new ArrayList<>();
	private static Map<Socket, String> playerNames = new HashMap<>();
	private static List<String> customWords = new ArrayList<>(); // 사용자 직접 출제 단어 목록
	private static int customWordIndex = 0; // 현재 사용 중인 custom 단어 인덱스
	private static Problem currentProblem = null; // 현재 카테고리의 Problem 인스턴스
	static PrintWriter hostOut;

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
				MainServer.hostOut = out;     // ★ 호스트 출력 스트림을 MainServer에 공유
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
						currentProblem = new Problem(gameInfo.getCategory()); // 새 Problem 생성
						String newWord = currentProblem.randomWord();
						gameInfo.setWord(newWord);
						out.println("WORD:" + newWord);
						out.println("ROUND:" + gameInfo.getCurrentRound());

						// 플레이어들에게 힌트 전송
						broadcastToPlayers("HINT:" + gameInfo.getHint());
						broadcastToPlayers("ROUND:" + gameInfo.getCurrentRound());
						broadcastToPlayers("NEW_ROUND");

						// 초기 점수 브로드캐스트 (Host에게도)
						broadcastScores();

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

							// 초기 점수 브로드캐스트 (Host에게도)
							broadcastScores();
						}

					} else if (msg.startsWith("CUSTOM_WORD:")) {
						// 사용자 직접 출제 (더 이상 사용 안 함 - CUSTOM_WORDS 사용)
						String customWord = msg.substring(12);
						gameInfo.setWord(customWord);
						out.println("WORD:" + customWord);
						out.println("ROUND:" + gameInfo.getCurrentRound());

						broadcastToPlayers("HINT:" + gameInfo.getHint());
						broadcastToPlayers("ROUND:" + gameInfo.getCurrentRound());
						broadcastToPlayers("NEW_ROUND");

					} else if (msg.equals("Next")) {
						// 현재 정답 저장
						String currentAnswer = gameInfo.getWord();
						Set<String> answeredPlayers = gameInfo.getAnsweredPlayers();

						if (gameInfo.nextRound()) {
							if ("custom".equals(gameInfo.getCategory())) {
								// custom 모드면 다음 단어 사용
								customWordIndex++;
								if (customWordIndex < customWords.size()) {
									String nextWord = customWords.get(customWordIndex);
									gameInfo.setWord(nextWord);
									out.println("WORD:" + nextWord);
									out.println("ROUND:" + gameInfo.getCurrentRound());

									// 정답 못 맞춘 플레이어들에게만 정답 알림
									broadcastAnswerReveal(currentAnswer, answeredPlayers);

									broadcastToPlayers("HINT:" + gameInfo.getHint());
									broadcastToPlayers("ROUND:" + gameInfo.getCurrentRound());
									broadcastToPlayers("NEW_ROUND");
								} else {
									// 단어 목록이 끝남
									out.println("GAME_END");
									broadcastScores();
									broadcastToPlayers("GAME_END");
								}
							} else {
								String newWord = currentProblem.randomWord(); // 같은 인스턴스 사용
								gameInfo.setWord(newWord);
								out.println("WORD:" + newWord);
								out.println("ROUND:" + gameInfo.getCurrentRound());

								// 정답 못 맞춘 플레이어들에게만 정답 알림
								broadcastAnswerReveal(currentAnswer, answeredPlayers);

								broadcastToPlayers("HINT:" + gameInfo.getHint());
								broadcastToPlayers("ROUND:" + gameInfo.getCurrentRound());
								broadcastToPlayers("NEW_ROUND");
							}
						} else {
							out.println("GAME_END");
							broadcastScores(); // 최종 점수 전송
							broadcastToPlayers("GAME_END");
						}
					} else if (msg.equals("RESTART")) {
						// 게임 재시작 신호를 모든 플레이어에게 브로드캐스트
						broadcastToPlayers("RESTART");
						System.out.println("[SERVER] 게임 재시작");
					} else if (msg.equals("SHUTDOWN")) {
						// 호스트가 게임 종료 - 모든 플레이어에게 종료 신호 전송
						broadcastToPlayers("SHUTDOWN");
						System.out.println("[SERVER] 게임 종료 - 모든 클라이언트 종료");
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

								// 정답자를 제외한 나머지 플레이어들에게만 정답자 알림
								String correctMsg = "CORRECT_PLAYER:" + playerName + ":" + gameInfo.getWord();
								synchronized (playerWriters) {
									for (int i = 0; i < playerWriters.size(); i++) {
										PrintWriter writer = playerWriters.get(i);
										if (writer != out) { // 정답자 본인은 제외
											writer.println(correctMsg);
										}
									}
								}

								// 모든 플레이어에게 점수 업데이트 브로드캐스트
								broadcastScores();

								// ★ 호스트에게도 정답자 정보 보내기
								//    => HostDrawFrame에서 HOST_CORRECT_PLAYER:... 메시지를 받아 팝업 띄움
								MainServer.sendToHost(
										"HOST_CORRECT_PLAYER:" + playerName + ":" + gameInfo.getWord()
								);

								// ★ 자동 다음 라운드 진행은 제거
								//    이제는 호스트 팝업의 "확인" 버튼이 Next 역할을 함
            /*
            if (gameInfo.allPlayersAnswered()) {
                System.out.println("[SERVER] 모든 플레이어 정답! 다음 라운드로...");

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        nextRound();
                    }
                }, 3000);
            }
            */
							}
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

	// 정답 못 맞춘 플레이어들에게만 정답 알림 브로드캐스트
	private static void broadcastAnswerReveal(String answer, Set<String> answeredPlayers) {
		String msg = "ANSWER_REVEAL:" + answer;

		synchronized (playerWriters) {
			// playerNames: Socket -> 플레이어 닉네임
			for (Map.Entry<Socket, String> entry : playerNames.entrySet()) {
				String playerName = entry.getValue();

				// 정답 맞춘 플레이어는 건너뜀
				if (answeredPlayers.contains(playerName)) {
					continue;
				}

				try {
					PrintWriter writer = new PrintWriter(entry.getKey().getOutputStream(), true);
					writer.println(msg);
				} catch (Exception e) {
					// 소켓 끊긴 경우 등은 무시
					System.out.println("[SERVER] ANSWER_REVEAL 전송 실패: " + e.getMessage());
				}
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

	// 다음 라운드로 진행
	private static void nextRound() {
		// 현재 정답 저장
		String currentAnswer = gameInfo.getWord();
		Set<String> answeredPlayers = gameInfo.getAnsweredPlayers();

		if (gameInfo.nextRound()) {
			if ("custom".equals(gameInfo.getCategory())) {
				// custom 모드면 다음 단어 사용
				customWordIndex++;
				if (customWordIndex < customWords.size()) {
					String nextWord = customWords.get(customWordIndex);
					gameInfo.setWord(nextWord);

					sendToHost("WORD:" + nextWord);
					sendToHost("ROUND:" + gameInfo.getCurrentRound());

					// 정답 못 맞춘 플레이어들에게만 정답 알림
					broadcastAnswerReveal(currentAnswer, answeredPlayers);

					broadcastToPlayers("HINT:" + gameInfo.getHint());
					broadcastToPlayers("ROUND:" + gameInfo.getCurrentRound());
					broadcastToPlayers("NEW_ROUND");
				} else {
					// 단어 목록이 끝남
					sendToHost("GAME_END");
					broadcastScores();
					broadcastToPlayers("GAME_END");
				}
			} else {
				String newWord = currentProblem.randomWord(); // 같은 인스턴스 사용
				gameInfo.setWord(newWord);

				sendToHost("WORD:" + newWord);
				sendToHost("ROUND:" + gameInfo.getCurrentRound());

				// 정답 못 맞춘 플레이어들에게만 정답 알림
				broadcastAnswerReveal(currentAnswer, answeredPlayers);

				broadcastToPlayers("HINT:" + gameInfo.getHint());
				broadcastToPlayers("ROUND:" + gameInfo.getCurrentRound());
				broadcastToPlayers("NEW_ROUND");
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