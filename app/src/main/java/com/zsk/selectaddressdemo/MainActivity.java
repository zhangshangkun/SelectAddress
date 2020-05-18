package com.zsk.selectaddressdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bigkoo.pickerview.OptionsPickerView;
import com.bigkoo.pickerview.listener.CustomListener;
import com.google.gson.Gson;

import org.json.JSONArray;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private OptionsPickerView pvOptions;
    private ArrayList<JsonBean> options1Items = new ArrayList<>();
    private ArrayList<ArrayList<String>> options2Items = new ArrayList<>();
    private ArrayList<ArrayList<ArrayList<String>>> options3Items = new ArrayList<>();
    private String provinces;
    private String citys;
    private String areas;
    private Thread thread;
    private static final int MSG_LOAD_DATA = 0x0001;
    private static final int MSG_LOAD_SUCCESS = 0x0002;
    private static final int MSG_LOAD_FAILED = 0x0003;
    private boolean isLoaded = false;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD_DATA:
                    if (thread == null) {//如果已创建就不再重新创建子线程了
//                        Toast.makeText(mContext, "Begin Parse Data", Toast.LENGTH_SHORT).show();
                        thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                // 写子线程中的操作,解析省市区数据
                                initJsonData();
                            }
                        });
                        thread.start();
                    }
                    break;

                case MSG_LOAD_SUCCESS:
//                    Toast.makeText(mContext, "Parse Succeed", Toast.LENGTH_SHORT).show();
                    isLoaded = true;
                    break;

                case MSG_LOAD_FAILED:
//                    Toast.makeText(mContext, "Parse Failed", Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    };
    private TextView tv_address;
    private Button btn_address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        btn_address = findViewById(R.id.btn_address);
        tv_address = findViewById(R.id.tv_address);
        btn_address.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoaded) {
                    ShowPickerView();
                } else {
                    Toast.makeText(MainActivity.this,"请稍后...",Toast.LENGTH_SHORT).show();
                }
            }
        });
        mHandler.sendEmptyMessage(MSG_LOAD_DATA);
    }
    private void ShowPickerView() {// 弹出选择器
        if (pvOptions == null) {
            pvOptions = new OptionsPickerView.Builder(this, new OptionsPickerView.OnOptionsSelectListener() {
                @Override
                public void onOptionsSelect(int options1, int options2, int options3, View v) {
                    String tx = options1Items.get(options1).getPickerViewText() +
                            options2Items.get(options1).get(options2) +
                            options3Items.get(options1).get(options2).get(options3);

                    provinces = options1Items.get(options1).getPickerViewText();
                    citys = options2Items.get(options1).get(options2);
                    areas = options3Items.get(options1).get(options2).get(options3);
                    tv_address.setText(tx);
                }
            })
                    .setDividerColor(Color.BLACK)
                    .setLayoutRes(R.layout.layout_custom_view, new CustomListener() {
                        @Override
                        public void customLayout(View v) {

                            TextView tvSubmit = (TextView) v.findViewById(R.id.tv_finish);
                            TextView tvCancel = (TextView) v.findViewById(R.id.tv_cancel);
                            tvSubmit.getPaint().setFakeBoldText(true);
                            tvCancel.getPaint().setFakeBoldText(true);
                            tvCancel.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                     pvOptions.dismiss();
                                }
                            });
                            tvSubmit.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    pvOptions.returnData();
                                    pvOptions.dismiss();
                                }
                            });
                        }
                    })
                    .setContentTextSize(18)
                    .setDividerColor(Color.parseColor("#dcdcdc"))
                    .setCyclic(false, false, false)
                    .build();
            pvOptions.setPicker(options1Items, options2Items, options3Items);//三级选择器
            pvOptions.show();
        } else {
            pvOptions.show();
        }
    }
    private void initJsonData() {//解析数据

        /**
         * 注意：assets 目录下的Json文件仅供参考，实际使用可自行替换文件
         * 关键逻辑在于循环体
         *
         * */
        String JsonData = new GetJsonDataUtil().getJson(this, "province.json");//获取assets目录下的json文件数据

        ArrayList<JsonBean> jsonBean = parseData(JsonData);//用Gson 转成实体

        /**
         * 添加省份数据
         *
         * 注意：如果是添加的JavaBean实体，则实体类需要实现 IPickerViewData 接口，
         * PickerView会通过getPickerViewText方法获取字符串显示出来。
         */
        options1Items = jsonBean;

        for (int i = 0; i < jsonBean.size(); i++) {//遍历省份
            ArrayList<String> CityList = new ArrayList<>();//该省的城市列表（第二级）
            ArrayList<ArrayList<String>> Province_AreaList = new ArrayList<>();//该省的所有地区列表（第三极）

            for (int c = 0; c < jsonBean.get(i).getCityList().size(); c++) {//遍历该省份的所有城市
                String CityName = jsonBean.get(i).getCityList().get(c).getName();
                CityList.add(CityName);//添加城市

                ArrayList<String> City_AreaList = new ArrayList<>();//该城市的所有地区列表

                //如果无地区数据，建议添加空字符串，防止数据为null 导致三个选项长度不匹配造成崩溃
                if (jsonBean.get(i).getCityList().get(c).getArea() == null
                        || jsonBean.get(i).getCityList().get(c).getArea().size() == 0) {
                    City_AreaList.add("");
                } else {

                    for (int d = 0; d < jsonBean.get(i).getCityList().get(c).getArea().size(); d++) {//该城市对应地区所有数据
                        String AreaName = jsonBean.get(i).getCityList().get(c).getArea().get(d);

                        City_AreaList.add(AreaName);//添加该城市所有地区数据
                    }
                }
                Province_AreaList.add(City_AreaList);//添加该省所有地区数据
            }

            /**
             * 添加城市数据
             */
            options2Items.add(CityList);

            /**
             * 添加地区数据
             */
            options3Items.add(Province_AreaList);
        }

        mHandler.sendEmptyMessage(MSG_LOAD_SUCCESS);

    }


    public ArrayList<JsonBean> parseData(String result) {//Gson 解析
        ArrayList<JsonBean> detail = new ArrayList<>();
        try {
            JSONArray data = new JSONArray(result);
            Gson gson = new Gson();
            for (int i = 0; i < data.length(); i++) {
                JsonBean entity = gson.fromJson(data.optJSONObject(i).toString(), JsonBean.class);
                detail.add(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mHandler.sendEmptyMessage(MSG_LOAD_FAILED);
        }
        return detail;
    }
}
