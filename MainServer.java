package termproject;

import java.util.*;
import java.net.*;
import java.io.*;

//animal, food, object 문제 출제
class Problem {
    private List<String> wordList = new ArrayList<>();
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

    private static final int HOST_PORT = 7100;
    private static Socket hostSocket = null;
    private static GameInfo gameInfo = new GameInfo();

    public static void main(String[] args) {
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

    public static GameInfo getGameInfo() {
        return gameInfo;
    }
}

