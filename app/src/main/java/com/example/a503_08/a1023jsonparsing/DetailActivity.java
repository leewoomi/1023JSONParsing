package com.example.a503_08.a1023jsonparsing;


import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class DetailActivity extends AppCompatActivity {

    Button back;
    TextView title, description, price;
    ImageView img;


    //다운로드 받는 동안 화면에 출력될 대화상자
    ProgressDialog progressDialog;

    //상위 Activity로 부터 넘겨받을 id
    String itemid;

    //itemid를 이용해서 서버에서 받아온 데이터를 저장할 변수
    Map<String, String> map;

    //화면 갱신을 위한 핸들러
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {

                //대화상자 제거
                progressDialog.dismiss();

                //텍스트 뷰에 데이터 출력
                title.setText(map.get("itemname"));
                description.setText(map.get("description"));
                price.setText(map.get("price"));

                //파일 이름을 가지고 이미지를 다운로드 받는 스레드 생성 및 시작
                ImgThread th = new ImgThread(map.get("pictureurl"));

                th.start();
            } else if (msg.what == 1) {

                //Message의 obj를 이미지 뷰에 출력
                Bitmap bitmap = (Bitmap) msg.obj;
                if (bitmap == null) {
                    Toast.makeText(DetailActivity.this, "이미지 다운로드 실패", Toast.LENGTH_SHORT).show();
                } else {
                    img.setImageBitmap(bitmap);
                }
            }
        }
    };

    //상세보기 위한 스레드
    class ThreadEx extends Thread {
        @Override
        public void run() {
            StringBuilder sb = new StringBuilder();
            //다운로드 받는 코드
            try {

                //다운로드 받을 주소 생성
                URL url = new URL("http://192.168.0.118:8080/android/getitem?itemid=" + itemid);

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
                JSONObject item = new JSONObject(sb.toString());
                map = new HashMap<>();
                map.put("itmename", item.getString("itemname"));
                map.put("description", item.getString("description"));

                map.put("price", item.getString("price"));

                map.put("pictureurl", item.getString("pictureurl"));


                Log.e("pictureurl", item.getString("pictureurl"));
                //핸들러 호출 - 다시 출력
                handler.sendEmptyMessage(0);
            } catch (Exception e) {
                Log.e("파싱 에러", e.getMessage());
            }
        }
    }

    //파일이름을 가지고 이미지를 다운로드 받는 스레드
    class ImgThread extends Thread {
        String filename;

        public ImgThread(String filename) {
            this.filename = filename;
        }

        @Override
        public void run() {
            try {
                InputStream is = new URL("http://192.168.0.118:8080/android/img/" + filename).openStream();

                Bitmap bitmap = BitmapFactory.decodeStream(is);
                is.close();
                Message msg = new Message();
                msg.obj = bitmap;
                msg.what = 1;
                handler.sendMessage(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);


        title = (TextView) findViewById(R.id.title);
        description = (TextView) findViewById(R.id.description);
        price = (TextView) findViewById(R.id.price);

        img = (ImageView) findViewById(R.id.img);

        back = (Button) findViewById(R.id.back);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //현재 액티비티 제거
                finish();
            }
        });

        //이전 화면에서 데이터 받아오기
        Intent intent = getIntent();
        itemid = intent.getStringExtra("itemid");

        progressDialog = ProgressDialog.show(this, "", "다운로드 중");
        new ThreadEx().start();


    }
}
