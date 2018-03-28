package com.v3ctorsoftware.iapgenerator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private String INSTANCE_ID;
    private final String PREMIUM_SKU = "premium_upgrade";
    ConsumeResponseListener listener = new ConsumeResponseListener() {
        @Override
        public void onConsumeResponse(@BillingResponse int responseCode, String outToken) {
            if (responseCode == BillingResponse.OK) {
                Toast.makeText(getApplicationContext(), "Consumed purchase", Toast.LENGTH_SHORT).show();
            }
        }
    };
    private BillingClient mBillingClient;

    public void purchaseIAP(View view) {
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSku(PREMIUM_SKU)
                .setType(BillingClient.SkuType.INAPP)
                .build();
        int responseCode = mBillingClient.launchBillingFlow(this, flowParams);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    @Override
    protected void onStart() {
        super.onStart();
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String idFromPrefs = preferences.getString("device_id", "null");
        if (!idFromPrefs.equals("null")) {
            INSTANCE_ID = idFromPrefs;
        } else {
            INSTANCE_ID = FirebaseInstanceId.getInstance().getToken();
        }
        PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
                if (responseCode == BillingResponse.OK
                        && purchases != null) {
                    for (Purchase purchase : purchases) {
                        mBillingClient.consumeAsync(purchase.getPurchaseToken(), listener);
                    }
                } else if (responseCode == BillingResponse.USER_CANCELED) {
                    // Handle an error caused by a user cancelling the purchase flow.
                } else {
                    // Handle any other error codes.
                }
            }
        };
        mBillingClient = BillingClient.newBuilder(this).setListener(purchasesUpdatedListener).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(
                    @BillingClient.BillingResponse int billingResponseCode) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    // The billing client is ready. You can query purchases here.
                    mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP,
                            new PurchaseHistoryResponseListener() {
                                @Override
                                public void onPurchaseHistoryResponse(@BillingResponse int responseCode,
                                                                      List<Purchase> purchasesList) {
                                    if (responseCode == BillingResponse.OK
                                            && purchasesList != null) {
                                        for (Purchase purchase : purchasesList) {
                                            mBillingClient.consumeAsync(purchase.getPurchaseToken(), listener);

                                        }
                                    }
                                }
                            });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }

    public void displayDeviceID(View view) {
        Toast.makeText(this, INSTANCE_ID, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void registerDevice(View view) {
        FloatingActionButton iapButton = findViewById(R.id.floatingIAPButton);
        Button sendInfoButton = findViewById(R.id.SendInfoButton);
        String idFromPrefs = preferences.getString("device_id", "null");
        String snackbartext;
        if (!idFromPrefs.equals("null")) {
            snackbartext = "Connected or Connected before";
            iapButton.setClickable(true);
            sendInfoButton.setClickable(true);
        }else{
            snackbartext = "Failed to get Instance ID. Internet OK?";
            iapButton.setClickable(false);
            sendInfoButton.setClickable(false);
        }
        Toast.makeText(this, snackbartext, Toast.LENGTH_LONG).show();
    }

    public void sendInformation(View view) {
        String url = "https://iapgenerator.appspot.com/main.php";
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Response", response);
                        final Snackbar snackBar = Snackbar.make(getCurrentFocus(), "Success! You'll receive an email confirmation soon. You will be notified on this device when IAPG is ready", Snackbar.LENGTH_INDEFINITE);
                        snackBar.setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                snackBar.dismiss();
                            }
                        });
                        snackBar.show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", error.getMessage());
                        final Snackbar snackBar = Snackbar.make(getCurrentFocus(), "Oops, something went wrong", Snackbar.LENGTH_INDEFINITE);
                        snackBar.setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                snackBar.dismiss();
                            }
                        });
                        snackBar.show();
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                EditText firstname = findViewById(R.id.firstname);
                EditText surname = findViewById(R.id.surname);
                EditText email = findViewById(R.id.email);
                EditText amount = findViewById(R.id.amount);
                Map<String, String> params = new HashMap<>();
                params.put("firstname", firstname.getText().toString());
                params.put("surname", surname.getText().toString());
                params.put("emailaddress", email.getText().toString());
                params.put("fid", preferences.getString("device_id", "null"));
                params.put("amount", amount.getText().toString());
                return params;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };
        queue.add(postRequest);


    }
}