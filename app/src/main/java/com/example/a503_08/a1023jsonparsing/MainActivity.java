package com.example.a503_08.a1023jsonparsing;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;

import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //ListView 출력관련 변수
    ArrayList<String> nameList;
    ArrayAdapter<String> adapter;
    ListView itemlist;

    //상세보기를 위해서 id를 저장할 List
    ArrayList<String> idList;


    //데이터를 다운로드 받는 동안 보여질 대화상자
    ProgressDialog progressDialog;

    TextView textView;


    SwipeRefreshLayout swipe_layout;

    //리스트 뷰의 데이터를 다시 출력하는 핸들러 생성
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            adapter.notifyDataSetChanged();
            progressDialog.dismiss();
            swipe_layout.setRefreshing(false);

        }
    };

    class ThreadEx extends Thread {
        @Override
        public void run() {
            StringBuilder sb = new StringBuilder();
            //다운로드 받는 코드
            try {

                //다운로드 받을 주소 생성
                URL url = new URL("http://192.168.0.118:8080/android/listitem");
                Log.e("URL", url.toString());
                //Connection 연결
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                con.setUseCaches(false);
                con.setConnectTimeout(30000);

                //문자열을 다운로드 받을 스트림 만들기
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

                //문자열을 다운로드 받아서 sb에 추가하기
                while (true) {
                    String line = br.readLine();
                    if (line == null) break;
                    sb.append(line + "\n");
                }
                br.close();
                con.disconnect();

            } catch (Exception e) {
                Log.e("다운로드 실패", e.getMessage());
            }


            //파싱하는 코드
            try {
                //전체 문자열을 배열로 변경
                JSONArray array = new JSONArray(sb.toString());

                //배열 순회
                nameList.clear();
                idList.clear();
                for (int i = 0; i < array.length(); i++) {

                    JSONObject object = array.getJSONObject(i);
                    //객체에서 itemname의 값을 가져와서 nameList에 추가
                    nameList.add(object.getString("itemname"));
                    idList.add(object.getString("itemid"));
                }

                //핸들러 호출 - 다시 출력
                handler.sendEmptyMessage(0);
            } catch (Exception e) {
                Log.e("파싱 에러", e.getMessage());
            }
        }
    }

    //데이터를 다운로드 받아서 파싱한 후 핸들러를 호출하는 스레드를 생성


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        swipe_layout = (SwipeRefreshLayout)findViewById(R.id.swipe_layout);

        swipe_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new ThreadEx().start();
            }
        });

        nameList = new ArrayList<>();
        idList = new ArrayList<>();


        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nameList);

        itemlist = (ListView) findViewById(R.id.itemlist);

        itemlist.setAdapter(adapter);

        //리스트 뷰에서 항목을 클릭했을 때 수행할 내용

        itemlist.setOnItemClickListener(
                new ListView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent,
                                            View view,
                                            int position,
                                            long id) {
                        Intent intent =
                                new Intent(MainActivity.this,
                                        DetailActivity.class);
                        intent.putExtra("itemid", idList.get(position));
                        startActivity(intent);
                    }
                });


    }


    //onResume 메소드를 재정의 해서 스레드를 시작
    //액티비티가 실행될 때 호출되는 메소드
    @Override
    public void onResume() {
        super.onResume();
        progressDialog = ProgressDialog.show(this, "", "다운로드 중");
        new ThreadEx().start();
    }
}
