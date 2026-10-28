package edu.utsa.cs3443.picpick;
import edu.utsa.cs3443.picpick.model.Photo;
public class TrashActivity extends PhotoGridActivity {
    @Override protected Photo.Status category() { return Photo.Status.TRASH; }
}
