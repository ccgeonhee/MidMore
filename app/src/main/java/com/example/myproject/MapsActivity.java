package com.example.myproject;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private Gps gpsTracker; //gps센서 이용.
    private GoogleMap mMap;
    private Geocoder geocoder; //위도,경도와 주소 변환
    private String mPath;//기본경로
    private String share_address;//이미지 저장경로 텍스트
    private Button search_button; //검색버튼
    private Button select_guide; //장소 선택 도우미 버튼
    private Button remove_button; //맵의 모든 마커 지우기 버튼
    private Button get_middle_button; //중간 장소 구하기 버튼
    private Button additional_menu; //부가 메뉴 버튼
    private ImageButton share_button;//공유 버튼
    private EditText address; //검색 창
    private static int mark_count = 0; //지점의 개수 저장.
    private String[][] lat_long = new String[3][2]; //각 지점에 대한 위도, 경도 정보
    private double lat1 = 0;
    private double lon1 = 0;
    private double lat2 = 0;
    private double lon2 = 0;
    private String Place_name; //즐겨찾기 북마크 이름
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    PlaceDatabaseManager databaseManager; //데이터베이스

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkPermission(){
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_maps);
        address = (EditText) findViewById(R.id.address);
        search_button = (Button) findViewById(R.id.search_button);
        select_guide = (Button) findViewById(R.id.load_data);
        remove_button = (Button) findViewById(R.id.remove_button);
        get_middle_button = (Button) findViewById(R.id.get_middle);
        additional_menu = (Button) findViewById(R.id.additional_menu);
        share_button = (ImageButton) findViewById(R.id.Share);
        databaseManager = PlaceDatabaseManager.getInstance(this);
        mPath = Environment.getExternalStorageDirectory().toString();
        share_address = mPath+"/Pictures/ScreenShot.png";

        checkPermission();


        if (!checkLocationServicesStatus()) { //안드로이드 구버전
            showDialogForLocationServiceSetting();
        } else {
            checkRunTimePermission();
        }

        notice(); //시작 안내

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        //구글 지도 불러오기
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    public void notice(){ //시작 안내문 출력
        AlertDialog.Builder oDialog = new AlertDialog.Builder(this,
                android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);

        String strHtml =
                "이 어플리케이션은 입력한 '두 지점'의<b><font color='#ff0000'> 중간거리 </font></b>를 구해주는 APP입니다.<br/>주소창을 통해 검색하시거나, 장소 선택 도우미를 이용해 지도에 마킹해주세요.";
        Spanned oHtml;

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
            // noinspection deprecation
            oHtml = Html.fromHtml(strHtml);
        }
        else
        {
            oHtml = Html.fromHtml(strHtml, Html.FROM_HTML_MODE_LEGACY);
        }

        oDialog.setTitle("안내")
                .setMessage(oHtml)
                .setPositiveButton("ok", null)
                .setCancelable(false)
                .show();
    }
    /*
     * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {
        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {
            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
            boolean check_result = true;
            // 모든 퍼미션을 허용했는지 체크합니다.
            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            if (check_result) {
                //위치 값을 가져올 수 있음
                ;
            } else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {
                    Toast.makeText(MapsActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(MapsActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    void checkRunTimePermission() {
        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MapsActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MapsActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)
            // 3.  위치 값을 가져올 수 있음

        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.
            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this, REQUIRED_PERMISSIONS[0])) {
                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(MapsActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MapsActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MapsActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }
    }

//    public boolean onCreateOptionsMenu(Menu menu) { //팝업 메뉴
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.popupmenu, menu);
//        getMenuInflater().inflate(R.menu.load_option_menu, menu);
//
//        return true;
//    }

    //본인 현재 GPS 주소를 얻기 위한 모듈.
    public String getCurrentAddress(double latitude, double longitude) {
        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_SHORT).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_SHORT).show();
            return "잘못된 GPS 좌표";
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_SHORT).show();
            return "주소 미발견";
        }
        Address address = addresses.get(0);
        return address.getAddressLine(0).toString();
    }


    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:
                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }
                break;
        }
    }


    public void onStop() {
        super.onStop();
    }

    public void onResume() {
        super.onResume();
    }

    public double getDistance(LatLng LatLng1, LatLng LatLng2) { // 두 지점의 거리를 구하는 모듈.
        double distance = 0;

        Location locationA = new Location("A");
        locationA.setLatitude(LatLng1.latitude);
        locationA.setLongitude(LatLng1.longitude);

        Location locationB = new Location("B");
        locationB.setLatitude(LatLng2.latitude);
        locationB.setLongitude(LatLng2.longitude);

        distance = locationA.distanceTo(locationB);

        return distance;
    }

    public void get_Current_Gps() { //현재 위치를 구해 마킹하고, 알려주는 모듈.
        gpsTracker = new Gps(MapsActivity.this);
        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();

        if (mark_count == 0) {//중간지점 구하고나서 맵 클리어.
            mMap.clear();
        } else if (mark_count == 2) {
            Toast.makeText(getApplicationContext(), "이미 두 지점이 마킹되었습니다. 중간 지점을 구해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        lat_long[mark_count][0] = Double.toString(latitude);
        lat_long[mark_count][1] = Double.toString(longitude);
        String Current_address = getCurrentAddress(latitude, longitude);

        LatLng Mypoint = new LatLng(latitude, longitude);
        // 마커 생성
        MarkerOptions mOptions2 = new MarkerOptions();
        mOptions2.title("현재 GPS 주소");
        mOptions2.snippet(Current_address);
        mOptions2.position(Mypoint);
        // 마커 추가

        mMap.addMarker(mOptions2);
        mark_count += 1;

        // 해당 좌표로 화면 줌
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Mypoint, 17));
        Toast.makeText(getApplicationContext(), "현재 위치는 \n" + Current_address + "입니다.", Toast.LENGTH_SHORT).show();
    }

    public void getPlaceData() { //즐겨찾기를 불러오는 모듈. Load_data_Activity로 데이터베이스의 정보를 intent를 통해 전송시킴.
        String[] columns = new String[]{"_id", "NAME", "LATITUDE", "LONGITUDE"};
        ArrayList<String> list = new ArrayList<String>();
        Cursor cursor = databaseManager.query(columns, null, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                System.out.println("getPlaceData 성공");
                for (int j = 1; j < 4; j++) {
                    list.add(cursor.getString(j));
                }
            }
            System.out.println("인텐트전 ArrayList는 " + list.size());
            Intent intent = new Intent(getApplicationContext(), Load_data_Activity.class);
            intent.putExtra("data", list);
            startActivity(intent);
        }
    }


    public boolean onKeyDown(int keyCode, KeyEvent event){ //종료 안내 모듈.
        switch(keyCode){
            case KeyEvent.KEYCODE_BACK:
                String alertTitle = "약속장소 정하셨나요?";
                String buttonMessage = "어플을 종료하시겠습니까?";
                String buttonYes = "Yes";
                String buttonNo = "No";

                new AlertDialog.Builder(MapsActivity.this)
                        .setTitle(alertTitle)
                        .setMessage(buttonMessage)
                        .setPositiveButton(buttonYes, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                moveTaskToBack(true);
                                finish();
                            }
                        })
                        .setNegativeButton(buttonNo, null)
                        .show();
        }
        return true;

    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    @Override
    public void onMapReady(final GoogleMap googleMap) { //맵과 관련된 모듈. 클릭 이벤트 정의.
        mMap = googleMap;
        geocoder = new Geocoder(this);

        get_middle_button.setEnabled(false);
        get_middle_button.setTextColor(Color.parseColor("#808080"));
        // 버튼 이벤트
        search_button.setOnClickListener(new Button.OnClickListener() { //검색 버튼
            @Override
            public void onClick(View v) {
                String str = address.getText().toString();
                List<Address> addressList = null;

                if (mark_count == 2) {
                    Toast.makeText(getApplicationContext(), "이미 두 지점이 마킹되었습니다. 중간 지점을 구해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    // editText에 입력한 텍스트(주소, 지역, 장소 등)을 지오 코딩을 이용해 변환
                    addressList = geocoder.getFromLocationName(
                            str, // 주소
                            10); // 최대 검색 결과 개수
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "주소를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (addressList.size() == 0) {
                    Toast.makeText(getApplicationContext(), "검색결과가 없습니다.", Toast.LENGTH_SHORT).show();
                }
                else {
                    System.out.println(addressList.get(0).toString());
                    // 콤마를 기준으로 split
                    String[] splitStr;
                    int lat_num = 10;
                    int lon_num = 12;

                    System.out.println(addressList.get(0).toString());
                    if (addressList.get(0).toString().contains("(")) {
                        lat_num += 1;
                        lon_num += 1;
                    }
                    // 콤마를 기준으로 split
                    splitStr = addressList.get(0).toString().split(",");
                    System.out.println(splitStr[0].length());
                    String search_address = splitStr[0].substring(splitStr[0].indexOf("\"") + 1, splitStr[0].length() - 1); // 주소
                    System.out.println(search_address);


                    String latitude = splitStr[lat_num].substring(splitStr[lat_num].indexOf("=") + 1); // 위도
                    String longitude = splitStr[lon_num].substring(splitStr[lon_num].indexOf("=") + 1); // 경도
                    lat_long[mark_count][0] = latitude;
                    lat_long[mark_count][1] = longitude;
                    System.out.println(latitude);
                    System.out.println(longitude);

                    // 좌표(위도, 경도) 생성
                    LatLng point = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                    // 마커 생성
                    MarkerOptions mOptions2 = new MarkerOptions();
                    mOptions2.title("검색 결과");
                    mOptions2.snippet(search_address);
                    mOptions2.position(point);
                    // 마커 추가
                    if (mark_count == 0) {
                        mMap.clear();
                    }

                    mMap.addMarker(mOptions2);
                    mark_count += 1;
                    // 해당 좌표로 화면 줌
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 17));

                    if (mark_count == 2) {
                        System.out.println("버튼활성화");
                        get_middle_button.setEnabled(true);
                        get_middle_button.setTextColor(Color.parseColor("#000000"));
                    }
                }
            }
        });

        select_guide.setOnClickListener(new View.OnClickListener() { //장소 선택 도우미.
            @Override
            public void onClick(View v) {
                PopupMenu p = new PopupMenu(getApplicationContext(), v);
                getMenuInflater().inflate(R.menu.load_option_menu, p.getMenu());

                // 이벤트 처리
                p.setOnMenuItemClickListener(
                        new PopupMenu.OnMenuItemClickListener() {
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.current_gps: //현재 gps
                                        if (mark_count == 2) {
                                            Toast.makeText(getApplicationContext(), "이미 두 지점이 마킹되었습니다. 중간 지점을 구해주세요.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            get_Current_Gps();
                                        }
                                        break;
                                    case R.id.load_favorite_place: //즐겨찾기 불러오기
                                        getPlaceData();
                                        break;
                                    case R.id.get_address_site: //도로명주소 사이트
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Intent place_intent = new Intent(Intent.ACTION_VIEW);
                                                Uri place_uri = Uri.parse("https://www.juso.go.kr/openIndexPage.do");
                                                place_intent.setData(place_uri);
                                                startActivity(place_intent);
                                            }
                                        }).start();
                                        break;
                                }
                                return true;
                            }
                        });
                p.show(); // 메뉴를 띄우기
                if (mark_count == 2) {
                    System.out.println("버튼활성화");
                    get_middle_button.setEnabled(true);
                    get_middle_button.setTextColor(Color.parseColor("#000000"));
                }
            }
        });
        remove_button.setOnClickListener(new View.OnClickListener() { //맵의 모든 마커를 지움.
            @Override
            public void onClick(View v) {
                mMap.clear();
                mark_count = 0;

                for (int i = 0; i < 3; i++) {
                    lat_long[i][0] = "";
                    lat_long[i][1] = "";
                }
            }
        });
        get_middle_button.setOnClickListener(new View.OnClickListener() { //중간 거리를 구함.
            @Override
            public void onClick(View v) {
                if (mark_count == 0) {
                    Toast.makeText(getApplicationContext(), "마킹된 지점이 없습니다. 검색을 통해 마킹해주세요.", Toast.LENGTH_SHORT).show();
                } else {
                    lat1 = Double.parseDouble(lat_long[0][0]);
                    lon1 = Double.parseDouble(lat_long[0][1]);
                    lat2 = Double.parseDouble(lat_long[1][0]);
                    lon2 = Double.parseDouble(lat_long[1][1]);

                    LatLng latlng1 = new LatLng(lat1, lon1);
                    LatLng latlng2 = new LatLng(lat2, lon2);

                    Toast.makeText(getApplicationContext(), "입력하신 두 지점의 거리는 대략" + Math.round(getDistance(latlng1, latlng2) / 1000) + "km 입니다.", Toast.LENGTH_SHORT).show();
                    LatLng middle_point = new LatLng((lat1 + lat2) / 2, (lon1 + lon2) / 2);

                    Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                    List<Address> addresses;
                    try { //위도, 경도를 통해 주소를 구함.
                        addresses = geocoder.getFromLocation(
                                (lat1 + lat2) / 2,
                                (lon1 + lon2) / 2,
                                7);
                    } catch (IOException ioException) {
                        //네트워크 문제
                        return;
                    }
                    Address address = addresses.get(0);

                    System.out.println(middle_point);
                    MarkerOptions mOptions = new MarkerOptions();
                    mOptions.title("두 지점의 중간 지점\n");
                    mOptions.snippet(address.getAddressLine(0).toString());
                    mOptions.position(middle_point);
                    mOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                    mMap.addMarker(mOptions);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(middle_point, 15));

                    lat_long[mark_count][0] = Double.toString((lat1 + lat2) / 2);
                    lat_long[mark_count][1] = Double.toString((lon1 + lon2) / 2);

                    mark_count += 1;
                }
            }
        });
        additional_menu.setOnClickListener(new View.OnClickListener() { //부가 메뉴
            @Override
            public void onClick(View v) {
                PopupMenu p = new PopupMenu(getApplicationContext(), v);
                getMenuInflater().inflate(R.menu.popupmenu, p.getMenu());
                // 이벤트 처리
                p.setOnMenuItemClickListener(
                        new PopupMenu.OnMenuItemClickListener() {
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.place_data: //해당 지역 근처 식당 정보
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Intent place_intent = new Intent(Intent.ACTION_VIEW);
                                                Uri place_uri = Uri.parse("https://www.google.co.kr/maps/search/%EB%A0%88%EC%8A%A4%ED%86%A0%EB%9E%91/@" + lat_long[mark_count - 1][0] + "," + lat_long[mark_count - 1][1] + ",14z");
                                                place_intent.setData(place_uri);
                                                startActivity(place_intent);
                                            }
                                        }).start();

                                        break;
                                    case R.id.save_favorite_place: //현재 지점을 즐겨찾기에 등록.
                                        if (mark_count == 0) {
                                            Toast.makeText(getApplicationContext(), "먼저 검색을 통해 마킹을 해주세요.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Bundle extras = getIntent().getExtras();
                                            AlertDialog.Builder alert = new AlertDialog.Builder(MapsActivity.this);

                                            alert.setTitle("장소 이름을 정해주세요.");
                                            alert.setMessage("장소 이름");

                                            final EditText name = new EditText(MapsActivity.this);
                                            alert.setView(name);

                                            alert.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    Place_name = name.getText().toString();
                                                    dialog.dismiss();

                                                    if (Place_name != null) {
                                                        ContentValues addRowValue = new ContentValues();

                                                        addRowValue.put("NAME", Place_name);
                                                        addRowValue.put("LATITUDE", lat_long[mark_count - 1][0]);
                                                        addRowValue.put("LONGITUDE", lat_long[mark_count - 1][1]);

                                                        databaseManager.insert(addRowValue);
                                                        Place_name = null;
                                                    }
                                                }
                                            });
                                            alert.create();
                                            alert.show();
                                        }
                                        break;
                                }
                                return true;
                            }
                        });
                p.show(); // 메뉴를 띄우기
            }

        });
        share_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
                    Bitmap bitmap;
                    @Override
                    public void onSnapshotReady(Bitmap snapshot) {
                        bitmap = snapshot;
                        try {
                            FileOutputStream out = new FileOutputStream(share_address);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                            Toast.makeText(MapsActivity.this, "스크린샷이 저장되었습니다.", Toast.LENGTH_LONG).show();
                            try {
                                File file = new File(mPath+"/Pictures","ScreenShot.png");
                                Uri uri = FileProvider.getUriForFile(MapsActivity.this, getApplicationContext().getPackageName() +".fileprovider", file);
                                Intent shareintent = new Intent(Intent.ACTION_SEND);
                                shareintent.putExtra(Intent.EXTRA_STREAM, uri);
                                shareintent.setType("image/*");
                                startActivity(Intent.createChooser(shareintent, "공유"));
                            }
                            catch (Exception e){
                                e.printStackTrace();
                                Toast.makeText(MapsActivity.this, "공유실패", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(MapsActivity.this, "오류로 인해 스크린샷이 저장되지 않았습니다.", Toast.LENGTH_LONG).show();
                        }
                    }
                };
                mMap.snapshot(callback);
            }

        });
//         Add a marker in Sydney and move the camera
        LatLng seoul = new LatLng(37.56, 126.97);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 10));

    }

}


