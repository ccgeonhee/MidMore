package com.example.myproject;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


//즐겨찾기 보여주는 파일.
public class Load_data_Activity extends Activity {
    String name="";
    String lat_lon="";
    ArrayList<String> list = new ArrayList<String>();
    ListView listview ;
    ListViewAdapter adapter;

    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
    List<Address> addresses;
    String tmp[] = new String[] {};
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.load_data);

        Intent intent = getIntent();

        // Adapter 생성
        adapter = new ListViewAdapter() ;

        list = intent.getStringArrayListExtra("data");
        listview = (ListView) findViewById(R.id.listview);
        listview.setAdapter(adapter);

        //intent로 데이터베이스의 정보를 받아들여옴.
        for(int i = 0; i< list.size();i++){
            if(i % 3 == 0){//name
                name = list.get(i);
                System.out.println("name은 " + name);
            }
            else if(i % 3 == 1){//lat
                lat_lon = list.get(i);
                System.out.println("lat은 " + lat_lon);
            }
            else if(i % 3 == 2){//lon
                lat_lon = lat_lon+" " + list.get(i);
                adapter.addItem(name, lat_lon);
                System.out.println("lat_lon은 " + lat_lon);
            }
        }

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {//클릭시 클립보드에 해당 즐겨찾기의 주소 저장.
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                ListViewItem item = (ListViewItem) parent.getItemAtPosition(position);
                lat_lon = item.getLat_lon();
                // get item
                tmp = lat_lon.split(" ");
                double latitude = Double.parseDouble(tmp[0]);
                double longitude = Double.parseDouble(tmp[1]);

                try {
                    geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                    addresses = geocoder.getFromLocation(
                            latitude,
                            longitude,
                            7);
                }catch (IOException e){
                    e.printStackTrace();
                }
                Address address = addresses.get(0);

                ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("label", address.getAddressLine(0).toString());
                clipboard.setPrimaryClip(clipData);
                Toast.makeText(getApplicationContext(), "클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show();
                // TODO : use item data.
            }
        }) ;

        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {//롱클릭시 해당 즐겨찾기 삭제.
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                PlaceDatabaseManager databaseManager = PlaceDatabaseManager.getInstance(getApplicationContext());
                ListViewItem item = (ListViewItem) parent.getItemAtPosition(position);
                String delete_name[] = {item.getName()};
                databaseManager.delete("NAME = ?", delete_name);

                adapter.notifyDataSetChanged();
                Toast.makeText(getApplicationContext(),item.getName() + " 이(가) 즐겨찾기에서 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                // 이벤트 처리 종료 , 여기만 리스너 적용시키고 싶으면 true , 아니면 false
                return true;
            }
        });



    }
}
