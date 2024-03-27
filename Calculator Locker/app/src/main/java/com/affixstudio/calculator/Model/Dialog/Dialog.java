package com.affixstudio.calculator.Model.Dialog;

import static androidx.core.app.ActivityCompat.finishAffinity;

import android.app.Activity;
import android.view.View;

import com.affixstudio.calculator.locker.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class Dialog {
    Activity a;

    public Dialog(Activity a) {
        this.a = a;
    }

   public BottomSheetDialog closeDialog;
    public void prepareCloseDialog()
    {
        closeDialog=new BottomSheetDialog(a);

        View v=a.getLayoutInflater().inflate(R.layout.close_dialog,null);
        v.findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeDialog.dismiss();

                finishAffinity(a);
            }
        });

        closeDialog.setContentView(v);







    }





}
