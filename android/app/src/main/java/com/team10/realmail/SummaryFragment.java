package com.team10.realmail;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.team10.realmail.api.SensorsApi;
import com.team10.realmail.api.SensorsData;
import com.team10.realmail.api.SensorsRequest;

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

    protected TextView username, timestamp, dailyMail;//declaring textview
    protected ImageView sensorPicture; //declaring imageview
    private FirebaseAuth auth; //
    private FirebaseFirestore database;
    private String firstName, lastName;
    private List<historyListItem> historyList = new ArrayList();//list of mail items,ojects from the histroy list item
    private List<historyListItem> dailyList = new ArrayList<>();//daily mailitem


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

    //on creatview where the backend is happening and ,ui component intract with ch other
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment,get the infor from the summary layout
        View view = inflater.inflate(R.layout.fragment_summary, container, false);


        //Finds the view with that ID in the layout
        username = view.findViewById(R.id.userName);//
        timestamp = view.findViewById(R.id.timestamp_summary);
        sensorPicture = view.findViewById(R.id.sensorPicture);
        dailyMail = view.findViewById(R.id.dailyMail);

        auth = FirebaseAuth.getInstance();//get instance of the database to insitiate of auth database

        getCurrentUserName(); // function

        // getting the today;date and time
        Calendar calendar = Calendar.getInstance();


        calendar.set(Calendar.HOUR_OF_DAY, 0);//setting the hr to zero at the beginning
        calendar.set(Calendar.MINUTE, 0); //setting the min to zero at the beginning
        calendar.set(Calendar.SECOND, 0); //setting the sec to zero at the beginning
        calendar.set(Calendar.MILLISECOND, 0); //setting the mil sec to zero at the beginning

        long todayInMillis = calendar.getTimeInMillis(); //getting the today's time in mili sec

        //fetching the mail receive from the database
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

                        //try this code and if their is excetion(error) so it catches
                        try {
                            //each timestemp is ISo format, tell the program ,there is date with this formate
                            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());

                            //pass the history list,first index and we get the current time occurance
                            Date date = isoFormat.parse(historyList.get(0).getTimeOfOccurence());

                            //month day hr min am/pm
                            SimpleDateFormat desiredFormat = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());
                            desiredFormat.setTimeZone(TimeZone.getDefault());

                            String formattedDate = desiredFormat.format(date);//iso format to desire format and store it format of date


                            timestamp.setText(formattedDate);//display format date in timestamp textview
                        } catch (ParseException e) { //catch exception
                            throw new RuntimeException(e); //display excption
                        }

                    }


                    for (historyListItem item : historyList) { // history list iteration

                        try {
                            Instant instant = Instant.parse(item.getTimeOfOccurence()); //time of occrance
                            long timestamp = instant.toEpochMilli();// convert time of occurance to milisec

                            if (timestamp >= todayInMillis) {
                                dailyList.add(item); //item from loop,it loops every single history list item
                            }
                        } catch (DateTimeParseException e) {// catch the error
                            e.printStackTrace();
                        }
                    }

                    if (dailyList.size() > 1) {
                        dailyMail.setText(dailyList.size() + " mails");
                    } else {
                        dailyMail.setText(dailyList.size() + " mail");

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


    private void getCurrentUserName() {
        String user = auth.getCurrentUser().getUid(); //store userid in string

        Log.d("Firestore", user);
        database = FirebaseFirestore.getInstance();// get the info from firestore database

        // go to folder and sub folder
        database.collection("users")
                .document(user)
                .collection("User Info")
                .document("User Names")
                .get()// get the content
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult(); //get the content n stor it in docu snapshot
                        if (document.exists()) {// if docu exist, store first n last name in strings
                            firstName = document.getString("firstName");
                            lastName = document.getString("lastName");


                            username.setText(firstName + " " + lastName); //set the textview tpo first n last


                            Log.d("Firestore", "Name: " + firstName + " " + lastName);
                        } else {
                            Log.d("DEBUG_DOC", "Raw Document Data: " + document.getData());
                            Log.d("Firestore", "Document does not exist.");
                        }
                    } else {
                        Log.w("Firestore", "Failed to fetch document.", task.getException());
                    }
                });

        username.setText(firstName + " " + lastName);
    }

    private void fetchPicture() {

        StorageReference rootRef = FirebaseStorage.getInstance().getReference();
        rootRef.listAll().addOnSuccessListener(listResult -> {

            List<String> folderNames = new ArrayList<>();
            for (StorageReference prefix : listResult.getPrefixes()) {
                folderNames.add(prefix.getName());
            }

            // current time first
            Collections.sort(folderNames, Collections.reverseOrder());

            // Get latest folder
            String latestFolder = folderNames.get(0);
            StorageReference latestFolderRef = rootRef.child(latestFolder);

            // bring the List of files in that folder
            latestFolderRef.listAll().addOnSuccessListener(folderContents -> {
                for (StorageReference fileRef : folderContents.getItems()) {
                    // get the first image
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {

                        //glide is open source tool tht allow to display images
                        Glide.with(getContext())
                                .load(uri.toString())
                                .into(sensorPicture);
                    });
                    break;
                }
            });

        }).addOnFailureListener(e -> {
            Log.e("Firebase", "Failed to list folders", e);//if it fails log the error
        });
    }
}
