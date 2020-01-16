import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Viet on 10/24/2015.
 */
public class LoadBibleChapter extends Thread {

    final String BOOK_NUMBER_ARRAY[] = {"10","20","30","40","50","60","70","80","90","100","110","120","130","140","150","160","190","220","230","240","250","260","290","300","310","330","340","350","360","370","380","390","400","410","420","430","440","450","460","470","480","490","500","510","520","530","540","550","560","570","580","590","600","610","620","630","640","650","660","670","680","690","700","710","720","730"};

    final String BASE_URL = "https://www.biblegateway.com";

    List<String> chapterUrlList;
    String book_number;
    String dbFile;

    LoadBibleChapter(List<String> chapterUrlList,String book_number,String dbFile) {
        this.chapterUrlList = chapterUrlList;
        this.book_number = book_number;
        this.dbFile = dbFile;
    }

    String chapterUrl;
    String chapterSiteContent;
    boolean isStoriesTableCleaned = false;
    public void run() {
        for (int i=0; i<chapterUrlList.size(); i++){
            //if (i<1 || i>1)
            //   continue;
            chapterUrl = BASE_URL + chapterUrlList.get(i);
            //System.out.println(chapterUrl);
            chapterSiteContent = loadSiteContent(chapterUrl);
            //System.out.println(chapterSiteContent);
            String chapterNumber = parseChapterNumber(chapterSiteContent);
            Map<String,String> verseMap = parseChapterVerses(chapterSiteContent);
            Map<String,String> storyMap = parseChapterStories(chapterSiteContent);
            //System.out.println(chapterNumber);
            //for (String verse : verseMap.keySet())
            //    System.out.println(verse+" = "+verseMap.get(verse));
            updateBibleChapter(book_number, String.valueOf(i+1), verseMap, dbFile);
            if (storyMap.size() > 0){
                if (isStoriesTableCleaned==false){
                    clearTable(dbFile, "stories");
                    isStoriesTableCleaned = true;
                }
                updateBibleStory(book_number, String.valueOf(i + 1), storyMap, dbFile);
            }
        }
    }

    /**
     * Loading site page's content
     * @return
     */
    String loadSiteContent(String sitePageUrl){
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(sitePageUrl);
        CloseableHttpResponse response = null;
        String bibleHomePage = "";
        try {
            response = httpclient.execute(httpGet);
            System.out.println(response.getStatusLine());
            HttpEntity entity = response.getEntity();
            // do something useful with the response body
            if (entity != null) {
                bibleHomePage = EntityUtils.toString(entity);
            }
            // and ensure it is fully consumed
            EntityUtils.consume(entity);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bibleHomePage;
    };

    /**
     * Parsing book_name list from book_list_page content
     * @param bookPage
     * @return
     */
    List<String> parseBookName(String bookPage){
        List<String> booknameList = new ArrayList<>();
        String bookName;
        String[] nameArray = bookPage.split("book-name\"><span class=\"expand icon-expand\"></span>");
        for (int i=1; i<nameArray.length; i++){
            bookName = nameArray[i];
            bookName = bookName.substring(0,bookName.indexOf("<span"));
            bookName = bookName.replaceAll(String.valueOf((char) 160), "").trim();
            booknameList.add(bookName);
        }
        return booknameList;
    }
    /**
     * Parsing chapter_url list from book_list_page content
     * @param bookPage
     * @return
     */
    Map<String,List<String>> loadChapterUrl(String bookPage){
        Map<String,List<String>> bookChapterUrlMap = new HashMap<>();
        String book, chapterUrl;
        String bookNumber;
        String[] bookArray = bookPage.split("<td class=\"chapters collapse\">");
        for (int i=1; i<bookArray.length; i++){
            List<String> chapterUrlList = new ArrayList<>();
            book = bookArray[i];
            String[] chapterUrlArray = book.split("href=\"");
            for (int j=1; j<chapterUrlArray.length; j++){
                chapterUrl = chapterUrlArray[j];
                chapterUrl = chapterUrl.substring(0,chapterUrl.indexOf("\""));
                chapterUrl = StringEscapeUtils.unescapeHtml4(chapterUrl);
                //System.out.println(chapterUrl);
                if (chapterUrl.indexOf("/passage/?search=")==0)
                    chapterUrlList.add(chapterUrl);
            }
            bookNumber = BOOK_NUMBER_ARRAY[i-1];
            bookChapterUrlMap.put(bookNumber, chapterUrlList);
        }
        return bookChapterUrlMap;
    }

    void loadBibleChapter(Map<String, List<String>> bookChapterUrlMap){
        List<String> chapterUrlList;
        String chapterUrl;
        String chapterSiteContent;
        boolean isStoriesTableCleaned = false;
        for (String book_number : bookChapterUrlMap.keySet()){

            //if (book_number=="660"){

            chapterUrlList = bookChapterUrlMap.get(book_number);
            for (int i=0; i<chapterUrlList.size(); i++){
                //if (i<1 || i>1)
                //   continue;
                chapterUrl = BASE_URL + chapterUrlList.get(i);
                //System.out.println(chapterUrl);
                chapterSiteContent = loadSiteContent(chapterUrl);
                //System.out.println(chapterSiteContent);
                String chapterNumber = parseChapterNumber(chapterSiteContent);
                Map<String,String> verseMap = parseChapterVerses(chapterSiteContent);
                Map<String,String> storyMap = parseChapterStories(chapterSiteContent);
                //System.out.println(chapterNumber);
                //for (String verse : verseMap.keySet())
                //    System.out.println(verse+" = "+verseMap.get(verse));
                updateBibleChapter(book_number, String.valueOf(i+1), verseMap, dbFile);
                if (storyMap.size() > 0){
                    if (isStoriesTableCleaned==false){
                        clearTable(dbFile, "stories");
                        isStoriesTableCleaned = true;
                    }
                    updateBibleStory(book_number, String.valueOf(i + 1), storyMap, dbFile);
                }
            }
            //System.out.println("updated book "+book_number);
            //break;
            //}
        }
    };

    String parseChapterNumber(String pageContent){
        String chapterMarker = "<span class=\"chapternum\">";
        String chapterNumber = pageContent.substring(pageContent.indexOf(chapterMarker)+chapterMarker.length());
        chapterNumber = chapterNumber.substring(0,chapterNumber.indexOf("</span"));
        chapterNumber = chapterNumber.replaceAll(String.valueOf((char) 160), "").trim();
        return chapterNumber;
    }
    Map<String,String> parseChapterVerses(String pageContent){
        Map<String,String> verseMap = new HashMap<>();
        String verse, verse_number = "1";
        String subtitle;
        String separator = "<br>";
        StringBuffer verse_text_sb = null;
        boolean isStoryTitle;
        boolean isSameVerse;
        String tempVerseStr = "";
        String tempSplitVerse;

        // process footnotes if it has
        String verseContent = "";
        String footnoteContent = "";
        if (pageContent.indexOf("<div class=\"footnotes") > -1){
            verseContent = pageContent.substring(0, pageContent.indexOf("<div class=\"footnotes"));
            footnoteContent = pageContent.substring(pageContent.indexOf("<div class=\"footnotes"));
            footnoteContent = footnoteContent.substring(0,footnoteContent.indexOf("</div>"));
        }else{
            verseContent = pageContent.substring(0, pageContent.indexOf("<div class=\"publisher-info-bottom"));
        }

        String redLetterSpan = "<span class=\"woj\">";
        // process the first verse first
        String chapterMarker = "<span class=\"chapternum\">";
        if (verseContent.indexOf(chapterMarker) > 0){
            // figure out if there is subtitle of this verse
            String verse_speaker_class = "<span class=\"verse_speaker\">";
            subtitle = verseContent.substring(0,verseContent.indexOf(chapterMarker));
            while (subtitle.indexOf("<h4 class=\"") > -1){
                subtitle = subtitle.substring(subtitle.indexOf("<h4 class=\""));
                subtitle = subtitle.substring(subtitle.indexOf("<span"));
                verse = subtitle.substring(subtitle.indexOf(">")+1,subtitle.indexOf("</span>"));
                if (verse_text_sb==null){
                    verse_text_sb = new StringBuffer(verse_speaker_class+"["+verse);
                }else{
                    verse_text_sb.append(separator);
                    verse_text_sb.append(verse_speaker_class+verse);
                }
            }
            if (verse_text_sb!=null){
                verse_text_sb.append("]"+"</span>");
            }
            // figure out the first verse
            verse = verseContent.substring(verseContent.indexOf(chapterMarker)+chapterMarker.length());
            verse = verse.substring(verse.indexOf("</span>")+7);
            verse = verse.substring(0,verse.indexOf("<span id="));

            tempVerseStr = "";

            if (verse_text_sb==null){
                verse_text_sb = new StringBuffer(tempVerseStr);

                while (verse.indexOf(redLetterSpan) > -1){
                    tempVerseStr = tempVerseStr + verse.substring(0,verse.indexOf("</span>")+7);
                    verse = verse.substring(verse.indexOf("</span>")+7);
                }
                tempVerseStr = tempVerseStr + verse.substring(0,verse.indexOf("</span>"));

                tempVerseStr = fixRefFootnote(tempVerseStr);
            }
            else{
                while (verse.indexOf(redLetterSpan) > -1){
                    tempVerseStr = verse.substring(0,verse.indexOf("</span>")+7);
                    verse = verse.substring(verse.indexOf("</span>")+7);
                }
                tempVerseStr += verse.substring(0,verse.indexOf("</span>"));

                tempVerseStr = fixRefFootnote(tempVerseStr);

                verse_text_sb.append(separator);
            }
            // check if begining of verse is highlighted
            if (verseContent.indexOf(redLetterSpan) > -1 &&
                    (verseContent.indexOf(redLetterSpan)+redLetterSpan.length() == verseContent.indexOf(chapterMarker))){
                tempVerseStr = redLetterSpan + tempVerseStr + "</span>";
            }

            verse_text_sb.append(tempVerseStr);

            // try to look for new line sentence in the same verse
            tempVerseStr = "";
            while(verse.indexOf("<p>") > -1){
                verse = verse.substring(verse.indexOf("<p>")+3);
                if (verse.indexOf("</p>") < 0){
                    // there is no new line, finish now
                    break;
                }else{
                    verse = verse.substring(verse.indexOf(">")+1);

                    while (verse.indexOf(redLetterSpan) > -1){
                        tempVerseStr = verse.substring(0,verse.indexOf("</span>")+7);
                        verse = verse.substring(verse.indexOf("</span>")+7);
                    }
                    tempVerseStr += verse.substring(0,verse.indexOf("</span>"));

                    tempVerseStr = fixRefFootnote(tempVerseStr);

                    verse_text_sb.append(separator);
                    verse_text_sb.append(tempVerseStr);
                }
            }
            tempVerseStr = "";
            while(verse.indexOf("<span class=\"text") > -1 && verse.indexOf("<br") > -1){
                isStoryTitle = false;
                // extract story subtitle if it has
                if (verse.indexOf("<h3>") + 4 == verse.indexOf("<span class=\"text")){
                    isStoryTitle = true;
                }
                verse = verse.substring(verse.indexOf("<span class=\"text"));
                verse = verse.substring(verse.indexOf(">")+1);
                verse_text_sb.append(separator);
                tempVerseStr = verse.substring(0,verse.indexOf("</span>"));
                tempVerseStr = fixRefFootnote(tempVerseStr);
                if (isStoryTitle==true){
                    tempVerseStr = "[" + tempVerseStr + "]";
                }
                verse_text_sb.append(tempVerseStr);
            }

            tempVerseStr = verse_text_sb.toString().replaceAll(String.valueOf((char) 160), " ").trim();

            verseMap.put("1",tempVerseStr);

        }
        // process the rest of verses
        String[] verseArray = verseContent.split("<sup class=\"versenum\">");
        // remove the first part (chapter) if existed
        if (verseArray.length > 0 && verseArray[0].indexOf(chapterMarker) > -1){
            verseArray[0] = verseArray[0].substring(verseArray[0].indexOf(chapterMarker));
        }

        for (int i=1; i<verseArray.length; i++){
            verse_text_sb = null;
            subtitle = verseArray[i-1];
            String verse_speaker_class = "<span class=\"verse_speaker\">";
            while (subtitle.indexOf("<h4 class=\"") > -1){
                subtitle = subtitle.substring(subtitle.indexOf("<h4 class=\""));
                subtitle = subtitle.substring(subtitle.indexOf("<span"));
                verse = subtitle.substring(subtitle.indexOf(">")+1,subtitle.indexOf("</span>"));
                if (verse_text_sb==null){
                    verse_text_sb = new StringBuffer(verse_speaker_class+"["+verse);
                }else{
                    verse_text_sb.append(separator);
                    verse_text_sb.append(verse_speaker_class+verse);
                }
            }
            if (verse_text_sb!=null){
                verse_text_sb.append("]"+"</span>");
            }

            verse = verseArray[i];
            if (verse.indexOf("<span id=") > -1)
                verse = verse.substring(0,verse.indexOf("<span id="));

            verse_number = verse.substring(0,verse.indexOf("</sup>"));
            isSameVerse = false;
            while (!Character.isDigit(verse_number.charAt(verse_number.length()-1))){
                if (Character.isAlphabetic(verse_number.charAt(verse_number.length()-1))){
                    if (verse_number.charAt(verse_number.length()-1)!= 'a')
                        isSameVerse = true;
                }
                // remove all non-digit character in the end of verse_number
                verse_number = verse_number.substring(0,verse_number.length()-1);
            }

            tempVerseStr = "";

            boolean isRedSpanClosed = false;
            if (verse_text_sb==null){

                verse_text_sb = new StringBuffer(tempVerseStr);

                String tempVerse = verse.substring(verse.indexOf("</sup>") + 6);
                if (tempVerse.indexOf("</p>") > -1)
                    tempVerse = tempVerse.substring(0,tempVerse.indexOf("</p>"));
                else if (tempVerse.indexOf("<span id=") > -1)
                    tempVerse = tempVerse.substring(0,tempVerse.indexOf("<span id="));

                int redSpanIndex = tempVerse.indexOf(redLetterSpan);
                while (redSpanIndex > -1){
                    if (tempVerse.indexOf("</span>") > -1 && tempVerse.indexOf("</span>") < redSpanIndex ) {
                        tempVerseStr = tempVerseStr + tempVerse.substring(0, redSpanIndex + redLetterSpan.length());
                        tempVerse = tempVerse.substring(redSpanIndex + redLetterSpan.length());
                        isRedSpanClosed = true;
                    }else{
                        tempVerseStr = tempVerseStr + tempVerse.substring(0,tempVerse.indexOf("</span>")+7);
                    }
                    tempVerse = tempVerse.substring(tempVerse.indexOf("</span>")+7);
                    redSpanIndex = tempVerse.indexOf(redLetterSpan);
                }
                if (verseArray[i-1].lastIndexOf(redLetterSpan) > -1 &&
                        (verseArray[i-1].lastIndexOf(redLetterSpan)+redLetterSpan.length() == verseArray[i-1].length())){

                    if (tempVerse.indexOf("</span>"+7) < tempVerse.lastIndexOf("</span>")){
                        tempVerseStr = tempVerseStr + tempVerse.substring(0,tempVerse.lastIndexOf("</span>"));
                        isRedSpanClosed = true;
                    }else{
                        tempVerseStr = tempVerseStr + tempVerse.substring(0,tempVerse.indexOf("</span>"));
                    }
                }else{
                    tempVerseStr = tempVerseStr + tempVerse.substring(0,tempVerse.indexOf("</span>"));
                }

                tempVerseStr = fixRefFootnote(tempVerseStr);

            }
            else{
                String tempVerse = verse.substring(verse.indexOf("</sup>") + 6);
                if (tempVerse.indexOf("</p>") > -1)
                    tempVerse = tempVerse.substring(0,tempVerse.indexOf("</p>"));
                else if (tempVerse.indexOf("<span id=") > -1)
                    tempVerse = tempVerse.substring(0,tempVerse.indexOf("<span id="));

                int redSpanIndex = tempVerse.indexOf(redLetterSpan);
                while (redSpanIndex > -1){
                    if (tempVerse.indexOf("</span>") > -1 && tempVerse.indexOf("</span>") < redSpanIndex ) {
                        tempVerseStr = tempVerseStr + tempVerse.substring(0, redSpanIndex + redLetterSpan.length());
                        tempVerse = tempVerse.substring(redSpanIndex + redLetterSpan.length());
                        isRedSpanClosed = true;
                    }else{
                        tempVerseStr = tempVerseStr + tempVerse.substring(0,tempVerse.indexOf("</span>")+7);
                    }
                    tempVerse = tempVerse.substring(tempVerse.indexOf("</span>")+7);
                    redSpanIndex = tempVerse.indexOf(redLetterSpan);
                }
                if (tempVerse.indexOf("</span>") < tempVerse.lastIndexOf("</span>")){
                    tempVerseStr = tempVerseStr + tempVerse.substring(0,tempVerse.lastIndexOf("</span>"));
                    isRedSpanClosed = true;
                }else{
                    tempVerseStr = tempVerseStr + tempVerse.substring(0,tempVerse.indexOf("</span>"));
                }

                tempVerseStr = fixRefFootnote(tempVerseStr);

                verse_text_sb.append(separator);
            }
            // check if the begining of verse is highlighted
            if (verseArray[i-1].lastIndexOf(redLetterSpan) > -1 &&
                    (verseArray[i-1].lastIndexOf(redLetterSpan)+redLetterSpan.length() == verseArray[i-1].length())){
                if(isRedSpanClosed)
                    tempVerseStr = redLetterSpan + tempVerseStr;
                else
                    tempVerseStr = redLetterSpan + tempVerseStr + "</span>";
            }

            verse_text_sb.append(tempVerseStr);
            // try to look for new line sentence in the same verse
            tempVerseStr = "";
            while(verse.indexOf("<p>") > -1){
                verse = verse.substring(verse.indexOf("<p>")+3);
                if (verse.indexOf("</p>") < 0 && verse.indexOf("</span>") < 0){
                    // there is no new line, finish now
                    break;
                }else{
                    verse = verse.substring(verse.indexOf(">")+1);
                    while (verse.indexOf(redLetterSpan) > -1){
                        tempVerseStr = tempVerseStr + verse.substring(0,verse.indexOf("</span>")+7);
                        verse = verse.substring(verse.indexOf("</span>")+7);
                    }
                    tempVerseStr = tempVerseStr + verse.substring(0,verse.indexOf("</span>"));

                    tempVerseStr = fixRefFootnote(tempVerseStr);

                    verse_text_sb.append(separator);
                    verse_text_sb.append(tempVerseStr);
                }
            }
            tempVerseStr = "";
            while(verse.indexOf("<span class=\"text") > -1 && (verse.indexOf("<p class=") > -1 || verse.indexOf("<br") > -1)){
                isStoryTitle = false;
                // extract story subtitle if it has
                if (verse.indexOf("<h3>") + 4 == verse.indexOf("<span class=\"text")){
                    isStoryTitle = true;
                }
                // remove subtitle h4 if it is existed
                if (verse.indexOf("<h4 class=\"") > -1 && (verse.indexOf("</h4>") > verse.indexOf("<span class=\"text")))
                    verse = verse.substring(verse.indexOf("</h4>")+5);

                verse = verse.substring(verse.indexOf("<span class=\"text"));
                verse = verse.substring(verse.indexOf(">")+1);

                if (verse.length()==0)  // check if this line contains the subtitle only (removed)
                    break;

                tempSplitVerse = verse;
                if (verse.indexOf("<span class=\"text") > -1){
                    tempSplitVerse = verse.substring(0,verse.indexOf("<span class=\"text"));
                    verse = verse.substring(verse.indexOf("<span class=\"text"));
                }

                if (tempSplitVerse.indexOf(redLetterSpan) > -1){
                    while (tempSplitVerse.indexOf(redLetterSpan) > -1){
                        tempVerseStr = tempVerseStr + tempSplitVerse.substring(0,tempSplitVerse.indexOf("</span>")+7);
                        tempSplitVerse = tempSplitVerse.substring(tempSplitVerse.indexOf("</span>")+7);
                    }
                }else{
                    tempVerseStr = tempSplitVerse.substring(0,tempSplitVerse.indexOf("</span>"));
                }

                //tempVerseStr = verse.substring(0,verse.indexOf("</span>"));
                tempVerseStr = fixRefFootnote(tempVerseStr);
                if (isStoryTitle==true){
                    tempVerseStr = "[" + tempVerseStr + "]";
                }

                verse_text_sb.append(separator);
                verse_text_sb.append(tempVerseStr);
            }

            tempVerseStr = verse_text_sb.toString().replaceAll(String.valueOf((char) 160), " ").trim();

            if (isSameVerse==true){
                tempVerseStr = verseMap.get(verse_number) + separator +  tempVerseStr;
                verseMap.replace(verse_number,tempVerseStr);
            }else{
                verseMap.put(verse_number, tempVerseStr);
            }
        }
        if (footnoteContent.length() > 0){
            String[] footnoteArray = footnoteContent.split("<li");
            String footnoteVerse, tempfootnoteStr;
            StringBuffer footnote = new StringBuffer("");
            String catchVerseMarker = "<a href";
            for (int i=1; i<footnoteArray.length; i++){
                footnote.append("- ");
                footnoteVerse = footnoteArray[i];
                footnoteVerse =footnoteVerse.substring(footnoteVerse.indexOf(catchVerseMarker));
                tempfootnoteStr =footnoteVerse.substring(footnoteVerse.indexOf(">")+1,footnoteVerse.indexOf("</a>"));
                footnote.append(tempfootnoteStr + " = ");
                tempfootnoteStr = footnoteVerse.substring(footnoteVerse.indexOf("<span"));
                tempfootnoteStr = tempfootnoteStr.substring(tempfootnoteStr.indexOf(">")+1,tempfootnoteStr.indexOf("</span>"));
                footnote.append(tempfootnoteStr);
                footnote.append(separator);
            }
            // using the old verse_number
            String tempStr =
                    verseMap.get(verse_number) +
                            separator +  separator +
                            footnote.toString().replaceAll(String.valueOf((char) 160), " ").trim();
            verseMap.put(verse_number,tempStr);
        }
        return verseMap;
    }
    String fixRefFootnote(String verseText){
        String supTagOpen = "<sup>";
        String supTagClose = "</sup>";
        StringBuffer verseTextBuffer = new StringBuffer("");
        while (verseText.indexOf("<sup data-fn=") > -1 && verseText.indexOf("<a href") > -1){
            // remove hyperlink references
            String href = verseText.substring(verseText.indexOf("<a href"),verseText.indexOf("</a>"));
            href = href.substring(href.indexOf(">")+1);
            verseTextBuffer.append(verseText.substring(0,verseText.indexOf("<sup data-fn=")) +
                    supTagOpen + "["+href+"]" + supTagClose);
            verseText = verseText.substring(verseText.indexOf("</sup>")+6);
        }
        verseTextBuffer.append(verseText);
        return verseTextBuffer.toString();
    }
    Map<String,String> parseChapterStories(String pageContent){
        Map<String,String> storyMap = new HashMap<>();
        String story, verse_number;

        // process the first story first
        String storyMarker = "<h3><span";
        if (pageContent.indexOf(storyMarker) > 0){

            // figure out the story
            String storyPageContent = pageContent.substring(pageContent.indexOf(storyMarker));

            while (storyPageContent.indexOf(storyMarker) > -1){
                story = storyPageContent.substring(storyPageContent.indexOf(storyMarker)+storyMarker.length());
                story = story.substring(story.indexOf(">")+1,story.indexOf("</span>"));

                // figure out the verse_number of the story
                verse_number = storyPageContent.substring(storyPageContent.indexOf(storyMarker));
                if (verse_number.indexOf("chapternum") > -1 && verse_number.indexOf("chapternum") < verse_number.indexOf("versenum")){
                    storyMap.put("1", story.replaceAll(String.valueOf((char) 160), " ").trim());
                }else if (verse_number.indexOf("versenum") > -1 && verse_number.indexOf("versenum") < verse_number.indexOf("</td>")){
                    verse_number = verse_number.substring(verse_number.indexOf("versenum"));
                    verse_number = verse_number.substring(verse_number.indexOf(">")+1, verse_number.indexOf("</sup>"));
                    while (!Character.isDigit(verse_number.charAt(verse_number.length()-1))){
                        // remove all non-digit character in the end of verse_number
                        verse_number = verse_number.substring(0,verse_number.length()-1);
                    }
                    storyMap.put(verse_number, story.replaceAll(String.valueOf((char) 160), " ").trim());
                }

                storyPageContent = storyPageContent.substring(storyPageContent.indexOf(storyMarker)+storyMarker.length()+1);
            }

        }
        return storyMap;
    }
    void updateBibleChapter(String book_number, String chapter, Map<String,String> verseMap,String file){
        SQLiteStatement st = null;
        System.out.println("UPDATED verses = " + book_number + ", " + chapter);
        try {
            // connect to the created database
            SQLiteConnection db = new SQLiteConnection(new File(file));
            db.open(true);

            db.exec("begin");

            st = db.prepare("UPDATE verses SET text = ? WHERE (book_number = ? AND chapter = ? AND verse = ?);");
            for(String verse : verseMap.keySet()){
                //System.out.println("book_name = "+booknameList.get(i));
                st.bind(1, verseMap.get(verse));
                st.bind(2, book_number);
                st.bind(3, chapter);
                st.bind(4, verse);
                st.step();
                st.reset();
            }

            db.exec("commit");

            st.dispose();

        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            try{
                if (st!=null)
                    st.dispose();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };
    void updateBibleStory(String book_number, String chapter, Map<String,String> storyMap,String file){
        SQLiteStatement st = null;
        System.out.println("UPDATED stories = " + book_number + ", " + chapter);
        try {
            // connect to the created database
            SQLiteConnection db = new SQLiteConnection(new File(file));
            db.open(true);

            db.exec("begin");

            st = db.prepare("REPLACE INTO stories (book_number, chapter, verse, title) VALUES (?,?,?,?);");
            for(String verse : storyMap.keySet()){
                //System.out.println("book_name = "+booknameList.get(i));
                st.bind(1, book_number);
                st.bind(2, chapter);
                st.bind(3, verse);
                st.bind(4, storyMap.get(verse));
                st.step();
                st.reset();
            }

            db.exec("commit");

            st.dispose();

        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            try{
                if (st!=null)
                    st.dispose();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };
    void clearTable(String file, String table){
        SQLiteStatement st = null;
        System.out.println("DELETE FROM " + table);
        try {
            // connect to the created database
            SQLiteConnection db = new SQLiteConnection(new File(file));
            db.open(true);

            st = db.prepare("DELETE FROM " + table);
            st.step();

            st.dispose();

        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            try{
                if (st!=null)
                    st.dispose();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };
}