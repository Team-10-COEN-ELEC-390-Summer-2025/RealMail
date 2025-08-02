package com.team10.realmail;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.team10.realmail.api.SensorsApi;
import com.team10.realmail.api.SensorsData;
import com.team10.realmail.api.SensorsRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.sql.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SummaryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SummaryFragment extends Fragment {

    protected TextView username, timestamp, dailyMail;
    protected ImageView sensorPicture;
    private FirebaseAuth auth;
    private FirebaseFirestore database;
    private String firstName, lastName;
    private List<historyListItem> historyList = new ArrayList();
    private List<historyListItem> dailyList = new ArrayList<>();


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";



    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public SummaryFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SummaryFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SummaryFragment newInstance(String param1, String param2) {
        SummaryFragment fragment = new SummaryFragment();
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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_summary, container, false);

        username = view.findViewById(R.id.userName);
        timestamp = view.findViewById(R.id.timestamp_summary);
        sensorPicture = view.findViewById(R.id.sensorPicture);
        dailyMail = view.findViewById(R.id.dailyMail);

        auth = FirebaseAuth.getInstance();//get instance

        getCurrentUserName();

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long todayInMillis = calendar.getTimeInMillis();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://us-central1-realmail-39ab4.cloudfunctions.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        SensorsApi api = retrofit.create(SensorsApi.class);

        // Hardcoded request
        SensorsRequest request = new SensorsRequest("1234567890", "sanomihigobertin@gmail.com");
        Call<List<SensorsData>> call = api.getSensorsWithMotionDetected(request);
        call.enqueue(new Callback<List<SensorsData>>() {
            @Override
            public void onResponse(Call<List<SensorsData>> call, Response<List<SensorsData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    historyList.clear();
                    for (SensorsData data : response.body()) {
                        // Use data.timestamp for the date
                        historyList.add(new historyListItem(
                                true, // Always show "New Mail"
                                data.timestamp // Correct field for date
                        ));


                        try {

                            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());


                            Date date = isoFormat.parse(historyList.get(0).getTimeOfOccurence());


                            SimpleDateFormat desiredFormat = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());
                            desiredFormat.setTimeZone(TimeZone.getDefault());

                            String formattedDate = desiredFormat.format(date);


                            timestamp.setText(formattedDate);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }

                    }

                    for(historyListItem item : historyList){
                        try {
                            Instant instant = Instant.parse(item.getTimeOfOccurence());
                            long timestamp = instant.toEpochMilli();

                            if (timestamp >= todayInMillis) {
                                dailyList.add(item);
                            }
                        } catch (DateTimeParseException e) {
                            e.printStackTrace();
                        }
                    }

                    if(dailyList.size() > 1){
                        dailyMail.setText(dailyList.size()+" mails");
                    } else{
                        dailyMail.setText(dailyList.size()+" mail");

                    }


                }
            }

            @Override
            public void onFailure(Call<List<SensorsData>> call, Throwable t) {
                // Handle error (e.g., show a Toast)
            }
        });

        fetchPicture();



        return view;
    }

    private void getCurrentUserName(){
        String user = auth.getCurrentUser().getUid();
        Log.d("Firestore", user);
        database = FirebaseFirestore.getInstance();

        database.collection("users")
                .document(user)
                .collection("User Info")
                .document("User Names")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            firstName = document.getString("firstName");
                            lastName = document.getString("lastName");

                            username.setText(firstName+" "+lastName);

                            Log.d("Firestore", "Name: " + firstName + " " + lastName);
                        } else {
                            Log.d("DEBUG_DOC", "Raw Document Data: " + document.getData());
                            Log.d("Firestore", "Document does not exist.");
                        }
                    } else {
                        Log.w("Firestore", "Failed to fetch document.", task.getException());
                    }
                    });

        username.setText(firstName+" "+lastName);
    }

    private void fetchPicture(){

        StorageReference rootRef = FirebaseStorage.getInstance().getReference();
        rootRef.listAll().addOnSuccessListener(listResult -> {

            List<String> folderNames = new ArrayList<>();
            for (StorageReference prefix : listResult.getPrefixes()) {
                folderNames.add(prefix.getName()); // e.g., 1234567890_20250802T173418
            }

            // Sort the folders in descending order (latest timestamp first)
            Collections.sort(folderNames, Collections.reverseOrder());

            // Get the latest folder
            String latestFolder = folderNames.get(0);
            StorageReference latestFolderRef = rootRef.child(latestFolder);

            // List files in that folder
            latestFolderRef.listAll().addOnSuccessListener(folderContents -> {
                for (StorageReference fileRef : folderContents.getItems()) {
                    // Assume you only want the first image
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        Glide.with(getContext())
                                .load(uri.toString())
                                .into(sensorPicture);
                    });
                    break; // Only use the first image
                }
            });

        }).addOnFailureListener(e -> {
            Log.e("Firebase", "Failed to list folders", e);
        });
    }
}
