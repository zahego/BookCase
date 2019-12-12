package temple.edu.bookcase;

import java.io.Serializable;

public class Book implements Serializable {
    int id;
    String title;
    String author;
    int published;
    String coverURL;
    int duration;
    boolean downloaded;
    int progress;
    public Book(int id, String title, String author, int published, String coverURL, int duration, boolean downloaded, int progress){
        this.id=id;
        this.title=title;
        this.author=author;
        this.published=published;
        this.coverURL=coverURL;
        this.duration=duration;
        this.downloaded=downloaded;
        this.progress=progress;
    }
}
