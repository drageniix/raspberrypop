package com.drageniix.raspberrypop.utilities.billing;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.view.View;
import android.widget.Toast;

import com.drageniix.raspberrypop.BuildConfig;
import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.utilities.DBHandler;
import com.drageniix.raspberrypop.utilities.Logger;
import com.drageniix.raspberrypop.fragments.SettingsFragment;
import com.drageniix.raspberrypop.utilities.Preferences;

import java.util.Collections;

public class Billing {
    private final static String SKU_PREMIUM = "001";

    private IabHelper billing;
    private Inventory inventory;
    private Preferences preferences;

    public Billing(BaseActivity activity, DBHandler handler){
        preferences = handler.getPreferences();
        billing = new IabHelper(activity, handler.getParser().getBillingKey());
        initiate();
    }

    private void initiate() {
        billing.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (result.isSuccess()) {
                    updateInventorySync();
                }
                updatePremium();
            }
        });
    }

    private void updateInventorySync(){
        try {
            inventory = billing.queryInventory(true, Collections.singletonList(SKU_PREMIUM), null);
        } catch (Exception e) {
            Logger.log(Logger.BILL, e);
        }
    }

    public void advertise(final BaseActivity activity, final SettingsFragment fragment){
        new AlertDialog.Builder(activity)
            .setTitle("Unlock Premium!")
            .setView(View.inflate(activity, R.layout.billing_advertisement, null))
            .setPositiveButton("Unlock", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    purchase(activity, fragment);
                }
            })
            .setNegativeButton(activity.getString(R.string.cancel), null)
            .create()
            .show();
    }


    private void purchase(final BaseActivity activity, final SettingsFragment fragment){
        try {
            billing.flagEndAsync();
            billing.launchPurchaseFlow(activity, SKU_PREMIUM, 444, new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info) {
                    if (result.isSuccess()) {
                        updateInventorySync();
                        updatePremium();
                        preferences.setPremium(true);
                        Toast.makeText(activity, activity.getString(R.string.premium_purchase_success), Toast.LENGTH_SHORT).show();
                        if (fragment != null && !fragment.isDetached()){fragment.updateValues();}
                    } else {
                        Toast.makeText(activity, activity.getString(R.string.premium_purchase_failure), Toast.LENGTH_SHORT).show();
                    }
                }
            }, "");
        } catch (Exception e) {
            Toast.makeText(activity, activity.getString(R.string.premium_unavailable_summary), Toast.LENGTH_SHORT).show();
            Logger.log(Logger.BILL, e);
        }
    }

    public static boolean isBeta(){
        return BuildConfig.DEBUG || BuildConfig.VERSION_NAME.contains("beta");
    }

    public boolean hasPremium(){
        return preferences.hasPremium();
    }

    public boolean canAddMedia(){
        return hasPremium() || DBHandler.getCollectionSize() <= 3;
    }

    public void updatePremium(){
        preferences.setPremium(
            isBeta() || preferences.hasPurchasedPremium() ||
            (inventory != null && inventory.hasPurchase(SKU_PREMIUM)) ||
            (inventory == null && preferences.hasPremium()));
    }

    public void getPremiumDetails(final SettingsFragment fragment){
        final BaseActivity activity = (BaseActivity) fragment.getActivity();
        final Preference premium = fragment.findPreference("PREMIUM");

        if (!hasPremium() && premium != null) {
            premium.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (inventory != null && !hasPremium()) {
                        purchase(activity, fragment);
                    }
                    return false;
                }
            });

            if (inventory != null && inventory.hasDetails(SKU_PREMIUM)) {
                final SkuDetails skuItem = inventory.getSkuDetails(SKU_PREMIUM);
                String title = skuItem.getTitle().substring(0, skuItem.getTitle().lastIndexOf("(") - 1);
                premium.setSummary(fragment.getString(R.string.premium_summary));
                premium.setIcon(activity.getIcon(R.drawable.locked, false));
                premium.setTitle(title + " (" + skuItem.getPrice() + ")");
            } else {
                premium.setIcon(activity.getIcon(R.drawable.locked, false));
                premium.setTitle(fragment.getString(R.string.premium_unavailable));
                premium.setSummary(fragment.getString(R.string.premium_unavailable_summary));
            }
        } else if (premium != null){
            fragment.getPreferenceScreen().removePreference(premium);
        }
    }
}