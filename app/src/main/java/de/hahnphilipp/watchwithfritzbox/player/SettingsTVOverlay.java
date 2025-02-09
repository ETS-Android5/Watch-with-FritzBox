package de.hahnphilipp.watchwithfritzbox.player;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.BrowseFrameLayout;
import androidx.leanback.widget.GuidedAction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.videolan.libvlc.MediaPlayer;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.DialogFragment;
import de.hahnphilipp.watchwithfritzbox.utils.DialogFragmentCallback;
import de.hahnphilipp.watchwithfritzbox.utils.TVSetting;

public class SettingsTVOverlay extends Fragment {

    public TVPlayerActivity context;
    public boolean isShown = false;

    TVSettingsOverlayRecyclerAdapter tvOverlayRecyclerAdapter;
    RecyclerView recyclerView;

    Timer t;
    Fragment openedFragment = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.settingstvoverlay, container, false);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.tvoverlayrecycler);


        ArrayList<TVSetting> tvSettings = new ArrayList<TVSetting>();

        tvOverlayRecyclerAdapter = new TVSettingsOverlayRecyclerAdapter(getContext(), tvSettings);
        final LinearLayoutManager llm = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(tvOverlayRecyclerAdapter);
        updateTVSettings();


        BrowseFrameLayout browseFrameLayout = view.findViewById(R.id.tvoverlayrecyclerBrowse);
        browseFrameLayout.setOnFocusSearchListener(new BrowseFrameLayout.OnFocusSearchListener() {
            @Override
            public View onFocusSearch(View focused, int direction) {
                if (recyclerView.hasFocus())
                    return focused; // keep focus on recyclerview! DO NOT return recyclerview, but focused, which is a child of the recyclerview
                else
                    return null; // someone else will find the next focus
            }
        });

        hideOverlays();
    }


    public void updateTVSettings(){
        ArrayList<TVSetting> tvSettings = new ArrayList<TVSetting>();

        if(context == null)
            return;

        final MediaPlayer player = context.mMediaPlayer;
        MediaPlayer.TrackDescription[] descriptionsAudio = new MediaPlayer.TrackDescription[0];
        MediaPlayer.TrackDescription[] descriptionsSubtitle = new MediaPlayer.TrackDescription[0];

        if(player != null){
            descriptionsAudio = player.getAudioTracks();
            descriptionsSubtitle = player.getSpuTracks();
        }

        if(!ChannelUtils.getHbbTvFromChannel(context,ChannelUtils.getLastSelectedChannel(context)).isEmpty()) {
            tvSettings.add(new TVSetting(getString(R.string.settings_open_hbbtv), R.drawable.ic_remote_tv, new Runnable() {
                @Override
                public void run() {
                    showHbbTV();
                }
            }, true));
        }

        if (descriptionsAudio != null && descriptionsAudio.length != 0) {
            tvSettings.add(new TVSetting(getString(R.string.audio_tracks), R.drawable.ic_audiotrack_24, new Runnable() {
                @Override
                public void run() {
                    showAudioTrackSelection();
                }
            }, true));
        }

        if (descriptionsSubtitle != null && descriptionsSubtitle.length != 0) {
            tvSettings.add(new TVSetting(getString(R.string.subtitles), R.drawable.ic_baseline_closed_caption_24, new Runnable() {
                @Override
                public void run() {
                    showSubtitleTrackSelection();
                }
            }, true));
        }

        if (ChannelUtils.getChannelByNumber(context, ChannelUtils.getLastSelectedChannel(context)).type != ChannelUtils.ChannelType.RADIO) {
            tvSettings.add(new TVSetting(getString(R.string.video_aspect), R.drawable.ic_baseline_video_settings_24, new Runnable() {
                @Override
                public void run() {
                    showVideoFormatSelection();
                }
            }, true));
        }

        tvSettings.add(new TVSetting(getString(R.string.settings_reorder_channels), R.drawable.ic_baseline_reorder_24, new Runnable() {
            @Override
            public void run() {
                showChannelEditor();
            }
        }, false));

        if(tvOverlayRecyclerAdapter != null) {
            tvOverlayRecyclerAdapter.objects = tvSettings;
            tvOverlayRecyclerAdapter.notifyDataSetChanged();
        }
    }

    public void showChannelEditor(){
        EditChannelListTVOverlay editChannelListTVOverlay = new EditChannelListTVOverlay();
        openedFragment = editChannelListTVOverlay;

        getActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.overlayChannels, editChannelListTVOverlay)
                .addToBackStack("selectionTVOverlay")
                .commit();
    }

    public void showHbbTV(){
        ArrayList<ChannelUtils.HbbTV> hbbTVList = ChannelUtils.getHbbTvFromChannel(context,ChannelUtils.getLastSelectedChannel(context));

        if(!hbbTVList.isEmpty()) {
            if(hbbTVList.size() == 1){
                HbbTVOverlay hbbTVOverlay = new HbbTVOverlay();
                openedFragment = hbbTVOverlay;

                hbbTVOverlay.hbbTvUrl = ChannelUtils.getHbbTvFromChannel(context, ChannelUtils.getLastSelectedChannel(context)).get(0).url;

                getActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.overlayChannels, hbbTVOverlay)
                        .addToBackStack("selectionTVOverlay")
                        .commit();

                context.mMediaPlayer.pause();
            }else {
                SelectionTVOverlay selectionTVOverlay = new SelectionTVOverlay();
                openedFragment = selectionTVOverlay;
                selectionTVOverlay.title = getString(R.string.settings_open_hbbtv_multi_title);
                for (ChannelUtils.HbbTV hbbTV : hbbTVList) {
                    selectionTVOverlay.tvSettings.add(new TVSetting(hbbTV.title, R.drawable.ic_remote_tv, new Runnable() {
                        @Override
                        public void run() {
                            getActivity().getSupportFragmentManager().beginTransaction().remove(openedFragment).commit();
                            HbbTVOverlay hbbTVOverlay = new HbbTVOverlay();
                            openedFragment = hbbTVOverlay;

                            hbbTVOverlay.hbbTvUrl = hbbTV.url;

                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .add(R.id.overlayChannels, hbbTVOverlay)
                                    .addToBackStack("selectionTVOverlay")
                                    .commit();

                            context.mMediaPlayer.pause();
                        }
                    },true));
                }
                getActivity().getSupportFragmentManager().beginTransaction()
                        .add(R.id.overlayChannels, selectionTVOverlay)
                        .addToBackStack("selectionTVOverlay")
                        .commit();
            }
        }
    }

    public void showVideoFormatSelection(){
        SelectionTVOverlay selectionTVOverlay = new SelectionTVOverlay();
        openedFragment = selectionTVOverlay;
        final MediaPlayer player = context.mMediaPlayer;
        selectionTVOverlay.title = getString(R.string.video_aspect);
        String[] aspect_ratios = {"16:9", "4:3", "21:9", "16:10"};
        //this should actually never be true, but just to be sure we do it anyways
        for (String aspect : aspect_ratios) {
            selectionTVOverlay.tvSettings.add(new TVSetting(aspect, R.drawable.ic_baseline_video_settings_24, new Runnable() {
                @Override
                public void run() {
                    player.setAspectRatio(aspect);
                    if(selectionTVOverlay != null)
                        getActivity().getSupportFragmentManager().beginTransaction().remove(selectionTVOverlay).commit();
                    openedFragment = null;
                }
            },true));
        }

        getActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.overlayChannels, selectionTVOverlay)
                .addToBackStack("selectionTVOverlay")
                .commit();
    }

    public void showSubtitleTrackSelection() {
        SelectionTVOverlay selectionTVOverlay = new SelectionTVOverlay();
        openedFragment = selectionTVOverlay;
        final MediaPlayer player = context.mMediaPlayer;
        selectionTVOverlay.title = getString(R.string.subtitles);
        MediaPlayer.TrackDescription[] descriptions = player.getSpuTracks();
        //this should actually never be true, but just to be sure we do it anyways
        if (descriptions == null || descriptions.length == 0) {
            Toast.makeText(getContext(), R.string.no_subtitle_tracks, Toast.LENGTH_SHORT).show();
            return;
        }
        for (final MediaPlayer.TrackDescription description : descriptions) {
            selectionTVOverlay.tvSettings.add(new TVSetting(description.name, R.drawable.ic_baseline_closed_caption_24, new Runnable() {
                @Override
                public void run() {
                    player.setAudioTrack((int)description.id);
                    if(selectionTVOverlay != null)
                        getActivity().getSupportFragmentManager().beginTransaction().remove(selectionTVOverlay).commit();
                    openedFragment = null;
                }
            }, true));
        }

        getActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.overlayChannels, selectionTVOverlay)
                .addToBackStack("selectionTVOverlay")
                .commit();
    }

    public void showAudioTrackSelection() {
        SelectionTVOverlay selectionTVOverlay = new SelectionTVOverlay();
        openedFragment = selectionTVOverlay;
        final MediaPlayer player = context.mMediaPlayer;
        selectionTVOverlay.title = getString(R.string.audio_tracks);
        MediaPlayer.TrackDescription[] descriptions = player.getAudioTracks();
        //this should actually never be true, but just to be sure we do it anyways
        if (descriptions == null || descriptions.length == 0) {
            Toast.makeText(getContext(), R.string.no_audio_tracks, Toast.LENGTH_SHORT).show();
            return;
        }
        for (final MediaPlayer.TrackDescription description : descriptions) {
            selectionTVOverlay.tvSettings.add(new TVSetting(description.name, R.drawable.ic_audiotrack_24, new Runnable() {
                @Override
                public void run() {
                    player.setAudioTrack((int)description.id);
                    if(selectionTVOverlay != null)
                        getActivity().getSupportFragmentManager().beginTransaction().remove(selectionTVOverlay).commit();
                    openedFragment = null;
                }
            },true));
        }

        getActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.overlayChannels, selectionTVOverlay)
                .addToBackStack("selectionTVOverlay")
                .commit();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(isShown){
            if(event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_BACK ||keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    if(openedFragment!= null){
                        if(openedFragment instanceof HbbTVOverlay) {
                            if(((HbbTVOverlay) openedFragment).webView.canGoBack()) {
                                ((HbbTVOverlay) openedFragment).webView.goBack();
                                return true;
                            }else{
                                context.mMediaPlayer.play();
                            }
                        }else if(openedFragment instanceof EditChannelListTVOverlay) {
                            context.mChannelOverlayFragment.tvOverlayRecyclerAdapter.objects = ChannelUtils.getAllChannels(context);
                        }
                        getActivity().getSupportFragmentManager().beginTransaction().remove(openedFragment).commit();
                        openedFragment = null;

                    }else{
                            hideOverlays();
                    }
                    return true;
                }
            }
        }
    return false;

    }


    public void showOverlays(){
        isShown = true;
        getView().setVisibility(View.VISIBLE);
    }

    public void hideOverlays(){
        isShown = false;
        getView().setVisibility(View.GONE);
    }

}
