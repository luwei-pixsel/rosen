package cn.il0ve.rosen.rosen;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class PlayMusicService extends Service {
    //播放音乐的媒体播放器
    private MediaPlayer mediaPlayer;
    //播放、暂停等的标识常量
    public static final int PLAY_MUSIC_PLAY = 1;
    public static final int PLAY_MUSIC_SEEK_PLAY = 2;
    public static final int PLAY_MUSIC_PAUSE = -1;
    //打印日志中显示所在service的标识常量
    private static final String TAG = "PlayMusicService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
        Toast.makeText(this, "show media player", Toast.LENGTH_SHORT).show();

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.audio);
            mediaPlayer.setLooping(false);
            sendPlayMusicBroadcast();
        }
    }

    //发送开始播放音乐的广播，传递音乐时长
    public void sendPlayMusicBroadcast() {
        Intent intent = new Intent("playMusic");
        intent.putExtra("during", mediaPlayer.getDuration());
        sendBroadcast(intent);
    }

    @Override
//    播放音乐
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.v(TAG, "onStart");

        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                int option = bundle.getInt("option");

                switch (option) {
                    case PLAY_MUSIC_PLAY:
                        play();
                        break;
                    case PLAY_MUSIC_SEEK_PLAY:
                        int progress = bundle.getInt("progress");
                        seekPlay(progress);
                        break;
                    case PLAY_MUSIC_PAUSE:
                        pause();
                        break;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void seekPlay(int progress) {
        if (progress != 0) {
            //定位播放音乐
            mediaPlayer.seekTo(progress);
            mediaPlayer.start();
        }

    }

    public void play() {
        if (!mediaPlayer.isPlaying())
            mediaPlayer.start();
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            mediaPlayer.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    public PlayMusicService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
