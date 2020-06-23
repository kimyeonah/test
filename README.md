#  출석체크 

출석체크 기능은 공부를 하기 위해 책상에 앉을 수 있도록 도와주는 기능이다. 
Time Table에서 시간표를 설정한 후 지정한 시간에 알람이 울리면 출석체크를 실행한다.
출석체크 방법에는 세 가지가 있다.
- firebase ML Kit를 이용한 사물(책상)인식
- firebase ML Kit를 이용한 Text 인식 
- openCV를 이용한 color histogram image Matching

우선, firebase ML Kit와 openCV를 사용하기 위해 (2번)내용을 수행해야 한다.


## 출석체크 방법 선택 

**AttendanceCheckActivity.java**

알람이 울릴 때, 어떤 방식으로 출석체크를 할지 선택하면서, 선택한 방식으로 출석체크 후 출석과 결석을 판단해주는 Activity이다.  총 네 가지의 button이 존재한다.

- button을 클릭하면 해당 Activity로 이동하여 출석체크 하는 동안 알람이 일시정지된다. 

아래는 각 button들의 onClickListener이다.
~~~java
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
        mediaPause();
        dialogSkip();
    }
});

btnOpencv = (Button)findViewById(R.id.btnOpencv);
btnOpencv.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        mediaPause();
        startActivityForResult(new Intent(getApplicationContext(), ImageMatchingActivity.class), IMAGE_MATCHING_ACTIVITY);
    }
});
}
~~~

랜덤으로 하나의 영어단어를 함께 넘겨주기위해 String.xml 에 영어단어 10가지를 추가하여 배열에 넣어주었다.
~~~java
randomText = getResources().getStringArray(R.array.random_text);  
rnd = new Random();
~~~

>String.xml
>~~~java
><string-array name="random_text">  
> <item>passion</item>  
> <item>wish</item>  
> <item>aspiration</item>  
> <item>peace</item>  
> <item>blossom</item>  
> <item>sunshine</item>  
> <item>cherish</item>  
> <item>smile</item>  
> <item>family</item>  
> <item>rainbow</item>  
></string-array>
>~~~

Text 인식을 이용한 출석체크 버튼을 눌렀을 경우 실행되는 textRecognition() method 이다.
랜덤으로 randomText배열에 들어가 있는 영어단어 하나를 함께 TextRecognitionActivity로 보내준다. 
~~~java
public void textRecognition(){  
	Intent intent = new Intent(this, TextRecognitionActivity.class );  
	int num = rnd.nextInt(9);  
	intent.putExtra("English", randomText[num]);  
  
	startActivityForResult(intent, TEXT_ACTIVITY);  
}
~~~

Skip button을 제외한 각 버튼들을 클릭하면, startActivityForResult를 통해 Activity마다 다른 requestCode와 함께 해당 Activity로 넘겨준다. 아래는 각 Activity의 requestCode를 정의해준 것이다.
~~~java
// 출석체크시 Activity 구분을 위한 requestCode  
final int LABEL_ACTIVITY = 1;  //사물 인식 Activity
final int TEXT_ACTIVITY = 2;  //Text 인식 Activity
final int IMAGE_MATCHING_ACTIVITY = 3; // Image Matching Activity
~~~


startActivityForResult를 사용하여 다른 Activity를 실행해줬을 경우, onActivityResult를 통해 Activity의 결과를 가져와 출석체크 출결여부를 결정한다.
Activity의 구분은 위에서 함께 넘겨준 requestCode로 구분할 수 있다.

- 출석체크 완료 시, 출석 count를 증가시키고 알람과 Activity를 꺼준다.
- 출석체크 실패 시, 출석체크를 다시 수행할 수 있게 일시정지 되었던 알람이 다시 울린다. 
~~~java
@Override  
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {  
	super.onActivityResult(requestCode, resultCode, data);  
  
	switch (requestCode) 
	{
		case LABEL_ACTIVITY: //ImageLabelActivity에서 받아온 data로 출결여부 결정
			String label = data.getStringExtra("labeling");  
		
			if(label.equals("Desk") || label.equals("Table") ){  
	            Toast.makeText(this, "Label 출석체크 완료 : "+label, Toast.LENGTH_SHORT).show();  
				alarmOff();  
				checkDaysTotal(weeks);    
				finish();  
			}  
	        else if(label.equals("BackPressed")){  
	            Toast.makeText(this, "Label 출석체크 취소", Toast.LENGTH_SHORT).show();  
				mediaRestart();  
			}  
	        else {  
	            Toast.makeText(this, "출석체크 실패 : "+label, Toast.LENGTH_SHORT).show();  
				count++;  
				mediaRestart();  
				
				//사물인식 출석체크 3번 실패 시 text 인식 출석체크 Activity 실행 
				if(count>=3){  
				count = 0;   
				textRecognition();  
				}  
	       }  break;  
		case TEXT_ACTIVITY :  //TextRecognitionActivity에서 받아온 data로 출결여부 결정
	        boolean checkValue = data.getBooleanExtra("checkValue", false);  
			if(checkValue == true){  
	            Toast.makeText(this, "Text 출석체크 완료", Toast.LENGTH_SHORT).show();  
				alarmOff();  
				checkDaysTotal(weeks);  
				finish();  
			}
			else {  
	            Toast.makeText(this, "Text 출석체크 취소", Toast.LENGTH_SHORT).show();  
				mediaRestart();  
			} break;  
		case IMAGE_MATCHING_ACTIVITY :  //ImageMatchingActivity에서 받아온 data로 출결여부 결정
	        boolean checkMatching = data.getBooleanExtra("checkMatching", false);  
			
			if(checkMatching == true){  
			    Toast.makeText(this, "ImageMatching 출석체크 완료", Toast.LENGTH_SHORT).show();  
			    alarmOff();  
			    checkDaysTotal(weeks);  
			    finish();  
			}
			else {  
	            Toast.makeText(this, "ImageMatching 출석체크 취소", Toast.LENGTH_SHORT).show();  
				mediaRestart();  
			}  
	}  
}
~~~
Skip button을 클릭시 수행되는 dialogSkip() method이다.
AlertDialog를 띄워 출석 여부를 고를 수 있다.
~~~java
public void dialogSkip(){  
    activity = this;  
    AlertDialog.Builder alertdialog = new AlertDialog.Builder(activity);  
    alertdialog.setMessage("출석여부를 고르세요.");  
  
    // 결석버튼  
    alertdialog.setPositiveButton("결석", new DialogInterface.OnClickListener(){  
        @Override  
	    public void onClick(DialogInterface dialog, int which) {  
            Toast.makeText(activity, "결석처리 되었습니다.", Toast.LENGTH_SHORT).show();  
		    alarmOff();  
		    finish();  
	    }  
    });  
    // 출석버튼  
    alertdialog.setNegativeButton("출석", new DialogInterface.OnClickListener() {  
        @Override  
	    public void onClick(DialogInterface dialog, int which) {  
            Toast.makeText(activity, "출석처리 되었습니다.", Toast.LENGTH_SHORT).show();  
		    alarmOff();  
		    checkDaysTotal(weeks);  
		    finish();  
		}  
    });
    // 취소버튼  
    alertdialog.setNeutralButton("취소", new DialogInterface.OnClickListener(){  
        @Override  
	    public void onClick(DialogInterface dialog, int id)  
        {  
            Toast.makeText(activity, "'취소'버튼을 누르셨습니다.", Toast.LENGTH_SHORT).show();  
		    mediaRestart();  
		}  
    });  
    
    AlertDialog alert = alertdialog.create();  
    alert.setTitle("Skip");  
    alert.show();  
}
~~~

## Firebase ML Kit를 이용한 출석체크 

Firebase ML Kit를 이용한 출석체크 기능으로는 사물인식 (Image Labeling), 문자 인식 (Text Recognition) 이 있다. 
- Firebase ML Kit를 사용하기 위해서는 아래와 같이 build.gradle에 정의해주어야 한다.

**build.gradle (:app)**
~~~java
implementation 'com.google.firebase:firebase-core:17.4.2'  
implementation 'com.google.firebase:firebase-ml-vision:24.0.0'  
~~~



**InternetCheck.java**
인터넷 여부를 체크하기 위해 인터넷을 체크하는 Activity를 추가한다.
~~~java
public class InternetCheck extends AsyncTask<Void,Void,Boolean> {  
  
	Consumer consumer;  
  
	public InternetCheck(Consumer consumer){  
	    this.consumer = consumer;  
		execute();  
	}  
  
	@Override  
	protected Boolean doInBackground(Void... voids) {  
	    try{  
            Socket socket = new Socket();  
			socket.connect(new InetSocketAddress("google.com",80),1500);  
			socket.close();  
			return true;  
		}
		catch (Exception e){  
            return false;  
		}  
	} 
  
	@Override  
	protected void onPostExecute(Boolean aBoolean) {  
        super.onPostExecute(aBoolean);  
		consumer.accept(aBoolean);  
	}  
  
    public interface Consumer { void accept(boolean internet); }  
}
~~~

### 사물 인식 (Image Labeling)
- 카메라로 책상을 촬영하여, 책상이 인식되면 출석체크가 완료된다.
- 사물인식 (Image Labeling) 기능을 사용할 때, 보다 안정적이고 빠르게 촬영 후 Detect하기 위해 CameraKit를 사용하였다.
- 촬영한 사진의 Label 값을 AttendanceCheckActivity로 전달한다.

>사물 인식을 통한 출석체크가 수행되는 과정
>1. **AttendanceCheckActivity**에서 사물 인식 기능 button을 클릭하여 **ImageLabelActivity**로 이동한다.
>2. **ImageLabelActivity**에서 cameraView를 통해 책상을 촬영한다.
>3. 촬영된 image에서 설정한 ConfidenceThreshold 값 이상인 Label중 가장 높은 Confidence를 가진 Label을 반환한다.
>4. **ImageLabelActivity**가 종료되면서 반환된 Label을 **AttendanceCheckActivity**로 넘겨준다.
>5. **AttendanceCheckActivity**에서 Label 값이 책상이 맞는지 확인한다. 
 
 Firebase ML Kit의 Image Labeling를 사용하기 위해 아래와 같이 build.gradle에 정의해주어야 한다.

**build.gradle (:app)**
~~~java
implementation 'com.google.firebase:firebase-ml-vision-image-label-model:19.0.0' 
~~~
>[Firebase MK Kit Image Labeling 설명 바로가기](https://firebase.google.com/docs/ml-kit/android/label-images)

CameraKit를 사용하기 위해 아래와 같이 build.gradle에  정의해주어야 한다.

**build.gradle (:app)**
~~~java
implementation 'com.wonderkiln:camerakit:0.13.1'
~~~
>[CameraKit github 바로가기](https://github.com/CameraKit/camerakit-android)


**ImageLabelActivity.java**
- ImageLabelActivity는 cameraKit의 cameraView와 Detect하기 위한 Button을 사용한다.

>activity_image_label.xml
cameraKit의 cameraView를 사용하기 위하여 아래와 같이 layout에 추가한다.
>~~~java
><com.wonderkiln.camerakit.CameraView  
>  android:id="@+id/camera_view"  
>  android:layout_width="match_parent"  
>  android:layout_height="match_parent"  
>  android:layout_above="@+id/btn_detect">
></com.wonderkiln.camerakit.CameraView>
>~~~
아래 코드는 Detect button의 onClickListener이다. button을 클릭하면 cameraView가 실행되고 촬영된다.
~~~java
btnDetect.setOnClickListener(new View.OnClickListener(){  
    @Override  
	public void onClick(View v) {  
        cameraView.start();  
		cameraView.captureImage();  
	}  
});
~~~

CameraKitListener 부분이다. cameraView에 바로 camera를 띄워 촬영한다.
~~~java
cameraView.addCameraKitListener(new CameraKitEventListener() {  
    @Override  
	public void onEvent(CameraKitEvent cameraKitEvent) { }  
  
    @Override  
	public void onError(CameraKitError cameraKitError) { }  
  
    @Override  
	public void onImage(CameraKitImage cameraKitImage) {  
        ...  
		Bitmap bitmap = cameraKitImage.getBitmap();  
		bitmap = Bitmap.createScaledBitmap(bitmap,cameraView.getWidth(),cameraView.getHeight(), false);  
		cameraView.stop();  
  
		runDetector(bitmap);  
	}  
  
    @Override  
	public void onVideo(CameraKitVideo cameraKitVideo) { }  
});
~~~
위의 CameraKitListener에서 사용한 runDetector() method이다.
아까 만들어준 InternetCheck.java 를 통해 인터넷을 체크한 후 , image에서 사물 인식 confidenceThreshold를 설정해준다.
설정한 confidenceThreshold의 값보다 높은 값을 가지는 Label이 반환한다.
~~~java
private void runDetector(Bitmap bitmap) {
    final FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

    new InternetCheck(new InternetCheck.Consumer() {
        @Override
        public void accept(boolean internet) {
            if(internet)
            {
                //인터넷이 있을 때 
                FirebaseVisionCloudImageLabelerOptions options =
                         new FirebaseVisionCloudImageLabelerOptions.Builder()
                                 .setConfidenceThreshold(0.7f) // 감지된 Label의 신뢰도 설정. 이 값보다 높은 신뢰도의 label만 반환됨
                                 .build();
                 FirebaseVisionImageLabeler detector =
                         FirebaseVision.getInstance().getCloudImageLabeler(options);

                 detector.processImage(image)
                         .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                             @Override
                             public void onSuccess(List<FirebaseVisionImageLabel> firebaseVisionCloudLabels) {
                                 processDataResultCloud(firebaseVisionCloudLabels);
                             }
                         })
                         .addOnFailureListener(new OnFailureListener() {
                             @Override
                             public void onFailure(@NonNull Exception e) {
		                     Log.d("EDMTERROR", e.getMessage());
			                 }
			             });
             }
             else
             {
                 Toast.makeText(ImageLabelActivity.this, "인터넷을 체크하고 다시 촬영해주세요.", Toast.LENGTH_LONG).show();
                 ...
             }
         }
     });
 }
~~~
위의 runDetector() 에서 사용한 processDataResultCloud() method 이다.
runDetector()에서 인식한 Label을 넘겨받아 Label 값이 존재할 경우 AttendanceCheckActivity로 Label 값을 넘겨주면서 ImageLabelActivity를 종료한다.
~~~java
private void processDataResultCloud(List<FirebaseVisionImageLabel> firebaseVisionCloudLabels) {  
	if(firebaseVisionCloudLabels.size()!=0){  
        for(FirebaseVisionImageLabel label : firebaseVisionCloudLabels)  
        {  
            String labeling = label.getText();  
			  
			Intent intent = new Intent();  
			intent.putExtra("labeling", labeling);  
			  
			setResult(RESULT_OK, intent);  
			finish();  
		}  
	}  
	else{  
		Intent intent = new Intent();  
		intent.putExtra("labeling", "NULL");  
	  
		setResult(RESULT_OK, intent);  
		finish();  
	}  
    ... 
}
~~~


### Text 인식 (Text Recognition)

- 제시된 영어단어를 노트에 따라 적고 촬영하여 두 개의 단어가 일치하면 출석체크가 완료된다.
- 출석체크 완료 시, true 값을 AttendanceCheckActivity에 전달한다.
- 출석체크 실패 시, 재촬영을 요구하는 text를 띄워준다.

>Text 인식을 통한 출석체크가 수행되는 과정
>1. **AttendanceCheckActivity**에서 하나의 영어단어를 랜덤으로 **TextRecognitionActivity**로 이동하면서 넘겨준다.
>2. **TextRecognitionActivity**에서 제시된 영어단어를 노트에 따라 적고 촬영한다.
>3. 촬영된 image에서 단어를 인식하고, 인식된 단어와 제시된 영어단어를 비교하여 같을 시에 true값을 반환한다.
>4. **TextRecognitionActivity**가 종료되면서 반환된 true 값을 **AttendanceCheckActivity**에 전달한다. 
	( 제시된 영어단어와 같지 않을 시에는 Activity가 종료되지 않으며, 재촬영을 요구하는 text를 띄워준다.)
>5. **AttendanceCheckActivity**에서 ture 값을 받았을 경우 출석체크가 완료된다.

AttendanceCheckActivity에서 전달받은 영어 단어를 textView에 표시해준다.
~~~java
Intent intent = getIntent();  
data = intent.getStringExtra("English");  
textView.setText("똑같이 작성해주세요 : "+ data + "\n");
~~~
사진 촬영 button의 onClickListener이다. 
~~~java
captureImageBtn.setOnClickListener(new View.OnClickListener() {  
    @Override  
    public void onClick(View v) {  
        dispatchTakePictureIntent();  
    }  
});
~~~
사진 촬영 button을 눌렀을 경우 실행되는 method이다. 카메라를 실행시켜준다.
~~~java
private void dispatchTakePictureIntent() {  
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);  
	if (takePictureIntent.resolveActivity(getPackageManager()) != null) {  
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);  
    }  
}
~~~
카메라로 촬영한 후에 실행되는 method이다.  
image를 Bitmap으로 저장하고 imageView에 촬영된 사진을 보여준 후, detectTextFromImage() method를 실행한다.
~~~java
@Override  
protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
    super.onActivityResult(requestCode, resultCode, data);  
	if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {  
        Bundle extras = data.getExtras();  
		imageBitmap = (Bitmap) extras.get("data");  
		imageView.setImageBitmap(imageBitmap);  
		detectTextFromImage();  
	}  
}
~~~
위의 카메라로 촬영한 후에 실행되는 method에서의 detectTextFromImage() method이다.
 촬영된 image에서 Text를 인식하고 성공했을 시에 displayTextFromImage() method를 실행한다.
~~~java
private void detectTextFromImage()  
{  
    new InternetCheck(new InternetCheck.Consumer() {  
        @Override  
		public void accept(boolean internet) {  
            if(internet){  
                FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(imageBitmap);  
				FirebaseVisionTextRecognizer firebaseVisionTextDetector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();  
				firebaseVisionTextDetector.processImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {  
                    @Override  
					public void onSuccess(FirebaseVisionText firebaseVisionText) {  
                        displayTextFromImage(firebaseVisionText);  
					}  
                }).addOnFailureListener(new OnFailureListener() {  
                    @Override  
					public void onFailure(@NonNull Exception e) {  
                        Toast.makeText(TextRecognitionActivity.this, "Error: "+ e.getMessage(), Toast.LENGTH_SHORT).show();  
						Log.d("Error: ", e.getMessage());  
					}  
                });  
			}else {  
                Toast.makeText(TextRecognitionActivity.this, "인터넷을 체크하고 다시 촬영해주세요.", Toast.LENGTH_LONG).show();  
			}  
        }  
    });  
 }
~~~

~~~java
private void displayTextFromImage(FirebaseVisionText firebaseVisionText) {  
    List<FirebaseVisionText.TextBlock> blockList = firebaseVisionText.getTextBlocks();  
	if(blockList.size() == 0){  
        textView2.setText("사진에서 단어가 인식되지 않았습니다. 다시 촬영해주세요.");  
	}  
    else {  
        String text = "";  
		for(FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks())  
        {  
            text = block.getText().toLowerCase();    
            check(text, data);  
        }   
    }  
}
~~~