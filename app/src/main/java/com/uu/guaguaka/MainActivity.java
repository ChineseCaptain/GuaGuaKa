package com.uu.guaguaka;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    EditText editText;
    GuaGuaView guaGuaView;
    Button btnOk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.editor);
        btnOk = (Button) findViewById(R.id.btn_ok);
        guaGuaView = (GuaGuaView) findViewById(R.id.guagua);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    guaGuaView.setmPrizeContent(v.getText().toString());
                    guaGuaView.reset();
                    return true;
                }
                return false;
            }
        });
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guaGuaView.setmPrizeContent(editText.getText().toString());
                guaGuaView.reset();
            }
        });
    }
}
