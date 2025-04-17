package com.ramphal.tictactoe;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.platform.MaterialSharedAxis;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.review.ReviewException;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.model.ReviewErrorCode;


public class MainActivity extends AppCompatActivity {

    MaterialButton change_board, three_play_friend, three_play_bot, setting;
    TextView board_name, win_cond, win_cond_title, game_rules, share, mode_apps, about, privacy;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    BottomSheetDialog bottomSheetDialog, bottomSheetDialog2, updateBottomSheet;
    int flag = 0;
    MaterialSwitch themeSwitch;
    private static final int REQUEST_CODE_UPDATE = 369;
    private AppUpdateManager appUpdateManager;
    private long backPressedTime;
    private Toast backToast;

    // SharedPreferences key
    private static final String PREFS_NAME = "TicTacToePrefs";
    private static final String KEY_BOARD = "board";
    private static final String KEY_THEME = "theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        applyTheme();
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        MaterialSharedAxis sharedAxisExit = new MaterialSharedAxis(MaterialSharedAxis.X,true);
        MaterialSharedAxis sharedAxisEnter = new MaterialSharedAxis(MaterialSharedAxis.X,false);
        getWindow().setEnterTransition(sharedAxisEnter);
        getWindow().setExitTransition(sharedAxisExit);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        AdsServices.loadInterstitialAd(MainActivity.this);
        // Initialize
        appUpdateManager = AppUpdateManagerFactory.create(this);
        checkForAppUpdate();

        Window window = getWindow();
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.primaryContainer));

        // Initialize SharedPreferences and editor
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        if (sharedPreferences == null || editor == null) {
            throw new RuntimeException("SharedPreferences or Editor not initialized.");
        }

        change_board = findViewById(R.id.change_board);
        three_play_friend = findViewById(R.id.three_play_friend);
        three_play_bot = findViewById(R.id.three_play_bot);
        board_name = findViewById(R.id.board_name);
        win_cond = findViewById(R.id.win_cond);
        win_cond_title = findViewById(R.id.win_cond_title);
        setting = findViewById(R.id.setting);

        bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
        bottomSheetDialog.setContentView(R.layout.setting);
        bottomSheetDialog.setCancelable(true);
        bottomSheetDialog.setCanceledOnTouchOutside(true);
        bottomSheetDialog.getWindow().setNavigationBarColor(getResources().getColor(R.color.secondaryContainer)); // Replace 'your_color' with your desired color
        bottomSheetDialog2 = new BottomSheetDialog(MainActivity.this);
        bottomSheetDialog2.setContentView(R.layout.game_rules);
        bottomSheetDialog2.setCancelable(true);
        bottomSheetDialog2.setCanceledOnTouchOutside(true);
        bottomSheetDialog2.getWindow().setNavigationBarColor(getResources().getColor(R.color.secondaryContainer)); // Replace 'your_color' with your desired color

        // Initialize the MaterialSwitch for theme switching
        themeSwitch = bottomSheetDialog.findViewById(R.id.materialSwitch);
        game_rules = bottomSheetDialog.findViewById(R.id.game_rules);
        share = bottomSheetDialog.findViewById(R.id.share);
        mode_apps = bottomSheetDialog.findViewById(R.id.mode_apps);
        about = bottomSheetDialog.findViewById(R.id.about);
        privacy = bottomSheetDialog.findViewById(R.id.privacy);

        String themePref = sharedPreferences.getString(KEY_THEME, "system");
        themeSwitch.setChecked(themePref.equals("dark"));

        int currentThemeMode = AppCompatDelegate.getDefaultNightMode();
        if (currentThemeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            // Set the switch to checked for dark mode
            themeSwitch.setChecked(true);
        } else if (currentThemeMode == AppCompatDelegate.MODE_NIGHT_NO) {
            // Set the switch to unchecked for light mode
            themeSwitch.setChecked(false);
        } else {
            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                themeSwitch.setChecked(true);  // Set switch to dark mode
            } else {
                themeSwitch.setChecked(false); // Set switch to light mode
            }
        }

        // Theme switch listener
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Switch to dark theme
                editor.putString(KEY_THEME, "dark");
            } else {
                // Switch to light theme
                editor.putString(KEY_THEME, "light");
            }
            editor.apply();
            bottomSheetDialog.dismiss();

            // Restart activity to apply the theme change
            applyTheme();
        });

        // Listener for Game Rules TextView to show bottomSheetDialog2 (Game Rules dialog)
        game_rules.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
                bottomSheetDialog2.show();  // Show the Game Rules dialog
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
                shareAppLink();
            }
        });

        mode_apps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=Varun.S"));
                startActivity(browserIntent);
            }
        });

        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.blogger.com/profile/00873435353667994979"));
                startActivity(browserIntent);
            }
        });

        privacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://ram-phal.blogspot.com/p/privacy-policy-tic-tac-toe.html"));
                startActivity(browserIntent);
            }
        });

        // Retrieve the last saved board state
        int boardFlag = sharedPreferences.getInt(KEY_BOARD, 0);
        updateBoardDisplay(boardFlag);

        change_board.setOnClickListener(v -> {
            int flag = sharedPreferences.getInt(KEY_BOARD, 0);
            if (flag == 0) {
                updateBoardDisplay(1);  // Six X Six Board
                editor.putInt(KEY_BOARD, 1).apply();  // Save board state
            } else if (flag == 1) {
                updateBoardDisplay(2);  // Nine X Nine Board
                editor.putInt(KEY_BOARD, 2).apply();  // Save board state
            } else if (flag == 2) {
                updateBoardDisplay(0);  // Three X Three Board
                editor.putInt(KEY_BOARD, 0).apply();  // Save board state
            }

            titleAnimation(win_cond_title);
            applyAnimation(board_name);
            applyAnimation(win_cond);
            applyAnimation(change_board);
            applyAnimation(three_play_friend);
            applyAnimation(three_play_bot);
        });

        three_play_bot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int flag = sharedPreferences.getInt(KEY_BOARD, 0);
                Intent withBotThree = new Intent(MainActivity.this, WithBotThree.class);
                Intent withBotSix = new Intent(MainActivity.this, WithBotSix.class);
                Intent withBotNine = new Intent(MainActivity.this, WithBotNine.class);
                if (flag == 0) {
                    ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this);
                    startActivity(withBotThree, activityOptions.toBundle());
                } else if (flag == 1) {
                    ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this);
                    startActivity(withBotSix, activityOptions.toBundle());
                } else if (flag == 2) {
                    ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this);
                    startActivity(withBotNine, activityOptions.toBundle());
                }
            }
        });

        three_play_friend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int flag = sharedPreferences.getInt(KEY_BOARD, 0);
                Intent withFriendThree = new Intent(MainActivity.this, WithFriendThree.class);
                Intent withFriendSix = new Intent(MainActivity.this, WithFriendSix.class);
                Intent withFriendNine = new Intent(MainActivity.this, WithFriendNine.class);
                if (flag == 0) {
                    ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this);
                    startActivity(withFriendThree, activityOptions.toBundle());
                } else if (flag == 1) {
                    ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this);
                    startActivity(withFriendSix, activityOptions.toBundle());
                } else if (flag == 2) {
                    ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(MainActivity.this);
                    startActivity(withFriendNine, activityOptions.toBundle());
                }
            }
        });

        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.show();
            }
        });

        bottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                getWindow().setNavigationBarColor(getResources().getColor(R.color.primaryContainer)); // Replace 'default_color' with your original color
            }
        });

        bottomSheetDialog2.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                getWindow().setNavigationBarColor(getResources().getColor(R.color.primaryContainer)); // Replace 'default_color' with your original color
            }
        });

    }

    private void updateBoardDisplay(int flag) {
        if (flag == 0) {
            board_name.setText("Three X Three Board");
            win_cond.setText("Winner: The first player to create a line of 3 identical symbols wins the game.");
        } else if (flag == 1) {
            board_name.setText("Six X Six Board");
            win_cond.setText("Winner: The first player to create a line of 4 identical symbols wins the game.");
        } else if (flag == 2) {
            board_name.setText("Nine X Nine Board");
            win_cond.setText("Winner: The first player to create a line of 5 identical symbols wins the game.");
        }
    }

    private void applyAnimation(View view) {
        view.setAlpha(0f);
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .start();
    }

    private void titleAnimation(View view) {
        view.setAlpha(0f);
        view.setScaleX(0.6f);
        view.setScaleY(0.6f);
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .start();
    }

    private void applyTheme() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Check if a theme preference is saved
        String themePref = sharedPreferences.getString(KEY_THEME, "system");  // "system" is the default value

        // Apply theme based on user preference or default to system theme
        if (themePref.equals("dark")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (themePref.equals("light")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            // Use system default theme if no preference is set
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private void shareAppLink() {
        // Define the Play Store URL for your app
        String appPackageName = getPackageName(); // Your app's package name
        String appLink = "https://play.google.com/store/apps/details?id=" + appPackageName;

        // Create an intent to share the app link
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Tic Tac Toe: Classic & Multiplayer");
        String shareMessage = "Enjoy the classic Tic Tac Toe game anytime, anywhere! Challenge your friends or test your skills with our offline version. Download now and start playing! " + appLink;
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        // Launch the share intent
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            // If the second back press is within 2 seconds, exit the app
            if (backToast != null) {
                backToast.cancel();
            }
            finishAffinity();  // This will close the app and all of its tasks
            super.onBackPressed();
            return;
        } else {
            // Show a toast prompting the user to press back again to exit
            backToast = Toast.makeText(getBaseContext(), "Press back again to exit", Toast.LENGTH_SHORT);
            backToast.show();
        }

        // Update the backPressedTime to the current time
        backPressedTime = System.currentTimeMillis();
    }

    private void checkForAppUpdate() {
        // Fetch the update availability and version information
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        // Add a listener to handle update availability
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Check if an immediate update is required
                if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    showUpdateRequiredDialog();  // Show the bottom sheet to force the update
                }
            }
        }).addOnFailureListener(e -> {
        });
    }

    private void startAppUpdate(AppUpdateInfo appUpdateInfo, int appUpdateType) throws IntentSender.SendIntentException {
        // Request an in-app update
        appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                appUpdateType,
                this,
                REQUEST_CODE_UPDATE
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_UPDATE) {
            if (resultCode != RESULT_OK) {
                checkForAppUpdate();  // Retry the update process immediately
                showUpdateRequiredDialog();  // Keep showing the update dialog until updated
            }
        }
    }

    private void showUpdateRequiredDialog() {
        // Create a dialog to inform the user that the update is mandatory
        updateBottomSheet = new BottomSheetDialog(MainActivity.this);
        updateBottomSheet.setContentView(R.layout.update);
        updateBottomSheet.setCancelable(false);  // Disable canceling the dialog
        updateBottomSheet.setCanceledOnTouchOutside(false);  // Disable outside touch to cancel

        updateBottomSheet.getWindow().setNavigationBarColor(getResources().getColor(R.color.secondaryContainer)); // Customize the color
        String appPackageName = getPackageName(); // Your app's package name
        String appLink = "https://play.google.com/store/apps/details?id=" + appPackageName;

        updateBottomSheet.show();
        MaterialButton update = updateBottomSheet.findViewById(R.id.update);
        MaterialButton appInfo = updateBottomSheet.findViewById(R.id.app_info);
        appInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appLink));
                startActivity(browserIntent);
                updateBottomSheet.dismiss();
            }
        });
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(appLink));
                startActivity(browserIntent);
                updateBottomSheet.dismiss();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // For immediate updates: Check if the update is still in progress, then resume the update process.
        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // Resume the update process for immediate update
                try {
                    startAppUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE);
                } catch (IntentSender.SendIntentException e) {
                    throw new RuntimeException(e);
                }
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // If the update is available, ensure the update dialog is shown again
                showUpdateRequiredDialog();
            }
        });
    }


}