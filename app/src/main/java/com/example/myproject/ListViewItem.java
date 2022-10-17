package com.example.myproject;

//리스트뷰 속의 아이템(2개의 텍스트{이름, 위도경도})
public class ListViewItem {
    private String nameStr ;
    private String lat_lonStr ;

    public void setName(String name) {
         nameStr = name;
    }
    public void setLat_lon(String lat_lon) {
        lat_lonStr =  lat_lon;
    }

    public String getName() {
        return this.nameStr ;
    }
    public String getLat_lon() {
        return this.lat_lonStr ;
    }
}
