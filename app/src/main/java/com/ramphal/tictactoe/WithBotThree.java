package com.ramphal.tictactoe;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.transition.platform.MaterialSharedAxis;
import com.google.android.play.core.review.ReviewException;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.model.ReviewErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WithBotThree extends AppCompatActivity {

    private MaterialCardView[] cards;
    private ImageView[] images;
    private MaterialCardView oScr, xScr;
    private TextView yourTurn, playerX, playerO, playerXScore, playerOScore;
    private MaterialCardView lastMoveCard; // Add this to your class variables
    private MaterialButton forward_move, previous_move, extendedFab, exit, game_rules;
    private List<int[]> moveHistory = new ArrayList<>(); // To store move history
    private int currentMoveIndex = -1; // Track the current move index in the history
    private List<ImageView> winningCombination = new ArrayList<>(); // Store winning combination
    private int playerXScoreValue = 0;
    private int playerOScoreValue = 0;
    private String winnerTag;
    private ReviewManager reviewManager;
    private int showAdIn = 0;

    private boolean gameOver = false;
    private boolean isReplaying = false; // Track whether the user is replaying moves
    private int count = 0;
    private int flag = new Random().nextInt(2);

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    private static final String PREFS_NAME = "TicTacToePrefs";
    private static final String KEY_WIN = "win";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);

        MaterialSharedAxis sharedAxisEnter = new MaterialSharedAxis(MaterialSharedAxis.X,true);
        MaterialSharedAxis sharedAxisExit = new MaterialSharedAxis(MaterialSharedAxis.X,false);

        getWindow().setEnterTransition(sharedAxisEnter);
        getWindow().setExitTransition(sharedAxisExit);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_with_bot_three);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupWindowColors();
        initializeViews();
        setupInitialUIState();
        AdsServices.loadInterstitialAd(WithBotThree.this);

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.game_rules);
        bottomSheetDialog.setCancelable(true);
        bottomSheetDialog.setCanceledOnTouchOutside(true);
        bottomSheetDialog.getWindow().setNavigationBarColor(getResources().getColor(R.color.secondaryContainer)); // Replace 'your_color' with your desired color

        game_rules.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.show();
            }
        });

        bottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                getWindow().setNavigationBarColor(getResources().getColor(R.color.tertiaryContainer)); // Replace 'default_color' with your original color
            }
        });

        if (!gameOver && flag == 1) {
            disableUserMoves(); // Disable user moves
            Handler handler = new Handler(); // Create a Handler instance
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    botMove(); // Call botMove() after 1.5 seconds
                    enableUserMoves();
                }
            }, 900);
        }
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callNewGameSheet();
            }
        });
        extendedFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gameOver){
                    callGameResult(winnerTag);
                } else {
                    callNewGameSheet();
                }
            }
        });
    }

    private void setupWindowColors() {
        Window window = getWindow();
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.tertiaryContainer));
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.secondaryContainer));
    }

    private void initializeViews() {
        cards = new MaterialCardView[]{
                findViewById(R.id.card1), findViewById(R.id.card2), findViewById(R.id.card3),
                findViewById(R.id.card4), findViewById(R.id.card5), findViewById(R.id.card6),
                findViewById(R.id.card7), findViewById(R.id.card8), findViewById(R.id.card9)
        };

        images = new ImageView[]{
                findViewById(R.id.img1), findViewById(R.id.img2), findViewById(R.id.img3),
                findViewById(R.id.img4), findViewById(R.id.img5), findViewById(R.id.img6),
                findViewById(R.id.img7), findViewById(R.id.img8), findViewById(R.id.img9)
        };

        // Forward card click to image click
        for (int i = 0; i < cards.length; i++) {
            final ImageView imageView = images[i];
            cards[i].setOnClickListener(v -> imageView.performClick());
        }

        oScr = findViewById(R.id.o_scr);
        xScr = findViewById(R.id.x_scr);
        yourTurn = findViewById(R.id.your_turn);
        playerX = findViewById(R.id.Friend);
        playerO = findViewById(R.id.you);
        playerXScore = findViewById(R.id.Friend_sco);
        playerOScore = findViewById(R.id.you_sco);
        previous_move = findViewById(R.id.previous_move);
        forward_move = findViewById(R.id.forward_move);
        extendedFab = findViewById(R.id.extended_fab);
        exit = findViewById(R.id.exit);
        game_rules = findViewById(R.id.game_rules);

        reviewManager = ReviewManagerFactory.create(this);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    private void setupInitialUIState() {
        // Set up initial UI for scores and colors
        updateTurnDisplay(flag == 0); // X starts
        resetCards();

        // Disable buttons initially
        updateMoveButtons();
    }

    public void newGame() {
        // Reset the game state
        resetCards(); // Clears images and card styles
        moveHistory.clear(); // Clear the move history
        currentMoveIndex = -1; // Reset the current move index
        count = 0; // Reset move count
        flag = new Random().nextInt(2);
        gameOver = false; // Reset game over state
        isReplaying = false; // Reset replaying state
        winnerTag = null;
        // Clear the winning combination list if any
        winningCombination.clear();
        extendedFab.setText("New Game");
        extendedFab.setIcon(ContextCompat.getDrawable(this, R.drawable.add_icon));

        // Reset scores (optional, if you want to reset scores after every game)
        playerXScoreValue = 0;
        playerOScoreValue = 0;
        updateScoreDisplay();

        // Re-enable clickability for all image views
        enableUserMoves();

        // Reset any other necessary state like scores, etc.
        setupInitialUIState();
        if (!gameOver && flag == 1) {
            disableUserMoves(); // Disable user moves
            Handler handler = new Handler(); // Create a Handler instance
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    botMove(); // Call botMove() after 1.5 seconds
                    enableUserMoves();
                }
            }, 900);
        }
    }

    public void restartGame() {
        // Reset the game state
        resetCards(); // Clears images and card styles
        moveHistory.clear(); // Clear the move history
        currentMoveIndex = -1; // Reset the current move index
        count = 0; // Reset move count
        flag = new Random().nextInt(2);
        gameOver = false; // Reset game over state
        isReplaying = false; // Reset replaying state
        winnerTag = null;
        // Clear the winning combination list if any
        winningCombination.clear();
        extendedFab.setText("New Game");
        extendedFab.setIcon(ContextCompat.getDrawable(this, R.drawable.add_icon));

        updateScoreDisplay();

        // Re-enable clickability for all image views
        enableUserMoves();

        // Reset any other necessary state like scores, etc.
        setupInitialUIState();
        if (!gameOver && flag == 1) {
            disableUserMoves(); // Disable user moves
            Handler handler = new Handler(); // Create a Handler instance
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    botMove(); // Call botMove() after 1.5 seconds
                    enableUserMoves();
                }
            }, 900);
        }
    }

    private void resetCards() {
        // Reset all card backgrounds to default
        for (int i = 0; i < images.length; i++) {
            images[i].setImageDrawable(null);  // Clear image drawable
            images[i].setTag(null);  // Clear image tag
            cards[i].setCardBackgroundColor(ContextCompat.getColor(this, R.color.primaryContainer)); // Reset card background color
            cards[i].setStrokeColor(ContextCompat.getColor(this, R.color.primary)); // Reset card stroke color
        }
    }


    public void click_img(View view) {
        if (isReplaying || gameOver) {
            return;
        }

        ImageView imgCurrent = (ImageView) view;

        if (imgCurrent.getDrawable() == null) {
            String tag = (flag == 0) ? "O" : "X";  // User is "O", Bot is "X"
            Drawable drawable = ContextCompat.getDrawable(this, (flag == 0) ? R.drawable.o : R.drawable.x);

            imgCurrent.setImageDrawable(drawable);
            imgCurrent.setTag(tag);

            // Add the move to history
            int imgIndex = getImageViewIndex(imgCurrent);
            moveHistory.add(new int[]{imgIndex, flag});
            currentMoveIndex++;

            // Apply zoom-in animation to the new move
            applyZoomAnimation(imgCurrent, true);

            MaterialCardView correspondingCard = getCorrespondingCardView(imgCurrent.getId());
            if (correspondingCard != null) {
                updateCardStroke(correspondingCard, tag);
                highlightLastMove(correspondingCard, tag);
            }

            count++;
            flag = 1 - flag; // Switch turns
            updateTurnDisplay(flag == 0);

            if (count >= 5) {
                checkWinner();
            }

            // Ensure replay mode is reset to allow normal gameplay
            isReplaying = false;

            updateMoveButtons();

            // If the game is not over and it's the bot's turn, disable user moves and make the bot move
            if (!gameOver && flag == 1) {
                disableUserMoves(); // Disable user moves
                Handler handler = new Handler(); // Create a Handler instance
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        botMove(); // Call botMove() after 1.5 seconds
                        enableUserMoves();
                    }
                }, 900);
            }
        }
    }

    private void botMove() {
        if (isReplaying || gameOver) {
            return;
        }

        // Step 1: Check if the bot (X) can win in the next move
        int winningMove = findWinningMove("X");
        if (winningMove != -1) {
            images[winningMove].performClick();
            return;
        }

        // Step 2: Check if the player (O) can win in the next move and block it
        int blockingMove = findWinningMove("O");
        if (blockingMove != -1) {
            images[blockingMove].performClick();
            return;
        }

        // Step 3: Take the center if available
        if (images[4].getTag() == null) {
            images[4].performClick();
            return;
        }

        // Step 4: Check if the bot can create a fork
        int forkMove = findForkMove("X");
        if (forkMove != -1) {
            images[forkMove].performClick();
            return;
        }

        // Step 5: Block opponent's fork if possible
        int opponentForkMove = findForkMove("O");
        if (opponentForkMove != -1) {
            images[opponentForkMove].performClick();
            return;
        }

        // Step 8: Choose a random available spot if no strategic moves are possible
        List<Integer> availableMoves = new ArrayList<>();
        for (int i = 0; i < images.length; i++) {
            if (images[i].getTag() == null) {
                availableMoves.add(i);
            }
        }

        if (!availableMoves.isEmpty()) {
            int botMoveIndex = availableMoves.get(new Random().nextInt(availableMoves.size()));
            images[botMoveIndex].performClick();
        }
    }

    private int findWinningMove(String playerTag) {
        // Check if there's a winning move for the specified player (X for bot, O for player)
        ImageView[][] winningCombinations = {
                {images[0], images[1], images[2]}, {images[3], images[4], images[5]}, {images[6], images[7], images[8]}, // Horizontal
                {images[0], images[3], images[6]}, {images[1], images[4], images[7]}, {images[2], images[5], images[8]}, // Vertical
                {images[0], images[4], images[8]}, {images[2], images[4], images[6]}  // Diagonal
        };

        for (ImageView[] combination : winningCombinations) {
            int emptyIndex = getEmptyIndexInCombination(combination);
            if (emptyIndex != -1) {
                String tag1 = (String) combination[0].getTag();
                String tag2 = (String) combination[1].getTag();
                String tag3 = (String) combination[2].getTag();

                // Check if two out of three have the same player's tag
                if (tag1 != null && tag1.equals(playerTag) && tag2 != null && tag2.equals(playerTag) && combination[2].getTag() == null) {
                    return getImageViewIndex(combination[2]);
                } else if (tag1 != null && tag1.equals(playerTag) && tag3 != null && tag3.equals(playerTag) && combination[1].getTag() == null) {
                    return getImageViewIndex(combination[1]);
                } else if (tag2 != null && tag2.equals(playerTag) && tag3 != null && tag3.equals(playerTag) && combination[0].getTag() == null) {
                    return getImageViewIndex(combination[0]);
                }
            }
        }
        return -1; // No winning move found
    }

    private int findForkMove(String playerTag) {
        List<Integer> potentialForks = new ArrayList<>();

        ImageView[][] winningCombinations = {
                {images[0], images[1], images[2]}, {images[3], images[4], images[5]}, {images[6], images[7], images[8]}, // Horizontal
                {images[0], images[3], images[6]}, {images[1], images[4], images[7]}, {images[2], images[5], images[8]}, // Vertical
                {images[0], images[4], images[8]}, {images[2], images[4], images[6]}  // Diagonal
        };

        // Step 1: Loop through all available spots
        for (int i = 0; i < images.length; i++) {
            if (images[i].getTag() == null) {
                // Temporarily set the tag to simulate the move
                images[i].setTag(playerTag);

                // Step 2: Check how many winning moves this move creates
                int winningMoves = 0;
                for (ImageView[] combination : winningCombinations) {
                    int emptyIndex = getEmptyIndexInCombination(combination);

                    if (emptyIndex != -1) {
                        String tag1 = (String) combination[0].getTag();
                        String tag2 = (String) combination[1].getTag();
                        String tag3 = (String) combination[2].getTag();

                        if ((tag1 != null && tag1.equals(playerTag) && tag2 != null && tag2.equals(playerTag)) ||
                                (tag1 != null && tag1.equals(playerTag) && tag3 != null && tag3.equals(playerTag)) ||
                                (tag2 != null && tag2.equals(playerTag) && tag3 != null && tag3.equals(playerTag))) {
                            winningMoves++;
                        }
                    }
                }

                // Step 3: If the move creates more than one winning opportunity, consider it a fork
                if (winningMoves >= 2) {
                    potentialForks.add(i);
                }

                // Step 4: Undo the temporary move
                images[i].setTag(null);
            }
        }

        // Step 5: Return the first fork move found or -1 if none found
        if (!potentialForks.isEmpty()) {
            return potentialForks.get(0);
        }

        return -1; // No fork move found
    }

    private int getEmptyIndexInCombination(ImageView[] combination) {
        for (int i = 0; i < combination.length; i++) {
            if (combination[i].getTag() == null) {
                return i;
            }
        }
        return -1;
    }

    private int getImageViewIndex(ImageView img) {
        for (int i = 0; i < images.length; i++) {
            if (images[i] == img) {
                return i;
            }
        }
        return -1; // Not found
    }



    public void previousMove(View view) {
        if (currentMoveIndex > 0) {
            isReplaying = true; // Enter replay mode

            // If user won the game, restore the original winning combination
            if (!winningCombination.isEmpty()) {
                restoreWinningCombination();
            }

            // Clear the current move
            int[] currentMove = moveHistory.get(currentMoveIndex);
            clearMove(currentMove[0]);

            currentMoveIndex--;

            // Restore the previous move
            int[] previousMove = moveHistory.get(currentMoveIndex);
            restoreMove(previousMove[0], previousMove[1]);

            // Apply zoom-out animation to the restored move
            applyZoomAnimation(images[previousMove[0]], false);

            flag = 1 - previousMove[1]; // Update flag to reflect the last player's turn
            updateTurnDisplay(flag == 0);

            // Update move buttons after moving backward
            updateMoveButtons();
        }
    }

    public void forwardMove(View view) {
        if (currentMoveIndex < moveHistory.size() - 1) {
            currentMoveIndex++;

            // Restore the move
            int[] nextMove = moveHistory.get(currentMoveIndex);
            restoreMove(nextMove[0], nextMove[1]);

            // Apply zoom-in animation to the restored move
            applyZoomAnimation(images[nextMove[0]], true);

            flag = 1 - nextMove[1]; // Update flag to reflect the last player's turn
            updateTurnDisplay(flag == 0);

            // If we are at the most recent move, exit replay mode
            if (currentMoveIndex == moveHistory.size() - 1) {
                isReplaying = false;  // Only stop replaying if it's a completed game and not a replay
            }

            // Only check for a winner if the game is not being replayed
            if (!isReplaying) {
                checkWinner();
            }

            // Update move buttons after moving forward
            updateMoveButtons();
        }
    }


    private void applyZoomAnimation(ImageView img, boolean zoomIn) {
        float fromScale = zoomIn ? 0.8f : 1.2f;  // Zoom-in or zoom-out effect
        float toScale = 1.0f;  // Normal size
        ScaleAnimation zoomAnimation = new ScaleAnimation(
                fromScale, toScale,   // Start and end scale for X
                fromScale, toScale,   // Start and end scale for Y
                Animation.RELATIVE_TO_SELF, 0.5f,  // Pivot point for X (center)
                Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point for Y (center)
        zoomAnimation.setDuration(150);  // Animation duration
        zoomAnimation.setFillAfter(true);  // Keep the final state
        img.startAnimation(zoomAnimation);  // Start animation on the ImageView
    }

    private void clearMove(int imgIndex) {
        images[imgIndex].setImageDrawable(null);
        images[imgIndex].setTag(null);
        MaterialCardView correspondingCard = cards[imgIndex];
        correspondingCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primaryContainer));
        correspondingCard.setStrokeColor(ContextCompat.getColor(this, R.color.primary));
    }

    private void restoreMove(int imgIndex, int player) {
        String tag = (player == 0) ? "O" : "X";
        Drawable drawable = ContextCompat.getDrawable(this, (player == 0) ? R.drawable.o : R.drawable.x);

        images[imgIndex].setImageDrawable(drawable);
        images[imgIndex].setTag(tag);
        MaterialCardView correspondingCard = cards[imgIndex];
        updateCardStroke(correspondingCard, tag);
        if (lastMoveCard != null) {
            lastMoveCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primaryContainer));
        }
        updateCardBackground(correspondingCard, tag);
        lastMoveCard = correspondingCard;
    }

    private void updateMoveButtons() {
        previous_move.setEnabled(currentMoveIndex > 0); // Enable if there's a previous move
        forward_move.setEnabled(currentMoveIndex < moveHistory.size() - 1); // Enable if there's a forward move
        if (previous_move.isEnabled()) {
            previous_move.setIconTintResource(R.color.primary);
        } else {
            previous_move.setIconTintResource(R.color.onTertiary);
        }
        if (forward_move.isEnabled()) {
            forward_move.setIconTintResource(R.color.primary);
        } else {
            forward_move.setIconTintResource(R.color.onTertiary);
        }
    }

    private void restoreWinningCombination() {
        if (!winningCombination.isEmpty()) {
            for (ImageView img : winningCombination) {
                String tag = (String) img.getTag();
                Drawable drawable = ContextCompat.getDrawable(this, "O".equals(tag) ? R.drawable.o : R.drawable.x);
                img.setImageDrawable(drawable);

                MaterialCardView correspondingCard = getCorrespondingCardView(img.getId());
                if (correspondingCard != null) {
                    correspondingCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primaryContainer));
                }
            }
            winningCombination.clear(); // Clear after restoring
        }
    }

    private void highlightLastMove(MaterialCardView currentCard, String tag) {
        // Reset the background color of the last move
        if (lastMoveCard != null) {
            lastMoveCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primaryContainer));
        }
        // Set the background color of the current move
        updateCardStroke(currentCard, tag);
        updateCardBackground(currentCard, tag);

        // Update the last move card
        lastMoveCard = currentCard;
    }

    private void updateTurnDisplay(boolean isXTurn) {
        // Update the UI to reflect whose turn it is (X or O)
        xScr.setCardBackgroundColor(ContextCompat.getColor(this, isXTurn ? R.color.primaryContainer : R.color.tertiaryContainer));
        oScr.setCardBackgroundColor(ContextCompat.getColor(this, isXTurn ? R.color.tertiaryContainer : R.color.primaryContainer));

        playerO.setTextColor(ContextCompat.getColor(this, isXTurn ? R.color.primary : R.color.tertiaryContainer));
        playerOScore.setTextColor(ContextCompat.getColor(this, isXTurn ? R.color.primary : R.color.tertiaryContainer));
        playerX.setTextColor(ContextCompat.getColor(this, isXTurn ? R.color.tertiaryContainer : R.color.primary));
        playerXScore.setTextColor(ContextCompat.getColor(this, isXTurn ? R.color.tertiaryContainer: R.color.primary));

        yourTurn.setTextColor(ContextCompat.getColor(this, isXTurn ? R.color.lower_Green_container : R.color.lower_Orange_container));
        yourTurn.setText(isXTurn ? "Your Turn!" : "Friend's Turn!");
    }

    private MaterialCardView getCorrespondingCardView(int imageViewId) {
        for (int i = 0; i < images.length; i++) {
            if (images[i].getId() == imageViewId) {
                return cards[i];  // Return the corresponding card
            }
        }
        return null;  // Return null if no match is found
    }


    private void updateCardStroke(MaterialCardView card, String tag) {
        int strokeColor = ContextCompat.getColor(this, "O".equals(tag) ? R.color.lower_Green_container : R.color.lower_Orange_container);
        card.setStrokeColor(strokeColor);
    }

    private void updateCardBackground(MaterialCardView card, String tag) {
        int backgroundColor = ContextCompat.getColor(this, "O".equals(tag) ? R.color.light_back_Green_container : R.color.light_back_Orange_container);
        card.setCardBackgroundColor(backgroundColor);
    }


    private void checkWinner() {
        // Define all possible winning combinations
        ImageView[][] winningCombinations = {
                {images[0], images[1], images[2]}, {images[3], images[4], images[5]}, {images[6], images[7], images[8]}, // Horizontal
                {images[0], images[3], images[6]}, {images[1], images[4], images[7]}, {images[2], images[5], images[8]}, // Vertical
                {images[0], images[4], images[8]}, {images[2], images[4], images[6]}  // Diagonal
        };

        // Check each combination
        for (ImageView[] combination : winningCombinations) {
            if (checkThree(combination[0], combination[1], combination[2])) {
                highlightWinningSequence(combination);
                return;
            }
        }

        // If no winner is found, check for a draw
        if (isBoardFull()) {
            disableUserMoves();
            endGame();
        }
    }

    private boolean checkThree(ImageView img1, ImageView img2, ImageView img3) {
        // Check if three images have the same tag and are not null
        return img1.getTag() != null && img1.getTag().equals(img2.getTag()) && img2.getTag().equals(img3.getTag());
    }

    // Helper method to check if the board is full
    private boolean isBoardFull() {
        for (ImageView image : images) {
            // Check if any image has not been marked (for example, you can check if it's still using the default empty state)
            if (image.getDrawable() == null) { // Assuming that null means the spot is still empty
                return false; // Board is not full
            }
        }
        return true; // Board is full
    }

    private void highlightWinningSequence(ImageView[] winningImages) {
        winnerTag = (String) winningImages[0].getTag();
        Drawable drawable = ContextCompat.getDrawable(this, "O".equals(winnerTag) ? R.drawable.o_light : R.drawable.x_light);
        int color = ContextCompat.getColor(this, "O".equals(winnerTag) ? R.color.lower_Green_container : R.color.lower_Orange_container);

        // Store the winning combination
        winningCombination.clear(); // Clear any existing combination
        for (ImageView img : winningImages) {
            winningCombination.add(img); // Store each winning image

            img.setImageDrawable(drawable);

            // Get the corresponding card and change its background color
            MaterialCardView correspondingCard = getCorrespondingCardView(img.getId());
            if (correspondingCard != null) {
                correspondingCard.setCardBackgroundColor(color);
            }
        }

        if (!isReplaying && !gameOver) {  // Check both replay and game-over state
            // Increment the score for the winner
            if ("O".equals(winnerTag)) {
                playerOScoreValue++;  // O player wins
                updateScoreDisplay();
            } else if ("X".equals(winnerTag)) {
                playerXScoreValue++;  // X player wins
                updateScoreDisplay();
            }
        }

        disableUserMoves();
        endGame();
    }

    private void updateScoreDisplay() {
        playerXScore.setText(String.valueOf(playerXScoreValue));  // Update X's score
        playerOScore.setText(String.valueOf(playerOScoreValue));  // Update O's score
    }

    private void endGame() {
        if (!isReplaying && !gameOver) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    callGameResult(winnerTag);
                }
            }, 600);
        }
        xScr.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primaryContainer));
        oScr.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primaryContainer));
        playerO.setTextColor(ContextCompat.getColor(this, R.color.primary));
        playerOScore.setTextColor(ContextCompat.getColor(this, R.color.primary));
        playerX.setTextColor(ContextCompat.getColor(this, R.color.primary));
        playerXScore.setTextColor(ContextCompat.getColor(this, R.color.primary));
        yourTurn.setTextColor(ContextCompat.getColor(this, R.color.primary));
        yourTurn.setText("Game Over!");

        gameOver = true;  // Set game over flaghandler = new Handler(Looper.getMainLooper());
        extendedFab.setIcon(ContextCompat.getDrawable(this, R.drawable.arrow_up));
        extendedFab.setText("Show Result");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void callNewGameSheet(){
        exit.setIcon(ContextCompat.getDrawable(WithBotThree.this, R.drawable.play));
        BottomSheetDialog new_game_bottom_sheet = new BottomSheetDialog(this);
        new_game_bottom_sheet.setContentView(R.layout.new_game);
        new_game_bottom_sheet.setCancelable(true);
        new_game_bottom_sheet.setCanceledOnTouchOutside(true);
        new_game_bottom_sheet.getWindow().setNavigationBarColor(getResources().getColor(R.color.secondaryContainer)); // Replace 'your_color' with your desired color
        TextView you_sco = new_game_bottom_sheet.findViewById(R.id.you_sco);
        TextView Friend_sco = new_game_bottom_sheet.findViewById(R.id.Friend_sco);
        MaterialButton home = new_game_bottom_sheet.findViewById(R.id.home);
        MaterialButton new_game = new_game_bottom_sheet.findViewById(R.id.new_game);
        TextView you = new_game_bottom_sheet.findViewById(R.id.you);
        TextView Friend = new_game_bottom_sheet.findViewById(R.id.Friend);
        you_sco.setText(String.valueOf(playerOScoreValue));
        Friend_sco.setText(String.valueOf(playerXScoreValue));
        you.setText("You:");
        Friend.setText("Bot:");
        new_game_bottom_sheet.show();
        new_game.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGame();
                if (showAdIn == 3){
                    showAdIn = 0;
                }
                showAdIn = showAdIn + 1;
                new_game_bottom_sheet.dismiss();
            }
        });
        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new_game_bottom_sheet.dismiss();
                onBackPressed();
            }
        });
        new_game_bottom_sheet.setOnCancelListener(dialog -> {
            // Custom behavior when back is pressed or dialog is canceled
            new_game_bottom_sheet.dismiss();  // Dismiss the bottom sheet on back press
        });
        new_game_bottom_sheet.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                getWindow().setNavigationBarColor(getResources().getColor(R.color.tertiaryContainer)); // Replace 'default_color' with your original color
                exit.setIcon(ContextCompat.getDrawable(WithBotThree.this, R.drawable.pause));
            }
        });
    }

    public void callGameResult(String winnerTag){
        BottomSheetDialog result_bottom_sheet = new BottomSheetDialog(this);
        result_bottom_sheet.setContentView(R.layout.result);
        result_bottom_sheet.setCancelable(true);
        result_bottom_sheet.setCanceledOnTouchOutside(true);
        result_bottom_sheet.getWindow().setNavigationBarColor(getResources().getColor(R.color.secondaryContainer)); // Replace 'your_color' with your desired color
        LottieAnimationView animation_view = result_bottom_sheet.findViewById(R.id.animation_view);
        TextView textView = result_bottom_sheet.findViewById(R.id.textView);
        TextView textView2 = result_bottom_sheet.findViewById(R.id.textView2);
        MaterialButton new_game = result_bottom_sheet.findViewById(R.id.new_game);
        MaterialButton rematch = result_bottom_sheet.findViewById(R.id.rematch);
        if ("O".equals(winnerTag)) {
            animation_view.setAnimation(R.raw.win);
            textView.setText("Congratulations!");
            textView2.setText("You win this round");
            int winPref = sharedPreferences.getInt(KEY_WIN, 0);
            winPref = winPref + 1;
            editor.putInt(KEY_WIN, winPref);
            editor.apply();
        } else if ("X".equals(winnerTag)) {
            animation_view.setAnimation(R.raw.bot);
            textView.setText("Oops! You Lost!");
            textView2.setText("Bot win this round");
        } else if (winnerTag == null) {
            animation_view.setAnimation(R.raw.draw);
            textView.setText("It's a Draw!");
            textView2.setText("No one wins this round!");
        }
        result_bottom_sheet.show();
        new_game.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGame();
                if (showAdIn == 3){
                    AdsServices.showAd(WithBotThree.this);
                    AdsServices.loadInterstitialAd(WithBotThree.this);
                    showAdIn = 0;
                }
                showAdIn = showAdIn + 1;
                result_bottom_sheet.dismiss();
            }
        });
        rematch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartGame();
                if (showAdIn == 3){
                    AdsServices.showAd(WithBotThree.this);
                    AdsServices.loadInterstitialAd(WithBotThree.this);
                    showAdIn = 0;
                }
                showAdIn = showAdIn + 1;
                result_bottom_sheet.dismiss();
            }
        });
        result_bottom_sheet.setOnCancelListener(dialog -> {
            // Custom behavior when back is pressed or dialog is canceled
            result_bottom_sheet.dismiss();  // Dismiss the bottom sheet on back press
        });
        result_bottom_sheet.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                getWindow().setNavigationBarColor(getResources().getColor(R.color.tertiaryContainer)); // Replace 'default_color' with your original color
                int winPref = sharedPreferences.getInt(KEY_WIN, 0);
                if (winPref == 18){
                    winPref = 0;
                    editor.putInt(KEY_WIN, winPref);
                    editor.apply();
                    startInAppReview();
                }
            }
        });
    }

    private void disableUserMoves() {
        for (ImageView img : images) {
            img.setClickable(false);
        }
        for (MaterialCardView card : cards) {
            card.setClickable(false);
        }
    }

    private void enableUserMoves() {
        for (ImageView img : images) {
            img.setClickable(true);
        }
        for (MaterialCardView card : cards) {
            card.setClickable(true);
        }
    }

    private void startInAppReview() {
        // Request a ReviewInfo instance
        Task<ReviewInfo> request = reviewManager.requestReviewFlow();

        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // We can get the ReviewInfo object
                ReviewInfo reviewInfo = task.getResult();
                launchReviewFlow(reviewInfo);
            } else {
                // Handle any errors if request fails
                @ReviewErrorCode int reviewErrorCode = ((ReviewException) task.getException()).getErrorCode();
            }
        });
    }

    private void launchReviewFlow(ReviewInfo reviewInfo) {
        // Launch the in-app review flow
        Task<Void> flow = reviewManager.launchReviewFlow(this, reviewInfo);

        flow.addOnCompleteListener(task -> {
            // The review flow has finished
        });
    }
}
