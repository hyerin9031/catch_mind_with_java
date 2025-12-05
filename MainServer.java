package termproject;

import java.io.*;
import java.net.*;

// animal, food, object 문제 출제용 클래스
class Problem {
    private java.util.List<String> wordList = new java.util.ArrayList<>();
    private String animalFile = "catchmind_words_animal.txt";
    private String foodFile = "catchmind_words_food.txt";
    private String objectFile = "catchmind_words_object.txt";
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
                wordList.add(line);
            }
        }
    }

    public String randomWord() {
        int index = (int) (Math.random() * wordList.size());
        word = wordList.get(index);
        return word;
    }
}

public class MainServer {

    private static final int HOST_PORT = 7300;
    private static final int PLAYER_PORT = 7400; // 플레이어 전용 포트
    private static Socket hostSocket = null;
    private static GameInfo gameInfo = new GameInfo();

    public static void main(String[] args) {
        // -------------------
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

        // -------------------
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

    // ==================== HostHandler
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
                    System.out.println("[SERVER] MSG: " + msg);
                    if (msg.equals("animal") || msg.equals("food") || msg.equals("object") || msg.equals("custom")) {
                        gameInfo.setCategory(msg);
                        Problem problem = new Problem(gameInfo.getCategory());
                        String newWord = problem.randomWord();
                        gameInfo.setWord(newWord);
                        sendToHost("WORD:" + newWord);
                    } else if (msg.equals("Next")) {
                        if (gameInfo.nextRound()) {
                            Problem problem = new Problem(gameInfo.getCategory());
                            String newWord = problem.randomWord();
                            gameInfo.setWord(newWord);
                            sendToHost("WORD:" + newWord);
                        } else {
                            sendToHost("GAME_END");
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("[SERVER] Host 연결 종료");
            }
        }

        private void sendToHost(String msg) {
            out.println(msg);
        }
    }

    // ==================== PlayerHandler
    static class PlayerHandler implements Runnable {
        private BufferedReader in;
        private PrintWriter out;

        public PlayerHandler(Socket socket) {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
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
                        String nickname = msg.substring(5).trim();
                        gameInfo.addPlayer(nickname); // GameInfo에 플레이어 추가
                        System.out.println("[PLAYER] 닉네임 등록: " + nickname);
                    }
                    // TODO: 정답 처리, 점수 업데이트 등 추가 가능
                }
            } catch (Exception e) {
                System.out.println("[SERVER] Player 연결 종료");
            }
        }
    }

    // ==================== GameInfo getter
    public static GameInfo getGameInfo() {
        return gameInfo;
    }
}
