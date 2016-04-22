package com.matthiasko.scrollforreddit;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by matthiasko on 4/4/16.
 */
public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {

    private boolean mTwoPane; // TODO: change this

    private final ArrayList<ScrollComment> mValues;

    private Context context;

    public CommentsAdapter(Context context, ArrayList<ScrollComment> items) {

        this.context = context;
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.comments_card_view, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        holder.mItem = mValues.get(position);

        int commentDepth = mValues.get(position).getDepth();

        //System.out.println("commentDepth = " + commentDepth);

        if (commentDepth > 1) {

            // set new indentation by multiplying comment depth
            int indentedMarginSize = commentDepth * 10;

            //System.out.println("commentDepth = " + commentDepth);

            //int bodyCharCount = mValues.get(position).getBody().length();

            //int totalIndent = bodyCharCount + commentDepth;

            //String indentedComment = mValues.get(position).getBody().replaceAll("(?m)^", "\t");

            //String indentedComment = String.format("%" + totalIndent + "s", mValues.get(position).getBody());

            holder.commentBody.setText(mValues.get(position).getBody());

            // setting margins dynamically here depending on the comment level
            // margins are set in pixels, might have to convert to dp...
            ViewGroup.MarginLayoutParams commentMargins = (ViewGroup.MarginLayoutParams) holder.commentBody
                    .getLayoutParams();

            commentMargins.setMargins(indentedMarginSize, 0, 0, 0);

            ViewGroup.MarginLayoutParams authorMargin = (ViewGroup.MarginLayoutParams) holder.commentAuthor
                    .getLayoutParams();

            authorMargin.setMargins(indentedMarginSize, 0, 0, 0);

            //System.out.println("indentedComment = " + indentedComment);

        } else {

            holder.commentBody.setText(mValues.get(position).getBody());
        }

        //holder.commentBody.setText(mValues.get(position).getBody());
        holder.commentAuthor.setText(mValues.get(position).getAuthor());
        holder.commentPoints.setText(String.valueOf(mValues.get(position).getScore()));

        //System.out.println("holder.commentBody = " + holder.commentBody);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // this should happen on a long click
                // get all comment children
                // remove them from current array
                // insert/replace 'hidden comment' for top level comment

                //String postId = mValues.get(position).postId;

                //System.out.println("postId = " + postId);

                //CommentNode commentNode = mValues.get(position).postCommentNode;

                //System.out.println("commentNode.getTotalSize() = " + commentNode.getTotalSize());

                //String testTitle = mValues.get(position).postTitle;

                //System.out.println("testTitle = " + testTitle);

                /*
                String commentAuthor = commentNode.getComment().getAuthor();
                int commentPoints = commentNode.getComment().getScore();
                Date commentTime = commentNode.getComment().getCreated();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(commentTime.toString(), Locale.US);
                String commentText = commentNode.getComment().getBody();
                */

            }
        });
    }

    @Override
    public int getItemCount() {

        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final View mView;
        public ScrollComment mItem;
        TextView commentBody;
        TextView commentAuthor;
        TextView commentPoints;

        public ViewHolder(View view) {
            super(view);
            mView = view;

            commentBody = (TextView) view.findViewById(R.id.commentBody);
            commentAuthor = (TextView) view.findViewById(R.id.commentAuthor);
            commentPoints = (TextView) view.findViewById(R.id.commentPoints);
        }
    }
}
