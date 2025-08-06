package com.team10.realmail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.team10.realmail.api.SensorsApi;
import com.team10.realmail.api.SensorsData;
import com.team10.realmail.api.SensorsRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HistoryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HistoryFragment extends Fragment {

    protected RecyclerView recyclerView;
    private historyListAdapter adapter;
    private List<historyListItem> historyList = new ArrayList<>();
    private List<HistoryListDisplayItem> displayList = new ArrayList<>();
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HistoryFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HistoryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HistoryFragment newInstance(String param1, String param2) {
        HistoryFragment fragment = new HistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerView = view.findViewById(R.id.historylist);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new historyListAdapter(displayList, getContext());
        recyclerView.setAdapter(adapter);

        // Retrofit setup
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://us-central1-realmail-39ab4.cloudfunctions.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        SensorsApi api = retrofit.create(SensorsApi.class);

        // Get email from Firebase authenticated user
        String userEmail = null;
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userEmail = user.getEmail();
        }
        SensorsRequest request = new SensorsRequest(null, userEmail);
        Call<List<SensorsData>> call = api.getSensorsWithMotionDetected(request);
        call.enqueue(new Callback<List<SensorsData>>() {
            @Override
            public void onResponse(Call<List<SensorsData>> call, Response<List<SensorsData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayList.clear();
                    // Group by device_id
                    Map<String, List<historyListItem>> grouped = new LinkedHashMap<>();
                    for (SensorsData data : response.body()) {
                        String deviceId = data.device_id;
                        historyListItem item = new historyListItem(
                                true,
                                data.timestamp
                        );
                        if (!grouped.containsKey(deviceId)) {
                            grouped.put(deviceId, new ArrayList<>());
                        }
                        grouped.get(deviceId).add(item);
                    }
                    // Flatten to displayList with headers
                    for (Map.Entry<String, List<historyListItem>> entry : grouped.entrySet()) {
                        displayList.add(new HistoryListDisplayItem(HistoryListDisplayItem.TYPE_HEADER, entry.getKey(), null));
                        for (historyListItem item : entry.getValue()) {
                            displayList.add(new HistoryListDisplayItem(HistoryListDisplayItem.TYPE_ITEM, entry.getKey(), item));
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<SensorsData>> call, Throwable t) {
                // Handle error (e.g., show a Toast)
            }
        });

        // Inflate the layout for this fragment
        return view;
    }
}