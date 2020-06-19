package com.android.MakeYouStudy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Random;

public class AttendanceCheckActivity extends AppCompatActivity {

    private static final String TAG = "MyActivity"; //
    // 출석체크시 Activity 구분을 위한 숫자
    final int LABEL_ACTIVITY = 1;
    final int TEXT_ACTIVITY = 2;
    final int IMAGE_MATCHING_ACTIVITY = 3;
    // Button
    private Button btnCheck;
    private Button btnTextCheck;
    private Button btnSkip;
    private Button btnOpencv;
    // ImageLabel 인식불가 count num
    private int count;
    // Text Recognition 랜덤 문자
    private Random rnd;
    private String[] randomText;
    // Skip dialog
    private Activity activity;
    // Alarm object
    private AlarmManager alarmManager;
    private int reqCode;
    private Intent sintent;
    private Context context;
    //firebase
    FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef;
    DatabaseReference mdatabase = FirebaseDatabase.getInstance().getReference();

    int size;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_check);

        randomText = getResources().getStringArray(R.array.random_text);
        rnd = new Random();
        this.context = this;

        // AlarmService의 mediaPlayer제어를 위한 Intent
        sintent = new Intent(context, AlarmService.class);
        // AlarmReceiver를 위한 object와 intent
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = getIntent();
        reqCode = intent.getIntExtra("reqCode", -1);
        Log.d("reqTest", " 받아온 reqCode값은 : " + reqCode);

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference().child("images").child(user.getUid());

        checksize();

        btnCheck = (Button)findViewById(R.id.btnCheck);
        btnCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPause();
                startActivityForResult(new Intent(getApplicationContext(), ImageLabelActivity.class), LABEL_ACTIVITY);
            }
        });

        btnTextCheck = (Button)findViewById(R.id.btnTextCheck);
        btnTextCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPause();
                textRecognition();
            }
        });

        btnSkip = (Button)findViewById(R.id.btnSkip);
        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogSkip();
            }
        });

        btnOpencv = (Button)findViewById(R.id.btnOpencv);
        btnOpencv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(size < 4)
                {
                    dialogImageMatching();
                }else {
                    startActivityForResult(new Intent(getApplicationContext(), ImageMatchingActivity.class), IMAGE_MATCHING_ACTIVITY);
                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checksize();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case LABEL_ACTIVITY:

                String label = data.getStringExtra("labeling");

                if(label.equals("Desk") || label.equals("Table") ){
                    Toast.makeText(this, "Label 출석체크 완료 : "+label, Toast.LENGTH_SHORT).show();
                    alarmOff();
                    Log.d(TAG, "실행");
                    finish();
                }
                else if(label.equals("BackPressed")){
                    Toast.makeText(this, "Label 출석체크 취소", Toast.LENGTH_SHORT).show();
                    mediaRestart();
                }
                else {
                    Toast.makeText(this, "출석체크 실패 : "+label, Toast.LENGTH_SHORT).show();
                    count++;

                    Log.d("count_number", ""+count);

                    if(count>=3){ // count 가 3일 때 (사물인식 출석체크 3번 실패 시) count = 0으로 셋팅후 textRecognition 메소드 실행(text 인식 출석체크 Activity 실행)
                        count = 0;
                        Log.d("count_reset", ""+count);

                        textRecognition();
                    }
                }
                break;
            case TEXT_ACTIVITY :
                boolean checkValue = data.getBooleanExtra("checkValue", false);
                if(checkValue == true){
                    Toast.makeText(this, "Text 출석체크 완료", Toast.LENGTH_SHORT).show();
                    alarmOff();
                    finish();
                }else {
                    Toast.makeText(this, "Text 출석체크 취소", Toast.LENGTH_SHORT).show();
                    mediaRestart();
                }
                break;
            case IMAGE_MATCHING_ACTIVITY :
                boolean checkMatching = data.getBooleanExtra("checkMatching", false);
                if(checkMatching == true){
                    Toast.makeText(this, "ImageMatching 출석체크 완료", Toast.LENGTH_SHORT).show();
                    finish();
                }else {
                    Toast.makeText(this, "ImageMatching 출석체크 취소", Toast.LENGTH_SHORT).show();
                }
        }
    }


    public void textRecognition(){
        Log.d("textRecognition_method", "start");
        Intent intent = new Intent(this, TextRecognitionActivity.class );

        int num = rnd.nextInt(9);

        intent.putExtra("English", randomText[num]);

        startActivityForResult(intent, TEXT_ACTIVITY);
    }

    public void dialogSkip(){
        activity = this;
        AlertDialog.Builder alertdialog = new AlertDialog.Builder(activity);
        alertdialog.setMessage("출석여부를 고르세요.");

        // 확인버튼 - 결석
        alertdialog.setPositiveButton("결석", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(activity, "결석처리 되었습니다.", Toast.LENGTH_SHORT).show();
                alarmOff();
                finish();
            }
        });
        // 취소버튼
        alertdialog.setNegativeButton("출석", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(activity, "출석처리 되었습니다.", Toast.LENGTH_SHORT).show();

                finish();
            }
        });
        alertdialog.setNeutralButton("취소", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int id)
            {
                Toast.makeText(activity, "'취소'버튼을 누르셨습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        AlertDialog alert = alertdialog.create();
        alert.setTitle("Skip");
        alert.show();
    }

    public void dialogImageMatching(){
        activity = this;

        AlertDialog.Builder alertdialog = new AlertDialog.Builder(activity);
        alertdialog.setMessage("자신의 책상사진 5장을 등록해야 사용 가능합니다. 등록하시겠습니까?");

        // 확인버튼 - 등록
        alertdialog.setPositiveButton("등록하러가기", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
            }
        });

        // 취소버튼
        alertdialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(activity, "취소 되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });


        AlertDialog alert = alertdialog.create();
        alert.setTitle("사진을 등록하세요!");

        alert.show();

    }

    public void checksize(){
        mdatabase.child("image").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() == null){
                    Log.d("Checksize : ", "Uid child is null");
                    // 처음 등록할 때 size값과 position값을 초기화시켜준다.
                    mdatabase.child("image").child(user.getUid()).child("size").setValue("0");
                    mdatabase.child("image").child(user.getUid()).child("position").setValue("0");
                    size = 0;

                }else{
                    size = Integer.parseInt(dataSnapshot.child("size").getValue(String.class));
                }

            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    // media를 pause하기위한 Service호출
    public void mediaPause(){
        sintent.putExtra("state", "pause");
        // Oreo(26) 버전 이후부터는 Background 에서 실행을 금지하기 때문에 Foreground 에서 실행해야 함
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(sintent);
        } else {
            context.startService(sintent);
        }
    }
    // pause된 media를 restart하기 위한 Service호출
    public void mediaRestart(){
        sintent.putExtra("state", "restart");
        // Oreo(26) 버전 이후부터는 Background 에서 실행을 금지하기 때문에 Foreground 에서 실행해야 함
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(sintent);
        } else {
            context.startService(sintent);
        }
    }
    // 실행중인 alarm을 삭제
    public void alarmOff(){
        // AlarmReceiver
        Intent intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        intent.putExtra("state","off");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
        sendBroadcast(intent);
        Log.d("ReqTest", reqCode + " 의 pendingintent 알람이 해제되었습니다.");
    }
}