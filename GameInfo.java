package termproject;

import java.util.*;

public class GameInfo {
	private HashMap<String, Integer> player = new HashMap<>();	// player 이름과 점수
	private List<String> playerList = new ArrayList<>();	// player 리스트

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
	 
	 public int getScore(String name) {	// player 점수 반환
		 return player.get(name);
	 }
	 
	 public void setCategory(String category) {	// 출제 카테고리 설정
		 this.category = category;
	 }
	 
	 public String getCategory() {	// 카테고리 반환
		 return category;
	 }
	 
	 public void setWord(String word) { // 문제 단어 설정
		 this.word = word;
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
			 return true;
	     }else {
	    	 return false;
	     }
	 }
	 
	 public void reset() {	// 게임 리셋
		 for(String s : playerList) {
			 player.replace(s, 0);
			 this.currentRound = 1;
			 this.category = null;
			 this.word = null;
		 }
	 }
}
