package com.offlineide;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.widget.Toast;

/** Receives Android PackageInstaller status and launches its required confirmation UI. */
public final class InstallResultReceiver extends BroadcastReceiver {
  public static final String ACTION = "com.offlineide.action.APK_INSTALL_STATUS";

  @Override public void onReceive(Context context, Intent intent) {
    if (!ACTION.equals(intent.getAction())) return;
    int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
    if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
      Intent confirmation;
      if (Build.VERSION.SDK_INT >= 33)
        confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
      else {
        //noinspection deprecation
        confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
      }
      if (confirmation != null) {
        confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(confirmation);
      }
      return;
    }
    String message = status == PackageInstaller.STATUS_SUCCESS
      ? "APK installed successfully."
      : "APK installation failed: " + intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
  }
}
