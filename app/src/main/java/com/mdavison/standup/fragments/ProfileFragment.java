package com.mdavison.standup.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.mdavison.standup.R;
import com.mdavison.standup.activities.PostDetailsActivity;
import com.mdavison.standup.adapters.PostAdapter;
import com.mdavison.standup.models.Post;
import com.mdavison.standup.models.User;
import com.mdavison.standup.support.EndlessRecyclerViewScrollListener;
import com.mdavison.standup.support.Extras;
import com.mdavison.standup.support.ImageHelp;
import com.mdavison.standup.support.ItemClickSupport;
import com.mdavison.standup.support.RequestCodes;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.parceler.Parcels;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static android.os.Environment.DIRECTORY_PICTURES;

/**
 * This Fragment shows details about a user.
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final String PHOTO_FILENAME = "photo.jpg";
    private List<Post> userPosts;
    private PostAdapter postAdapter;
    private ParseUser user;
    private ImageView ivProfile;
    private File photoFile;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivProfile = view.findViewById(R.id.ivProfile);
        final ImageView ivLogout = view.findViewById(R.id.ivLogout);
        final ParseUser selectedUser = Parcels.unwrap(getActivity().getIntent()
                .getParcelableExtra(Extras.EXTRA_USER));
        if (selectedUser == null) {
            user = ParseUser.getCurrentUser();
            ivLogout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ParseUser.logOut();
                    getActivity().finish();
                }
            });
            ivProfile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                        startActivityForResult(intent, RequestCodes.PICK_PHOTO_CODE);
                    }
                }
            });
        } else {
            user = selectedUser;
            ivLogout.setVisibility(View.INVISIBLE);
        }
        final TextView tvUsername = view.findViewById(R.id.tvUsername);
        tvUsername.setText(user.getUsername());
        final ImageView ivProfile = view.findViewById(R.id.ivProfile);
        ParseFile profileImage = user.getParseFile("picture");
        if (profileImage != null) {
            Glide.with(getContext()).load(profileImage.getUrl())
                    .circleCrop().into(ivProfile);
        } else {
            Glide.with(getContext()).clear(ivProfile);
        }
        userPosts = new ArrayList<>();
        final RecyclerView rvPosts = view.findViewById(R.id.rvPosts);
        postAdapter = new PostAdapter(getContext(), userPosts);
        rvPosts.setAdapter(postAdapter);
        final LinearLayoutManager layoutManager =
                new LinearLayoutManager(getContext());
        rvPosts.setLayoutManager(layoutManager);
        queryPosts();
        ItemClickSupport.addTo(rvPosts).setOnItemClickListener(
                new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView,
                                              int position, View v) {
                        Intent i = new Intent(getContext(),
                                PostDetailsActivity.class);
                        i.putExtra(Extras.EXTRA_POST,
                                Parcels.wrap(userPosts.get(position)));
                        startActivity(i);
                    }
                });
        final EndlessRecyclerViewScrollListener scrollListener =
                new EndlessRecyclerViewScrollListener(layoutManager) {
                    @Override
                    public void onLoadMore(int page, int totalItemsCount,
                                           RecyclerView view) {
                        queryPosts();
                    }
                };
        rvPosts.addOnScrollListener(scrollListener);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.PICK_PHOTO_CODE && data != null) {
            Uri photoUri = data.getData();
            Bitmap selectedImage =
                    ImageHelp.loadFromUri(getContext(), photoUri);
            File mediaStorageDir = new File(
                    getContext().getExternalFilesDir(DIRECTORY_PICTURES), TAG);
            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
                Log.e(TAG, "failed to create directory");
                Toast.makeText(getContext(), "Failed to select photo",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            String destinationFilename =
                    mediaStorageDir.getPath() + File.separator + PHOTO_FILENAME;
            try (FileOutputStream out = new FileOutputStream(
                    destinationFilename)) {
                selectedImage.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "failed to save photo");
                Toast.makeText(getContext(), "Failed to select photo",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            photoFile = new File(destinationFilename);
            Glide.with(getContext()).load(photoFile)
                    .circleCrop().into(ivProfile);
            user.put(User.KEY_PICTURE, new ParseFile(photoFile));
            user.saveInBackground();
        }
    }

    private void queryPosts() {
        ParseQuery<Post> query = ParseQuery.getQuery(Post.class);
        query.whereEqualTo(Post.KEY_AUTHOR, user);
        query.setLimit(20);
        query.setSkip(userPosts.size());
        query.addDescendingOrder(Post.KEY_CREATED_AT);
        query.findInBackground((newPosts, error) -> {
            if (error != null) {
                Log.e(TAG, "Issue with getting posts", error);
                Toast.makeText(getContext(), "Issue getting posts",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            postAdapter.addAll(newPosts);
        });
    }

    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoFile =
                ImageHelp.getPhotoFileUri(getContext(), TAG, PHOTO_FILENAME);
        Uri fileProvider = FileProvider.getUriForFile(getContext(),
                "com.codepath.fileprovider.standup", photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);
        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
            startActivityForResult(intent,
                    RequestCodes.CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    }
}
