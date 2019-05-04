package com.example.TollPayActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.toll_mapbox_test.R;
import com.example.toll_mapbox_test.TollApplication;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Optional;
public class MainActivity extends AppCompatActivity {

    /**
     * A client for interacting with the Google Pay API
     *
     * @see <a
     *     href="https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentsClient">PaymentsClient</a>
     */
    private PaymentsClient mPaymentsClient;

    /**
     * A Google Pay payment button presented to the viewer for interaction
     *
     * @see <a href="https://developers.google.com/pay/api/android/guides/brand-guidelines">Google Pay
     *     payment button brand guidelines</a>
     */
    private View mGooglePayButton;

    /** A constant integer you define to track a request for payment data activity */
    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 42;
    // toll app var - used for referencing the TollApplicationClass
    /**
     * Initialize the Google Pay API on creation of the activity
     *
     * @see Activity#onCreate(android.os.Bundle)
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pay_activity_main);
        // updating string values ___ what tolls were clicked
        TextView textView = findViewById(R.id.toll_list_tv1);
        TextView textView1 = findViewById(R.id.total);
        StringBuilder tempStr = ((TollApplication) getApplicationContext()).tempStr;
        String formStr = ((TollApplication) getApplicationContext()).formStr;
        String whatTolls = ((TollApplication) getApplicationContext()).whatTolls;
        Double total = ((TollApplication) getApplicationContext()).total;

        TollApplication tollContext = ((TollApplication) getApplicationContext());
        int n = 4;

        for (int i = 0; i < n; i++) {
            String[] tc_ar = tollContext.toll_cost;
            if ( (tollContext).tollArray[i] == 1 ) {
                tempStr.append(formStr)
                        .append(formStr.toString().format(String.valueOf(i + 1)))
                        .append("\t").append(tc_ar[i]).append("\n");
                        String temp_ = tc_ar[i].replace("$", "");
                        total += Double.valueOf(temp_);


            }
        }
        String tc_cost = String.format("$" + "%.2f", total);
        textView.setText(tempStr);
        textView1.setText(tc_cost);
        // END TOLL UPDATING STUFF //
        // initialize a Google Pay API client for an environment suitable for testing
        mPaymentsClient =
                Wallet.getPaymentsClient(
                        this,
                        new Wallet.WalletOptions.Builder()
                                // Check on the environments
                                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                                .build());

        possiblyShowGooglePayButton();
//        **
        backToMap(tollContext);
    }
    public void backToMap(TollApplication tollContext) {
        CheckBox t1 = findViewById(R.id.toll1);
        CheckBox t2 = findViewById(R.id.toll2);
        CheckBox t3 = findViewById(R.id.toll3);
        CheckBox t4 = findViewById(R.id.toll4);
        findViewById(R.id.back_to_map).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // deselect all tolls, reset all strings, reset booleans, reset total value
                // restarting the activity
                for(int i=0; i<tollContext.numTolls; i++){
                    tollContext.tollArray[i] = 0;
                }
                tollContext.isFinished = true;
                Intent mainIntent = new Intent (MainActivity.this, com.example.toll_mapbox_test.MainActivity.class);
                Intent intent = new Intent (MainActivity.this, MainActivity.class);
                tollContext.tempStr = new StringBuilder("");
                tollContext.formStr = "Toll ";
                tollContext.total = 0.0;
                tollContext.whatTolls = "";
                TextView textView = findViewById(R.id.toll_list_tv1);
                TextView total = findViewById(R.id.total);
                // need to reset these -- FORMAT -- //
                textView.setText("");
                total.setText("");
                // reset strings
                finish();
                startActivity(mainIntent);
            }
        });
    }
    /**
     * Determine the viewer's ability to pay with a payment method supported by your app and display a
     * Google Pay payment button
     *
     * @see <a
     *     href="https://developers.google.com/android/reference/com/google/android/gms/wallet/PaymentsClient#isReadyToPay(com.google.android.gms.wallet.IsReadyToPayRequest)">PaymentsClient#IsReadyToPay</a>
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void possiblyShowGooglePayButton() {
        final Optional<JSONObject> isReadyToPayJson = GooglePay.getIsReadyToPayRequest();
        if (!isReadyToPayJson.isPresent()) {
            return;
        }
        IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(isReadyToPayJson.get().toString());
        if (request == null) {
            return;
        }
        Task<Boolean> task = mPaymentsClient.isReadyToPay(request);
        task.addOnCompleteListener(
                new OnCompleteListener<Boolean>() {
                    @SuppressWarnings("ConstantConditions")
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        try {
                            boolean result = false;
                            if (task.getResult(ApiException.class) != null) {
                                result = task.getResult(ApiException.class);
                            }
                            if (result) {
                                // show Google as a payment option
                                mGooglePayButton = findViewById(R.id.googlepay);
                                mGooglePayButton.setOnClickListener(
                                        new View.OnClickListener() {
                                            @RequiresApi(api = Build.VERSION_CODES.N)
                                            @Override
                                            public void onClick(View view) {
                                                requestPayment(view);
                                            }
                                        });
                                mGooglePayButton.setVisibility(View.VISIBLE);
                            }
                        } catch (ApiException exception) {
                            // handle developer errors
                        }
                    }
                });
    }

    /**
     * Display the Google Pay payment sheet after interaction with the Google Pay payment button
     *
     * @param view optionally uniquely identify the interactive element prompting for payment
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void requestPayment(View view) {
        Optional<JSONObject> paymentDataRequestJson = GooglePay.getPaymentDataRequest();
        if (!paymentDataRequestJson.isPresent()) {
            return;
        }
        PaymentDataRequest request =
                PaymentDataRequest.fromJson(paymentDataRequestJson.get().toString());
        if (request != null) {
            AutoResolveHelper.resolveTask(
                    mPaymentsClient.loadPaymentData(request), this, LOAD_PAYMENT_DATA_REQUEST_CODE);
        }
    }

    /**
     * Handle a resolved activity from the Google Pay payment sheet
     *
     * @param requestCode the request code originally supplied to AutoResolveHelper in
     *     requestPayment()
     * @param resultCode the result code returned by the Google Pay API
     * @param data an Intent from the Google Pay API containing payment or error data
     * @see <a href="https://developer.android.com/training/basics/intents/result">Getting a result
     *     from an Activity</a>
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // value passed in AutoResolveHelper
            case LOAD_PAYMENT_DATA_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:

                            PaymentData paymentData = PaymentData.getFromIntent(data);
                            assert paymentData != null;
                        try {
                            String json = paymentData.toJson();

                             JSONObject paymentMethodData = new JSONObject(json)
                                .getJSONObject("paymentMethodData");

                            String paymentToken = paymentMethodData
                                                     .getJSONObject("tokenizationData")
                                                     .getString("tok_1EIgjPJHrspDSWGh2t09sRkg");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                    case AutoResolveHelper.RESULT_ERROR:
                        Status status = AutoResolveHelper.getStatusFromIntent(data);
                        // Log the status for debugging.
                        // Generally, there is no need to show an error to the user.
                        // The Google Pay payment sheet will present any account errors.
                        break;
                    default:
                        // Do nothing.
                }
                break;
            default:
                // Do nothing.
        }
    }


}