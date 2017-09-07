
package com.deitel.spoton;

import android.graphics.Color;
import android.os.CountDownTimer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class SpotOnView extends View
{
    // constant for accessing the high score in SharedPreference
    private SharedPreferences preferences; // stores the high score
    private List<Integer> numbers = new ArrayList<Integer>();

    // variables for managing the game
    private int spotsTouched; // number of spots touched
    private int score; // current score
    private int level = 1; // current level
    private int viewWidth; // stores the width of this View
    private int viewHeight; // stores the height of this view
    private long animationTime; // how long each spot remains on the screen
    public static boolean gameOver; // whether the game has ended
    private boolean dialogDisplayed=false; // whether the game has ended
    private int highScore; // the game's all time high score

    // collections of spots (ImageViews) and Animators
    private final Queue<ImageView> spots =
            new ConcurrentLinkedQueue<ImageView>();
    private final Queue<Animator> animators =
            new ConcurrentLinkedQueue<Animator>();


    private TextView levelTextView; // displays current level
   private RelativeLayout relativeLayout; // displays spots
    private Resources resources; // used to load resources
    private LayoutInflater layoutInflater; // used to inflate GUIs
    private int levelInitialValue = 0; // used to inflate GUIs
    private int level4InitialValue = 0; // used to inflate GUIs

    // time in milliseconds for spot and touched spot animations
    private static final int INITIAL_ANIMATION_DURATION = 60000;
    private static final Random random = new Random(); // for random coords
    private static final int SPOT_DIAMETER = 100; // initial spot size
    private static final float SCALE_X = 0.5f; // end animation x scale
    private static final float SCALE_Y = 0.5f; // end animation y scale
    private static final int INITIAL_SPOTS = 1; // initial # of spots
    private static final int SPOT_DELAY = 1000; // delay in milliseconds
    private static final int NEW_LEVEL = 10; // spots to reach new level
    private Handler spotHandler; // adds new spots to the game

    // sound IDs, constants and variables for the game's sounds
    private static final int UH_OH = 31;
    private static final int APPLAUSE = 32;
    public  TextView timer; // stores the high score

    private static final int SOUND_PRIORITY = 1;
    private static final int SOUND_QUALITY = 100;
    private static final int MAX_STREAMS = 4;
    private SoundPool soundPool; // plays sound effects
    private int volume; // sound effect volume
    private Map<Integer, Integer> soundMap; // maps ID to soundpool
    CountDownTimer levelOne;
    CountDownTimer levelTwo;
    CountDownTimer levelThree;
    CountDownTimer levelFour;


    public Context context;
    // constructs a new SpotOnView
    public SpotOnView(Context context, SharedPreferences sharedPreferences,
                      RelativeLayout parentLayout)
    {
        super(context);

        // load the high score
        preferences = sharedPreferences;
        this.context =context;
        // save Resources for loading external values
        resources = context.getResources();

        // save LayoutInflater
        layoutInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        // get references to various GUI components
        relativeLayout = parentLayout;

        if(level < 5)
        levelTextView = (TextView) relativeLayout.findViewById(
                R.id.levelTextView);
        timer = (TextView) parentLayout.findViewById(R.id.remainingTextView);

        spotHandler = new Handler();
    } // end SpotOnView constructor

    // store SpotOnView's width/height
    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh)
    {
        viewWidth = width; // save the new width
        viewHeight = height; // save the new height
    } // end method onSizeChanged

    // called by the SpotOn Activity when it receives a call to onPause
    public void pause()
    {
        soundPool.release(); // release audio resources
        soundPool = null;
        cancelAnimations(); // cancel all outstanding animations
    } // end method pause

    // cancel animations and remove ImageViews representing spots
    private void cancelAnimations()
    {
        // cancel remaining animations
        for (Animator animator : animators)
            animator.cancel();

        // remove remaining spots from the screen
        for (ImageView view : spots)
            relativeLayout.removeView(view);

        spotHandler.removeCallbacks(addSpotRunnable);
        animators.clear();
        spots.clear();
    } // end method cancelAnimations

    // called by the SpotOn Activity when it receives a call to onResume
    public void resume(Context context)
    {
        initializeSoundEffects(context); // initialize app's SoundPool

        if (!dialogDisplayed)
            resetGame(); // start the game
    } // end method resume

    // start a new game
    public void resetGame()
    {
        spots.clear(); // empty the List of spots
        animators.clear(); // empty the List of Animators
        int k =spots.size(); // empty the List of spots

        animationTime = INITIAL_ANIMATION_DURATION; // init animation length
        spotsTouched = 0; // reset the number of spots touched
        score = 0; // reset the score
        level = 1; // reset the level
        gameOver = false; // the game is not over
        displayScores(); // display scores and level



        // add INITIAL_SPOTS new spots at SPOT_DELAY time intervals in ms
        for (int i = 1; i <= INITIAL_SPOTS; ++i)
            spotHandler.postDelayed(addSpotRunnable, i * SPOT_DELAY);
    } // end method resetGame

    // create the app's SoundPool for playing game audio
    private void initializeSoundEffects(Context context)
    {
        // initialize SoundPool to play the app's three sound effects
        soundPool = new SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .build();

        // set sound effect volume
        AudioManager manager =
                (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // create sound map
        soundMap = new HashMap<Integer, Integer>(); // create new HashMap

        // add each sound effect to the SoundPool
        soundMap.put(1,soundPool.load(context, R.raw.onevoice, SOUND_PRIORITY));
        soundMap.put(2,soundPool.load(context, R.raw.twovoice, SOUND_PRIORITY));
        soundMap.put(3,soundPool.load(context, R.raw.threevoice, SOUND_PRIORITY));
        soundMap.put(4,soundPool.load(context, R.raw.fourvoice, SOUND_PRIORITY));
        soundMap.put(5,soundPool.load(context, R.raw.fivevoice, SOUND_PRIORITY));
        soundMap.put(6,soundPool.load(context, R.raw.sixvoice, SOUND_PRIORITY));
        soundMap.put(7,soundPool.load(context, R.raw.sevenvoice, SOUND_PRIORITY));
        soundMap.put(8,soundPool.load(context, R.raw.eightvoice, SOUND_PRIORITY));
        soundMap.put(9,soundPool.load(context, R.raw.ninevoice, SOUND_PRIORITY));
        soundMap.put(10,soundPool.load(context, R.raw.tenvoice, SOUND_PRIORITY));
        soundMap.put(11,soundPool.load(context, R.raw.elevenvoice, SOUND_PRIORITY));
        soundMap.put(12,soundPool.load(context, R.raw.twelvevoice, SOUND_PRIORITY));
        soundMap.put(13,soundPool.load(context, R.raw.thirteenvoice, SOUND_PRIORITY));
        soundMap.put(14,soundPool.load(context, R.raw.fourteenvoice, SOUND_PRIORITY));
        soundMap.put(15,soundPool.load(context, R.raw.fifteenvoice, SOUND_PRIORITY));
        soundMap.put(16,soundPool.load(context, R.raw.sixteenvoice, SOUND_PRIORITY));
        soundMap.put(17,soundPool.load(context, R.raw.seventeenvoice, SOUND_PRIORITY));
        soundMap.put(18,soundPool.load(context, R.raw.eighteenvoice, SOUND_PRIORITY));
        soundMap.put(19,soundPool.load(context, R.raw.nineteenvoice, SOUND_PRIORITY));
        soundMap.put(20,soundPool.load(context, R.raw.twentyvoice, SOUND_PRIORITY));
        soundMap.put(21,soundPool.load(context, R.raw.twentyonevoice, SOUND_PRIORITY));
        soundMap.put(22,soundPool.load(context, R.raw.twentytwovoice, SOUND_PRIORITY));
        soundMap.put(23,soundPool.load(context, R.raw.twentythreevoice, SOUND_PRIORITY));
        soundMap.put(24,soundPool.load(context, R.raw.twentyfourvoice, SOUND_PRIORITY));
        soundMap.put(25,soundPool.load(context, R.raw.twentyfivevoice, SOUND_PRIORITY));
        soundMap.put(26,soundPool.load(context, R.raw.twentysixvoice, SOUND_PRIORITY));
        soundMap.put(27,soundPool.load(context, R.raw.twentysevenvoice, SOUND_PRIORITY));
        soundMap.put(28,soundPool.load(context, R.raw.twentyeightvoice, SOUND_PRIORITY));
        soundMap.put(29,soundPool.load(context, R.raw.twentyninevoice, SOUND_PRIORITY));
        soundMap.put(30,soundPool.load(context, R.raw.thirtyvoice, SOUND_PRIORITY));
        soundMap.put(UH_OH,soundPool.load(context, R.raw.uhoh, SOUND_PRIORITY));
        soundMap.put(APPLAUSE,soundPool.load(context, R.raw.applause, SOUND_PRIORITY));

    } // end method initializeSoundEffect

    // display scores and level
    private void displayScores()
    {
        // display the high score, current score and level

        if(level < 5)
        levelTextView.setText(
                resources.getString(R.string.level) + " " + level);
    } // end function displayScores
private void gameOver(){

   for(ImageView spot : spots)
       relativeLayout.removeView(spot);
     levelInitialValue = 0; // used to inflate GUIs
     level4InitialValue = 0;

    new AlertDialog.Builder(context)
            .setTitle(resources.getString(R.string.game_over))
            .setMessage(resources.getString(R.string.game_over_restart))
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    resetGame();
                }
            })

            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();


                }

    // Runnable used to add new spots to the game at the start
    private Runnable addSpotRunnable = new Runnable()
    {
        public void run()
        {
            levels(); // add a new spot to the game
        } // end method run
    }; // end Runnable

    // adds a new spot at a random location and starts its animation
    public void addNewSpot(int drawableID,int id)
    {
        // choose two random coordinates for the starting and ending points
        int x = random.nextInt(viewWidth - SPOT_DIAMETER);
        int y = random.nextInt(viewHeight - SPOT_DIAMETER);
        int x2 = random.nextInt(viewWidth - SPOT_DIAMETER);
        int y2 = random.nextInt(viewHeight - SPOT_DIAMETER);

        // create new spot
        final ImageView spot =
                (ImageView) layoutInflater.inflate(R.layout.untouched, null);
        spots.add(spot); // add the new spot to our list of spots
        spot.setLayoutParams(new RelativeLayout.LayoutParams(
                SPOT_DIAMETER, SPOT_DIAMETER));
        spot.setImageResource(drawableID);
        spot.setId(id);
        spot.setX(x); // set spot's starting x location
        spot.setY(y); // set spot's starting y location
        spot.setOnClickListener( // listens for spot being clicked
                new OnClickListener()
                {
                    public void onClick(View v)
                    {
                        touchedSpot(spot); // handle touched spot
                    } // end method onClick
                } // end OnClickListener
        ); // end call to setOnClickListener
        relativeLayout.addView(spot); // add spot to the screen

        // configure and start spot's animation
        spot.animate().x(x2).y(y2).scaleX(SCALE_X).scaleY(SCALE_Y)
                .setDuration(animationTime).setListener(
                new AnimatorListenerAdapter()
                {
                    @Override
                    public void onAnimationStart(Animator animation)
                    {
                        animators.add(animation); // save for possible cancel
                    } // end method onAnimationStart

                    public void onAnimationEnd(Animator animation)
                    {
                        animators.remove(animation); // animation done, remove

                    } // end method onAnimationEnd
                } // end AnimatorListenerAdapter
        ); // end call to setListener
    } // end addNewSpot method



    private void touchedSpot(ImageView spot)
    {
        boolean flagCorrectClick = false;
        if(level==4){
            if(spot.getId()==numbers.get(level4InitialValue))
            {  flagCorrectClick = true;
            level4InitialValue+=1;}

        }
        else{
            if(spot.getId()==levelInitialValue+1)
                flagCorrectClick = true;
        }
        if(flagCorrectClick) {
            relativeLayout.removeView(spot);
            spots.remove(spot);
            ++spotsTouched; // increment the number of spots touched

            if (soundPool != null)
                soundPool.play(soundMap.get(spot.getId()), volume, volume,
                        SOUND_PRIORITY, 0, 1f);

            // increment level if player touched 10 spots in the current level
            if(level==4 && spotsTouched==40)
            {
                if (soundPool != null)
                    soundPool.play(soundMap.get(APPLAUSE), volume, volume,
                            SOUND_PRIORITY, 0, 1f);

                levelFour.cancel();
                new AlertDialog.Builder(context)
                        .setTitle(resources.getString(R.string.congrats))
                        .setMessage(resources.getString(R.string.congrats_game))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                resetGame();

                            }
                        })

                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
                timer.setText(resources.getString(R.string.remainingTime) + " Game Completed");

            }
            if (spotsTouched !=0 && spotsTouched % NEW_LEVEL == 0 ) {
                int time = 60;

                ++level; // increment the level
                if(level==4){
                    levelThree.cancel();
                    levelFour();
                }
                else if(level<4){
                    if(level==2)
                        levelOne.cancel();
                    if(level==3)
                        levelTwo.cancel();
                    levels();
                }


            }
            levelInitialValue+=1;
        }
        else if (soundPool != null)
            soundPool.play(soundMap.get(UH_OH), volume, volume,
                    SOUND_PRIORITY, 0, 1f);

        if(level < 5)
        levelTextView.setText(
                resources.getString(R.string.level) + " " + level);

    }



    private void levels(){
        if(level==1){
            addNewSpot(R.drawable.one,1);
            addNewSpot(R.drawable.two,2);
            addNewSpot(R.drawable.three,3);
            addNewSpot(R.drawable.four,4);
            addNewSpot(R.drawable.five,5);
            addNewSpot(R.drawable.six,6);
            addNewSpot(R.drawable.seven,7);
            addNewSpot(R.drawable.eight,8);
            addNewSpot(R.drawable.nine,9);
            addNewSpot(R.drawable.ten,10);
           levelOne = new CountDownTimer(60000, 1000) {

               public void onTick(long millisUntilFinished) {
                   timer.setText(resources.getString(R.string.remainingTime) +" "+ millisUntilFinished / 1000);
                   if(millisUntilFinished / 1000 <10){
                       timer.setTextColor(Color.RED);
                   }else  timer.setTextColor(Color.BLACK);
               }

               public void onFinish() {
                   timer.setText(resources.getString(R.string.remainingTime) + " Game Over");
                   gameOver =false;
                   gameOver();
               }

            }.start();
        }
        else if(level == 2) {
            addNewSpot(R.drawable.eleven, 11);
            addNewSpot(R.drawable.twelve, 12);
            addNewSpot(R.drawable.thirteen, 13);
            addNewSpot(R.drawable.fourteen, 14);
            addNewSpot(R.drawable.fifteen, 15);
            addNewSpot(R.drawable.sixteen, 16);
            addNewSpot(R.drawable.seventeen, 17);
            addNewSpot(R.drawable.eighteen, 18);
            addNewSpot(R.drawable.nineteen, 19);
            addNewSpot(R.drawable.twenty, 20);
           levelTwo = new CountDownTimer(55000, 1000) {

               public void onTick(long millisUntilFinished) {
                   timer.setText(resources.getString(R.string.remainingTime) +" "+ millisUntilFinished / 1000);
                   if(millisUntilFinished / 1000 <10){
                       timer.setTextColor(Color.RED);
                   }else  timer.setTextColor(Color.BLACK);
               }

                public void onFinish() {
                    gameOver =false;
                    gameOver();
                }

            }.start();
        }
        else if(level ==3){
            addNewSpot(R.drawable.twentyone,21);
            addNewSpot(R.drawable.twentytwo,22);
            addNewSpot(R.drawable.twentythree,23);
            addNewSpot(R.drawable.twentyfour,24);
            addNewSpot(R.drawable.twentyfive,25);
            addNewSpot(R.drawable.twentysix,26);
            addNewSpot(R.drawable.twentyseven,27);
            addNewSpot(R.drawable.twentyeight,28);
            addNewSpot(R.drawable.twentynine,29);
            addNewSpot(R.drawable.thirty,30);
           levelThree = new CountDownTimer(50000, 1000) {

               public void onTick(long millisUntilFinished) {
                   timer.setText(resources.getString(R.string.remainingTime) +" "+ millisUntilFinished / 1000);
                   if(millisUntilFinished / 1000 <10){
                       timer.setTextColor(Color.RED);
                   }else  timer.setTextColor(Color.BLACK);
               }

                public void onFinish() {
                    gameOver =false;
                    gameOver();
                }

            }.start();
        }
    }

    private void levelFour(){
        int count =0;
        while(count<10) {
            int i = random.nextInt(30);
            i+=1;
            if(numbers.contains(i)){
                continue;
            }
            switch (i){

                case 1:
                {
                    addNewSpot(R.drawable.two, 1);
                    numbers.add(i);
                    count++;
                    break;
                }
                case 2:
                {
                    addNewSpot(R.drawable.two, 2);
                    numbers.add(i);
                    count++;
                    break;
                }case 3:
                {
                    addNewSpot(R.drawable.three, 3);
                    numbers.add(i);
                    count++;
                    break;
                }case 4:
                {
                    addNewSpot(R.drawable.four, 4);
                    numbers.add(i);
                    count++;
                    break;
                }case 5:
                {
                    addNewSpot(R.drawable.five, 5);
                    numbers.add(i);
                    count++;
                    break;
                }case 6:
                {
                    addNewSpot(R.drawable.six, 6);
                    numbers.add(i);
                    count++;
                    break;
                }case 7:
                {
                    addNewSpot(R.drawable.seven, 7);
                    numbers.add(i);
                    count++;
                    break;
                }case 8:
                {
                    addNewSpot(R.drawable.eight, 8);
                    numbers.add(i);
                    count++;
                    break;
                }
                case 9:
                {
                    addNewSpot(R.drawable.nine, 9);
                    numbers.add(i);
                    count++;
                    break;
                }
                case 10:
                {
                    addNewSpot(R.drawable.ten, 10);
                    numbers.add(i);
                    count++;
                    break;
                }
                case 11:
                {
                    addNewSpot(R.drawable.eleven, 11);
                    numbers.add(i);
                    count++;
                    break;
                }
                case 12:
                {
                    addNewSpot(R.drawable.twelve, 12);

                    numbers.add(i);
                    count++;
                    break;
                }case 13:
                {
                    addNewSpot(R.drawable.thirteen, 13);
                    numbers.add(i);
                    count++;
                    break;
                }case 14:
                {
                    addNewSpot(R.drawable.fourteen, 14);

                    numbers.add(i);
                    count++;
                    break;
                }case 15:
                {
                    addNewSpot(R.drawable.fifteen, 15);
                    numbers.add(i);
                    count++;
                    break;
                }case 16:
                {
                    addNewSpot(R.drawable.sixteen, 16);
                    numbers.add(i);
                    count++;
                    break;
                }case 17:
                {
                    addNewSpot(R.drawable.seventeen, 17);
                    numbers.add(i);
                    count++;
                    break;
                }case 18:
                {
                    addNewSpot(R.drawable.eighteen, 18);
                    numbers.add(i);
                    count++;
                    break;
                }case 19:
                {
                    addNewSpot(R.drawable.nineteen, 19);
                    numbers.add(i);
                    count++;
                    break;
                }case 20:
                {
                    addNewSpot(R.drawable.twenty, 20);

                    numbers.add(i);
                    count++;
                    break;
                }case 21:
                {
                    addNewSpot(R.drawable.twentyone, 21);
                    numbers.add(i);
                    count++;
                    break;
                }case 22:
                {
                    addNewSpot(R.drawable.twentytwo, 22);
                    numbers.add(i);
                    count++;
                    break;
                }case 23:
                {
                    addNewSpot(R.drawable.twentythree, 23);
                    numbers.add(i);
                    count++;
                    break;
                }case 24:
                {
                    addNewSpot(R.drawable.twentyfour, 24);
                    numbers.add(i);
                    count++;
                    break;
                }case 25:
                {
                    addNewSpot(R.drawable.twentyfive, 25);
                    numbers.add(i);
                    count++;
                    break;
                }case 26:
                {
                    addNewSpot(R.drawable.twentysix, 26);
                    numbers.add(i);
                    count++;
                    break;
                }case 27:
                {
                    addNewSpot(R.drawable.twentyseven, 27);
                    numbers.add(i);
                    count++;
                    break;
                }case 28:
                {
                    addNewSpot(R.drawable.twentyeight, 28);
                    numbers.add(i);
                    count++;
                    break;
                }case 29:
                {
                    addNewSpot(R.drawable.twentynine, 29);
                    numbers.add(i);
                    count++;
                    break;
                }case 30:
                {
                    addNewSpot(R.drawable.thirty, 30);
                    numbers.add(i);
                    count++;
                    break;
                }


            }
        }
        Collections.sort(numbers);
        levelFour =new CountDownTimer(45000, 1000) {

            public void onTick(long millisUntilFinished) {
                timer.setText(resources.getString(R.string.remainingTime) +" "+ millisUntilFinished / 1000);
                if(millisUntilFinished / 1000 <10){
                    timer.setTextColor(Color.RED);
                }else  timer.setTextColor(Color.BLACK);
            }

            public void onFinish() {
                gameOver =false;
                gameOver();
            }

        }.start();

    }
} // end class SpotOnView



