package termproject;

import java.util.*;
import java.net.*;
import java.io.*;

//animal, food, object 문제 출제
class Problem{
	private List<String> wordList = new ArrayList<>();
	private String animalfile = "catchmind_words_animal.txt";
	private String foodfile = "catchmind_words_food.txt";
	private String objectfile = "catchmind_words_object.txt";
	private int index;
	private String word;
	
	public Problem(String category) {
		if(category.equals("animal")) {
			try {
				readFile(animalfile);
			}catch(IOException e) {
				e.printStackTrace();
			}
		}else if(category.equals("food")) {
			try {
				readFile(foodfile);
			}catch(IOException e) {
				e.printStackTrace();
			}
		}else if(category.equals("object")) {
			try {
				readFile(objectfile);
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void readFile(String filePath) throws IOException {
		try(BufferedReader in = new BufferedReader(new FileReader(filePath))){
			String line;
			while((line=in.readLine())!=null){
				wordList.add(line);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}

	}
	
	public String randomWord() {
		this.index = (int)(this.wordList.size() * Math.random());
		word = wordList.get(index); // 문제선택
		return word;
	}
	
}	

public class MainServer {
		
}
