import java.util.*;

public class GameInfo {
	private HashMap<String, Integer> player = new HashMap<>();	// player 이름과 점수
	private List<String> playerList = new ArrayList<>();	// player 리스트
	private Set<String> answeredPlayers = new HashSet<>(); // 현재 라운드에서 정답 맞춘 플레이어

	private int currentRound = 1;	// 현재 라운드
	private int maxRound = 10;	// 마지막 라운드
	private String category, word;	// 출제 카테고리, 문제 단어

	public void addPlayer(String name) {	// player 추가
		player.put(name, 0);
		playerList.add(name);
	}

	public void rmPlayer(String name) {	// player 삭제
		player.remove(name);
		playerList.remove(name);
	}

	public List<String> getPlayers() {	// player 리스트 반환
		return playerList;
	}

	public HashMap<String, Integer> getGameState(){
		return player;
	}

	public int getScore(String name) {	// player 점수 반환
		return player.getOrDefault(name, 0);
	}

	// 점수 증가 메서드 추가
	public void addScore(String name, int points) {
		if (player.containsKey(name)) {
			player.put(name, player.get(name) + points);
		}
	}

	public void setCategory(String category) {	// 출제 카테고리 설정
		this.category = category;
	}

	public String getCategory() {	// 카테고리 반환
		return category;
	}

	public void setWord(String word) { // 문제 단어 설정
		this.word = word;
		answeredPlayers.clear(); // 새 단어 설정 시 정답자 목록 초기화
	}

	public String getWord() { // 문제 단어 반환
		return word;
	}

	public int getCurrentRound() {	// 현재 라운드 반환
		return currentRound;
	}

	public boolean nextRound() {	// 다음 라운드
		if (currentRound < maxRound) {
			currentRound++;
			answeredPlayers.clear(); // 새 라운드 시작 시 정답자 목록 초기화
			return true;
		}else {
			return false;
		}
	}

	// 정답 확인
	public boolean checkAnswer(String playerName, String answer) {
		if (answeredPlayers.contains(playerName)) {
			return false; // 이미 정답을 맞춘 플레이어
		}

		if (word != null && word.equalsIgnoreCase(answer.trim())) {
			answeredPlayers.add(playerName);
			// 정답 맞춘 순서에 따라 점수 차등 부여
			int points = 10 - (answeredPlayers.size() - 1) * 2; // 첫 번째: 10점, 두 번째: 8점...
			if (points < 2) points = 2; // 최소 2점
			addScore(playerName, points);
			return true;
		}
		return false;
	}

	// 모든 플레이어가 정답을 맞췄는지 확인
	public boolean allPlayersAnswered() {
		return answeredPlayers.size() == playerList.size();
	}

	// 힌트 생성 (글자 수만큼 _ 표시)
	public String getHint() {
		if (word == null) return "";
		StringBuilder hint = new StringBuilder();
		for (int i = 0; i < word.length(); i++) {
			hint.append("_ ");
		}
		return hint.toString().trim();
	}

	public void reset() {	// 게임 리셋
		for(String s : playerList) {
			player.replace(s, 0);
		}
		this.currentRound = 1;
		this.category = null;
		this.word = null;
		answeredPlayers.clear();
	}
}