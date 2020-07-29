package com.mdavison.standup.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mdavison.standup.R;
import com.mdavison.standup.activities.CommunityDetailsActivity;
import com.mdavison.standup.activities.PostDetailsActivity;
import com.mdavison.standup.adapters.CommunityAdapter;
import com.mdavison.standup.models.Community;
import com.mdavison.standup.support.EndlessRecyclerViewScrollListener;
import com.mdavison.standup.support.Extras;
import com.mdavison.standup.support.ItemClickSupport;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

/**
 * This Fragment shows an explore feed, featuring recommended content and the
 * ability to search for Communities, Posts, and Users.
 */
public class ExploreFragment extends Fragment {
    private static final String TAG = "ExploreFragment";
    private List<Community> communities;
    private CommunityAdapter communityAdapter;
    private EditText etSearch;

    public ExploreFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        communities = new ArrayList<>();
        etSearch = view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i,
                                          int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1,
                                      int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                communityAdapter.clear();
                queryCommunities();
            }
        });
        final RecyclerView rvContent = view.findViewById(R.id.rvContent);
        communityAdapter = new CommunityAdapter(getContext(), communities);
        rvContent.setAdapter(communityAdapter);
        final LinearLayoutManager layoutManager =
                new LinearLayoutManager(getContext());
        rvContent.setLayoutManager(layoutManager);
        queryCommunities();
        ItemClickSupport.addTo(rvContent).setOnItemClickListener(
                new ItemClickSupport.OnItemClickListener() {
                    @Override
                    public void onItemClicked(RecyclerView recyclerView,
                                              int position, View v) {
                        Intent i = new Intent(getContext(),
                                CommunityDetailsActivity.class);
                        i.putExtra(Extras.EXTRA_COMMUNITY,
                                Parcels.wrap(communities.get(position)));
                        startActivity(i);
                    }
                });
        final EndlessRecyclerViewScrollListener scrollListener =
                new EndlessRecyclerViewScrollListener(layoutManager) {
                    @Override
                    public void onLoadMore(int page, int totalItemsCount,
                                           RecyclerView view) {
                        queryCommunities();
                    }
                };
        rvContent.addOnScrollListener(scrollListener);
    }

    private void queryCommunities() {
        if (etSearch.getText().toString().isEmpty()) {
            ParseRelation<Community> communitiesRelation =
                    ParseUser.getCurrentUser().getRelation("communities");
            communitiesRelation.getQuery()
                    .findInBackground((newCommunities, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Issue with getting communities", error);
                            Toast.makeText(getContext(), "Issue getting communities",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        communityAdapter.clear();
                        communityAdapter.addAll(newCommunities);
                        Log.i(TAG, "Received " + newCommunities.size() +
                                " communities");
                    });
        } else {
            ParseQuery<Community> query = ParseQuery.getQuery(Community.class);
            query.setLimit(20);
            query.setSkip(communities.size());
            query.addDescendingOrder(Community.KEY_USER_COUNT);
            query.whereStartsWith(Community.KEY_NAME, etSearch.getText().toString());
            query.findInBackground((newCommunities, error) -> {
                if (error != null) {
                    Log.e(TAG, "Issue searching communities", error);
                    Toast.makeText(getContext(), "Issue searching communities",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                communityAdapter.addAll(newCommunities);
                Log.i(TAG,
                        "Received " + newCommunities.size() + " communities");
            });
        }
    }
}