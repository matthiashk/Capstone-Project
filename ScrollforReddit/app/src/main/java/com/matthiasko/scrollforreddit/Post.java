package com.matthiasko.scrollforreddit;

/**
 * Created by matthiasko on 4/1/16.
 */
public class Post {

    public String postTitle;
    public String postSubreddit;
    public String postAuthor;
    public String postSource;
    public String postThumbnail;
    public int postPoints;
    public int postNumberOfComments;
    //public CommentNode postCommentNode;
    public String postId;
    public String postDomain;


    public Post(String postTitle, String postSubreddit, String postAuthor, String postSource,
                String postThumbnail, int postPoints, int postNumberOfComments, String postId,
                String postDomain) {

        this.postTitle = postTitle;
        this.postSubreddit = "r/" + postSubreddit;
        this.postAuthor = postAuthor;
        this.postSource = postSource;
        this.postThumbnail = postThumbnail;
        this.postPoints = postPoints;
        this.postNumberOfComments = postNumberOfComments;
        //this.postCommentNode = postCommentNode;
        this.postId = postId;
        this.postDomain = postDomain;

    }

    /* // for parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
        public Post createFromParcel(Parcel pc) {
            return new Post(pc);
        }

        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    public Post (Parcel source) {

    }
    */
}
