package cn.il0ve.rosen.rosen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    //显示界面中的控件
    private TextView textViewSongName;
    private TextView textViewLyric;
    private TextView textViewDuration;
    private Button buttonPlay;
    private Button buttonPause;
    private Button buttonPrevious;
    private Button buttonNext;
    private SeekBar seekBar;
    //service组件的跳转intent
    private Intent intent;

    //接受播放音乐service的广播
    private PlayMusicBroadcastReiver playMusicReiver;
    private IntentFilter intentFilter;


    //打印日志标识所在activity的常量
    private static final String TAG = "MainActivity";

    //处理自动更新seekbar播放进度
    private Handler handler;
    private int progress = 0;
    private boolean ProgressIsChangeOrNot = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //获得控件
        textViewSongName = (TextView) findViewById(R.id.playing_song_name);
        textViewLyric = (TextView) findViewById(R.id.playing_lyric);
        textViewDuration = (TextView) findViewById(R.id.playing_duration);
        buttonPlay = (Button) findViewById(R.id.playing_button_play);
        buttonPause = (Button) findViewById(R.id.playing_button_pause);
        buttonPrevious = (Button) findViewById(R.id.playing_button_previous);
        buttonNext = (Button) findViewById(R.id.playing_button_next);
        seekBar = (SeekBar) findViewById(R.id.playing_seekBar);

        //添加控件的触发事件
        buttonPlay.setOnClickListener(new onPlayButtonClickListener());
        buttonPause.setOnClickListener(new onPauseButtonClickListener());
        seekBar.setOnSeekBarChangeListener(new onMusicSeekBarChangeListener());

        //注册广播接收，接收service发送的播放音乐广播，包括音乐时长
        playMusicReiver = new PlayMusicBroadcastReiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction("playMusic");
        registerReceiver(playMusicReiver, intentFilter);
    }

    //接收播放音乐的广播
    public class PlayMusicBroadcastReiver extends BroadcastReceiver {
        //播放音乐的时长
        int musicDuring = 0;

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.v(TAG, "onReceive in PlayMusicBroadcastReiver");
//            Toast.makeText(MainActivity.this, "recive music time", Toast.LENGTH_SHORT).show();

            String action = intent.getAction();
            if (action.equals("playMusic")) {
                musicDuring = intent.getIntExtra("during", 0);
                textViewDuration.setText("0/" + duringParse(musicDuring));
                seekBar.setMax(musicDuring);
                //自动更新播放音乐的进度条
               changeSeekBarProgress();
            }
        }

        //自动更新seekbar播放进度
        public void changeSeekBarProgress() {
            //接收消息，处理handler-thread的消息
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    //如果正在播放音乐，自动更新seekbar进度条
                    if (msg.what == 100 && ProgressIsChangeOrNot == true) {
                        //更新seekbar进度条
                        seekBar.setProgress(msg.arg1);
                        //更新已经播放音乐的时间值
                        textViewDuration.setText(duringParse(msg.arg1) + "/" + duringParse(musicDuring));
                    }
                }
            };
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    //每隔100ms发送消息
                    while (progress < musicDuring) {
                        try {
                            Thread.sleep(100);
                            if (ProgressIsChangeOrNot == true)
                                progress += 100;
                            Log.d("in handler-thread", "current progress is " + progress+"ms");
                            Message msg = Message.obtain();
                            msg.arg1 = progress;
                            msg.what = 100;
                            handler.sendMessage(msg);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    //停止更新seekbar进度
                    ProgressIsChangeOrNot = false;
                }
            }.start();
        }
    }

    //时间格式转换
    public String duringParse(int during) {
        String time = "";
        SimpleDateFormat dateFormat = new SimpleDateFormat("mm:ss", Locale.CHINA);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        time = dateFormat.format(during);
        return time;
    }

    public class onMusicSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            //暂停音乐
//            intent = new Intent(MainActivity.this, PlayMusicService.class);
//            Bundle bundle = new Bundle();
//            bundle.putInt("option", PlayMusicService.PLAY_MUSIC_PAUSE);
//            intent.putExtras(bundle);
//            startService(intent);
            //停止更新seekbar进度
//            ProgressIsChangeOrNot = false;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            //获取播放时间点
            progress = seekBar.getProgress();

            //播放音乐
            intent = new Intent(MainActivity.this, PlayMusicService.class);
            Bundle bundle = new Bundle();
            bundle.putInt("option", PlayMusicService.PLAY_MUSIC_SEEK_PLAY);//播放
            bundle.putInt("progress", progress);
            intent.putExtras(bundle);
            startService(intent);

            //显示歌词
            displayLyric();

            //自动更新进度条
            ProgressIsChangeOrNot = true;
        }
    }

    public class onPauseButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            //暂停音乐
            intent = new Intent(MainActivity.this, PlayMusicService.class);
            Bundle bundle = new Bundle();
            bundle.putInt("option", PlayMusicService.PLAY_MUSIC_PAUSE);//播放
            intent.putExtras(bundle);
            startService(intent);

            //停止更新seekbar进度
            ProgressIsChangeOrNot = false;
        }
    }

    public class onPlayButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            //播放音乐
            intent = new Intent(MainActivity.this, PlayMusicService.class);
            Bundle bundle = new Bundle();
            bundle.putInt("option", PlayMusicService.PLAY_MUSIC_PLAY);//播放
            intent.putExtras(bundle);
            startService(intent);

            //显示歌词
            displayLyric();

            //自动更新进度条
            ProgressIsChangeOrNot = true;
        }
    }

    //显示歌词
    public void displayLyric() {
        textViewSongName.setText("好人一生平安");
        textViewLyric.setText("好人一生平安\n" +
                "\n" +
                "歌手:李娜\n" +
                "\n" +
                "有过多少往事\n" +
                "\n" +
                "仿佛就在昨天\n" +
                "\n" +
                "有过多少朋友\n" +
                "\n" +
                "仿佛还在身边\n" +
                "\n" +
                "也曾心意沉沉\n" +
                "\n" +
                "相逢是苦是甜?\n" +
                "\n" +
                "如今举杯祝愿\n" +
                "\n" +
                "好人一生平安\n" +
                "\n" +
                "谁能与我同醉\n" +
                "\n" +
                "相知年年岁岁\n" +
                "\n" +
                "咫尺天涯皆有缘\n" +
                "\n" +
                "此情温暖人间\n" +
                "\n" +
                "谁能与我同醉\n" +
                "\n" +
                "相知年年岁岁\n" +
                "\n" +
                "咫尺天涯皆有缘\n" +
                "\n" +
                "此情温暖人间\n" +
                "\n" +
                "也曾心意沉沉\n" +
                "\n" +
                "相逢是苦是甜?\n" +
                "\n" +
                "如今举杯祝愿\n" +
                "\n" +
                "好人一生平安\n" +
                "\n" +
                "谁能与我同醉\n" +
                "\n" +
                "相知年年岁岁\n" +
                "\n" +
                "咫尺天涯皆有缘\n" +
                "\n" +
                "此情温暖人间\n" +
                "\n" +
                "谁能与我同醉\n" +
                "\n" +
                "相知年年岁岁\n" +
                "\n" +
                "咫尺天涯皆有缘\n" +
                "\n" +
                "此情温暖人间");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //回收service组件
        if (intent != null)
            stopService(intent);
    }
}
