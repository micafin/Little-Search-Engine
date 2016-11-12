package search;

import java.io.*;
import java.util.*;

/**
 * This class encapsulates an occurrence of a keyword in a document. It stores the
 * document name, and the frequency of occurrence in that document. Occurrences are
 * associated with keywords in an index hash table.
 * 
 * @author Sesh Venugopal
 * 
 */
class Occurrence {
	/**
	 * Document in which a keyword occurs.
	 */
	String document;
	
	/**
	 * The frequency (number of times) the keyword occurs in the above document.
	 */
	int frequency;
	
	/**
	 * Initializes this occurrence with the given document,frequency pair.
	 * 
	 * @param doc Document name
	 * @param freq Frequency
	 */
	public Occurrence(String doc, int freq) {
		document = doc;
		frequency = freq;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "(" + document + "," + frequency + ")";
	}
}

/**
 * This class builds an index of keywords. Each keyword maps to a set of documents in
 * which it occurs, with frequency of occurrence in each document. Once the index is built,
 * the documents can searched on for keywords.
 *
 */
public class LittleSearchEngine {
	
	/**
	 * This is a hash table of all keywords. The key is the actual keyword, and the associated value is
	 * an array list of all occurrences of the keyword in documents. The array list is maintained in descending
	 * order of occurrence frequencies.
	 */
	HashMap<String,ArrayList<Occurrence>> keywordsIndex;
	
	/**
	 * The hash table of all noise words - mapping is from word to itself.
	 */
	HashMap<String,String> noiseWords;
	
	/**
	 * Creates the keyWordsIndex and noiseWords hash tables.
	 */
	public LittleSearchEngine() {
		keywordsIndex = new HashMap<String,ArrayList<Occurrence>>(1000,2.0f);
		noiseWords = new HashMap<String,String>(100,2.0f);
	}
	
	/**
	 * This method indexes all keywords found in all the input documents. When this
	 * method is done, the keywordsIndex hash table will be filled with all keywords,
	 * each of which is associated with an array list of Occurrence objects, arranged
	 * in decreasing frequencies of occurrence.
	 * 
	 * @param docsFile Name of file that has a list of all the document file names, one name per line
	 * @param noiseWordsFile Name of file that has a list of noise words, one noise word per line
	 * @throws FileNotFoundException If there is a problem locating any of the input files on disk
	 */
	public void makeIndex(String docsFile, String noiseWordsFile) 
	throws FileNotFoundException {
		// load noise words to hash table
		Scanner sc = new Scanner(new File(noiseWordsFile));
		while (sc.hasNext()) {
			String word = sc.next();
			noiseWords.put(word,word);
		}
		
		// index all keywords
		sc = new Scanner(new File(docsFile));
		while (sc.hasNext()) {
			String docFile = sc.next();
			HashMap<String,Occurrence> kws = loadKeyWords(docFile);
			mergeKeyWords(kws);
		}
		
	}

	/**
	 * Scans a document, and loads all keywords found into a hash table of keyword occurrences
	 * in the document. Uses the getKeyWord method to separate keywords from other words.
	 * 
	 * @param docFile Name of the document file to be scanned and loaded
	 * @return Hash table of keywords in the given document, each associated with an Occurrence object
	 * @throws FileNotFoundException If the document file is not found on disk
	 */
	public HashMap<String,Occurrence> loadKeyWords(String docFile) 
	throws FileNotFoundException {
		Scanner sc = new Scanner(new File(docFile));
		HashMap<String,Occurrence> loadWord=new HashMap<String,Occurrence>(1000,2.0f);

		while(sc.hasNext()){
			String word=sc.next();
			String checkWord=getKeyWord(word.trim());
			if(checkWord!=null){
				Occurrence checkLoad=loadWord.get(checkWord);
				if(checkLoad!=null){
					checkLoad.frequency++;
				}
				else{
					loadWord.put(checkWord,new Occurrence(docFile, 1));
				}
			}
		}
		sc.close();
		return loadWord;
	}
	
	/**
	 * Merges the keywords for a single document into the master keywordsIndex
	 * hash table. For each keyword, its Occurrence in the current document
	 * must be inserted in the correct place (according to descending order of
	 * frequency) in the same keyword's Occurrence list in the master hash table. 
	 * This is done by calling the insertLastOccurrence method.
	 * 
	 * @param kws Keywords hash table for a document
	 */
	public void mergeKeyWords(HashMap<String,Occurrence> kws) {
		
		for(String k:kws.keySet()){
			ArrayList<Occurrence> points=keywordsIndex.get(k);
			//checks elements in points
			if(points!=null){
				points.add(kws.get(k));
			}
			else{
				points=new ArrayList<Occurrence>();
				points.add(kws.get(k));
			}
			//System.out.println(keywordsIndex);
			//System.out.println(points);
			ArrayList<Integer> spot=insertLastOccurrence(points);
			//System.out.println(spot);
			keywordsIndex.put(k, points);
		}
	}
	
	/**
	 * Given a word, returns it as a keyword if it passes the keyword test,
	 * otherwise returns null. A keyword is any word that, after being stripped of any
	 * TRAILING punctuation, consists only of alphabetic letters, and is not
	 * a noise word. All words are treated in a case-INsensitive manner.
	 * 
	 * Punctuation characters are the following: '.', ',', '?', ':', ';' and '!'
	 * 
	 * @param word Candidate word
	 * @return Keyword (word without trailing punctuation, LOWER CASE)
	 */
	public String getKeyWord(String word) {
		boolean check=true,letterCheck=true;
		int index=word.length();
		for(int i=0;i<word.length();i++){
			if(Character.isLetter(word.charAt(i))&&!check){
				return null;
			}
			if(!Character.isLetter(word.charAt(i))&&word.charAt(i)!='.'&&word.charAt(i)!=','&&word.charAt(i)!='?'&&word.charAt(i)!=':'&&word.charAt(i)!=';'&&word.charAt(i)!='!'){
				return null;
			}
			if(!Character.isLetter(word.charAt(i))){
				check=false;
				if(letterCheck){
					index=i;
					letterCheck=false;
				}
			}
		}
		if(index>1){
			word=word.substring(0,index).toLowerCase();
		}
		else
			return null;
		if(noiseWords.get(word)!=null){
			return null;
		}
		return word;
	}
	
	/**
	 * Inserts the last occurrence in the parameter list in the correct position in the
	 * same list, based on ordering occurrences on descending frequencies. The elements
	 * 0..n-2 in the list are already in the correct order. Insertion of the last element
	 * (the one at index n-1) is done by first finding the correct spot using binary search, 
	 * then inserting at that spot.
	 * 
	 * @param occs List of Occurrences
	 * @return Sequence of mid point indexes in the input list checked by the binary search process,
	 *         null if the size of the input list is 1. This returned array list is only used to test
	 *         your code - it is not used elsewhere in the program.
	 */
	public ArrayList<Integer> insertLastOccurrence(ArrayList<Occurrence> occs) {
		if(occs==null){
			return null;
		}
		int lo=occs.size()-2,hi=0,mid=0;
		//System.out.println(occs);
		ArrayList<Integer> newOcc=new ArrayList<Integer>();
		
		while(hi<=lo){
			mid = (lo+hi)/2;   
			if (occs.get(occs.size()-1).frequency==occs.get(mid).frequency) {
				newOcc.add(mid);
				break;
			}
			if (occs.get(occs.size()-1).frequency>occs.get(mid).frequency) {
				newOcc.add(mid);
				lo = mid-1;
			} else {
				newOcc.add(mid);
				hi = mid+1;
			}
		}
		mid=(lo+(hi+1))/2;
		while((mid<occs.size()-1)&&occs.get(mid).frequency==occs.get(mid+1).frequency){
			mid=mid+1;
		}
		occs.add(mid, occs.get(occs.size()-1));
		occs.remove(occs.size()-1);
		return newOcc;
	}
	
	/**
	 * Search result for "kw1 or kw2". A document is in the result set if kw1 or kw2 occurs in that
	 * document. Result set is arranged in descending order of occurrence frequencies. (Note that a
	 * matching document will only appear once in the result.) Ties in frequency values are broken
	 * in favor of the first keyword. (That is, if kw1 is in doc1 with frequency f1, and kw2 is in doc2
	 * also with the same frequency f1, then doc1 will appear before doc2 in the result. 
	 * The result set is limited to 5 entries. If there are no matching documents, the result is null.
	 * 
	 * @param kw1 First keyword
	 * @param kw1 Second keyword
	 * @return List of NAMES of documents in which either kw1 or kw2 occurs, arranged in descending order of
	 *         frequencies. The result size is limited to 5 documents. If there are no matching documents,
	 *         the result is null.
	 */
	public ArrayList<String> top5search(String kw1, String kw2) {
		kw1.toLowerCase();
		kw2.toLowerCase();
		
		ArrayList<Occurrence> kw1List=keywordsIndex.get(kw1);
		ArrayList<Occurrence> kw2List=keywordsIndex.get(kw2);
		if(kw1List==null&&kw2List==null){
			return null;
		}
		ArrayList<Occurrence> sort=new ArrayList<Occurrence>();
		if(kw1List!=null){
			sort.addAll(kw1List);
		}
		if(kw2List!=null){
			sort.addAll(kw2List);
		}
		//System.out.println(keywordsIndex);
		ArrayList<String> top5List=new ArrayList<String>();
		int greatest=0,rem=0;
		String doc="";
		while(top5List.size()<5){
			for(int i=0;i<sort.size();i++){
				if(sort.get(i).frequency>greatest){
					greatest=sort.get(i).frequency;
					doc=sort.get(i).document;
					rem=i;
				}	
			}
			if(!top5List.contains(doc)){
				top5List.add(doc);
			}
			greatest=0;
			sort.remove(rem);
			if(sort.size()==0){
				break;
			}
		}
		return top5List;
	}
}