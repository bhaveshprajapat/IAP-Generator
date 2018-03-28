package com.v3ctorsoftware.iapgenerator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponse;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.List;

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

    @Override
    protected void onStart() {
        super.onStart();
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String idFromPrefs = preferences.getString("device_id", "null");
        if (!idFromPrefs.equals("null")) {
            INSTANCE_ID = idFromPrefs;
        } else {
            INSTANCE_ID = FirebaseInstanceId.getInstance().getId();
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
}
