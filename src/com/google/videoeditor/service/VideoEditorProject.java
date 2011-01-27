/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.videoeditor.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.media.videoeditor.MediaProperties;
import android.media.videoeditor.MediaVideoItem;
import android.media.videoeditor.VideoEditor;
import android.media.videoeditor.VideoEditor.PreviewProgressListener;
import android.net.Uri;
import android.util.Xml;
import android.view.SurfaceHolder;


/**
 * The  video editor project encapsulates the video editor and the project metadata
 */
public class VideoEditorProject {
    // The name of the metadata file
    private final static String PROJECT_METADATA_FILENAME = "metadata.xml";

    public static final int DEFAULT_ZOOM_LEVEL = 20;

    // XML definitions
    private static final String TAG_PROJECT = "project";
    private static final String TAG_MOVIE = "movie";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_URI = "uri";
    private static final String ATTR_SAVED = "saved";
    private static final String ATTR_THEME = "theme";
    private static final String ATTR_PLAYHEAD_POSITION = "playhead";
    private static final String ATTR_DURATION = "duration";
    private static final String ATTR_ZOOM_LEVEL = "zoom_level";

    // Instance variables
    private final VideoEditor mVideoEditor;
    private final String mProjectPath;
    private final long mProjectDurationMs;
    private String mProjectName;
    private long mLastSaved;
    private Uri mExportedMovieUri;
    private int mAspectRatio;
    private String mTheme;
    private long mPlayheadPosMs;
    private int mZoomLevel;
    private List<MovieMediaItem> mMediaItems = new ArrayList<MovieMediaItem>();
    private List<MovieAudioTrack> mAudioTracks = new ArrayList<MovieAudioTrack>();

    /**
     * Constructor
     *
     * @param videoEditor The video editor. Note that this can be null when
     *  we create the project for the purpose of displaying a project preview.
     * @param projectPath The project path
     * @param projectName The project name
     * @param lastSaved Time when project was last saved
     * @param playheadPosMs The playhead position
     * @param durationMs The project duration
     * @param zoomLevel The zoom level
     * @param exportedMovieUri The exported movie URI
     * @param theme The project theme
     */
    VideoEditorProject(VideoEditor videoEditor, String projectPath, String projectName,
            long lastSaved, long playheadPosMs, long durationMs, int zoomLevel,
            Uri exportedMovieUri, String theme) {
        mVideoEditor = videoEditor;
        if (videoEditor != null) {
            mAspectRatio = videoEditor.getAspectRatio();
        }

        mProjectPath = projectPath;
        mProjectName = projectName;
        mLastSaved = lastSaved;
        mPlayheadPosMs = playheadPosMs;
        mProjectDurationMs = durationMs;
        mZoomLevel = zoomLevel;
        mExportedMovieUri = exportedMovieUri;
        mTheme = theme;
    }

    /**
     * @return The project path
     */
    public String getPath() {
        return mProjectPath;
    }

    /**
     * @param projectName The project name
     */
    public void setProjectName(String projectName) {
        mProjectName = projectName;
    }

    /**
     * @return The project name
     */
    public String getName() {
        return mProjectName;
    }

    /**
     * @return Time when time was last saved
     */
    public long getLastSaved() {
        return mLastSaved;
    }

    /**
     * @return The project duration.
     *
     * Note: This method should only be called to retrieve the project duration
     * as saved on disk. Once a project is opened call computeDuration() to get
     * the current duration.
     */
    public long getProjectDuration() {
        return mProjectDurationMs;
    }

    /**
     * @return The zoom level
     */
    public int getZoomLevel() {
        return mZoomLevel;
    }

    /**
     * @param zoomLevel The zoom level
     */
    public void setZoomLevel(int zoomLevel) {
        mZoomLevel = zoomLevel;
    }

    /**
     * @return The aspect ratio
     */
    public int getAspectRatio() {
        return mAspectRatio;
    }

    /**
     * @return The playhead position
     */
    public long getPlayheadPos() {
        return mPlayheadPosMs;
    }

    /**
     * @param playheadPosMs The playhead position
     */
    public void setPlayheadPos(long playheadPosMs) {
        mPlayheadPosMs = playheadPosMs;
    }

    /**
     * @param aspectRatio The aspect ratio
     */
    void setAspectRatio(int aspectRatio) {
        mAspectRatio = aspectRatio;
    }

    /**
     * Add the URI of an exported movie
     *
     * @param uri The movie URI
     */
    void addExportedMovieUri(Uri uri) {
        mExportedMovieUri = uri;
    }

    /**
     * @return The exported movie URI
     */
    public Uri getExportedMovieUri() {
        return mExportedMovieUri;
    }

    /**
     * @param theme The theme
     */
    void setTheme(String theme) {
        mTheme = theme;
    }

    /**
     * @return The theme
     */
    public String getTheme() {
        return mTheme;
    }

    /**
     * Set the media items
     *
     * @param mediaItems The media items
     */
    void setMediaItems(List<MovieMediaItem> mediaItems) {
        mMediaItems = mediaItems;
    }

    /**
     * Add a media item at the end of the list
     *
     * @param mediaItem The media item
     */
    void addMediaItem(MovieMediaItem mediaItem) {
        final int count = mMediaItems.size();
        if (count > 0) {
            // Replace the end transition of the last item in the list
            mMediaItems.get(count - 1).setEndTransition(mediaItem.getBeginTransition());
        }

        mMediaItems.add(mediaItem);
    }

    /**
     * Insert a media item after the specified media item id
     *
     * @param mediaItem The media item
     * @param afterMediaItemId Insert after this media item id
     */
    void insertMediaItem(MovieMediaItem mediaItem, String afterMediaItemId) {
        if (afterMediaItemId == null) {
            if (mMediaItems.size() > 0) {
                // Invalidate the transition at the beginning of the timeline
                final MovieMediaItem firstMediaItem = mMediaItems.get(0);
                if (firstMediaItem.getBeginTransition() != null) {
                    firstMediaItem.setBeginTransition(null);
                }
            }

            mMediaItems.add(0, mediaItem);
        } else {
            final int mediaItemCount = mMediaItems.size();
            for (int i = 0; i < mediaItemCount; i++) {
                final MovieMediaItem mi = mMediaItems.get(i);
                if (mi.getId().equals(afterMediaItemId)) {
                    // Invalidate the transition at the end of this media item
                    mi.setEndTransition(null);
                    // Invalidate the reference in the next media item (if any)
                    if (i < mediaItemCount - 1) {
                        mMediaItems.get(i + 1).setBeginTransition(null);
                    }

                    // Insert the new media item
                    mMediaItems.add(i + 1, mediaItem);
                    return;
                }
            }

            throw new IllegalArgumentException("MediaItem not found: " + afterMediaItemId);
        }
    }

    /**
     * Update the specified media item
     *
     * @param newMediaItem The media item can be a new instance of the media
     *      item or an updated version of the same instance.
     */
    void updateMediaItem(MovieMediaItem newMediaItem) {
        final String newMediaItemId = newMediaItem.getId();
        final int count = mMediaItems.size();
        for (int i = 0; i < count; i++) {
            final MovieMediaItem mediaItem = mMediaItems.get(i);
            if (mediaItem.getId().equals(newMediaItemId)) {
                mMediaItems.set(i, newMediaItem);
                // Update the transitions of the previous and next item
                if (i > 0) {
                    final MovieMediaItem prevMediaItem = mMediaItems.get(i - 1);
                    prevMediaItem.setEndTransition(newMediaItem.getBeginTransition());
                }

                if (i < count - 1) {
                    final MovieMediaItem nextMediaItem = mMediaItems.get(i + 1);
                    nextMediaItem.setBeginTransition(newMediaItem.getEndTransition());
                }
                break;
            }
        }
    }

    /**
     * Remove the specified media item
     *
     * @param mediaItemId The media item id
     * @param transition The transition to be set between at the delete
     *      position
     */
    void removeMediaItem(String mediaItemId, MovieTransition transition) {
        String prevMediaItemId = null;
        final int count = mMediaItems.size();
        for (int i = 0; i < count; i++) {
            final MovieMediaItem mediaItem = mMediaItems.get(i);
            if (mediaItem.getId().equals(mediaItemId)) {
                mMediaItems.remove(i);
                if (transition != null) {
                    addTransition(transition, prevMediaItemId);
                } else {
                    if (i > 0) {
                        final MovieMediaItem prevMediaItem = mMediaItems.get(i - 1);
                        prevMediaItem.setEndTransition(null);
                    }

                    if (i < count - 1) {
                        final MovieMediaItem nextMediaItem = mMediaItems.get(i);
                        nextMediaItem.setBeginTransition(null);
                    }
                }
                break;
            }

            prevMediaItemId = mediaItem.getId();
        }
    }

    /**
     * @return The media items list
     */
    public List<MovieMediaItem> getMediaItems() {
        return mMediaItems;
    }

    /**
     * @return The media item count
     */
    public int getMediaItemCount() {
        return mMediaItems.size();
    }

    /**
     * @param mediaItemId The media item id
     *
     * @return The media item
     */
    public MovieMediaItem getMediaItem(String mediaItemId) {
        for (MovieMediaItem mediaItem : mMediaItems) {
            if (mediaItem.getId().equals(mediaItemId)) {
                return mediaItem;
            }
        }

        return null;
    }

    /**
     * @return The first media item
     */
    public MovieMediaItem getFirstMediaItem() {
        if (mMediaItems.size() == 0) {
            return null;
        } else {
            return mMediaItems.get(0);
        }
    }

    /**
     * Check if the specified media item id is the first media item
     *
     * @param mediaItemId The media item id
     *
     * @return true if this is the first media item
     */
    public boolean isFirstMediaItem(String mediaItemId) {
        final MovieMediaItem mediaItem = getFirstMediaItem();
        if (mediaItem == null) {
            return false;
        } else {
            return mediaItem.getId().equals(mediaItemId);
        }
    }

    /**
     * @return The last media item
     */
    public MovieMediaItem getLastMediaItem() {
        final int count = mMediaItems.size();
        if (count == 0) {
            return null;
        } else {
            return mMediaItems.get(count - 1);
        }
    }

    /**
     * Check if the specified media item id is the last media item
     *
     * @param mediaItemId The media item id
     *
     * @return true if this is the last media item
     */
    public boolean isLastMediaItem(String mediaItemId) {
        final MovieMediaItem mediaItem = getLastMediaItem();
        if (mediaItem == null) {
            return false;
        } else {
            return mediaItem.getId().equals(mediaItemId);
        }
    }

    /**
     * Find the previous media item with the specified id
     *
     * @param mediaItemId The media item id
     * @return The previous media item
     */
    public MovieMediaItem getPreviousMediaItem(String mediaItemId) {
        MovieMediaItem prevMediaItem = null;
        for (MovieMediaItem mediaItem : mMediaItems) {
            if (mediaItemId.equals(mediaItem.getId())) {
                break;
            } else {
                prevMediaItem = mediaItem;
            }
        }

        return prevMediaItem;
    }

    /**
     * Find the next media item with the specified id
     *
     * @param mediaItemId The media item id
     * @return The next media item
     */
    public MovieMediaItem getNextMediaItem(String mediaItemId) {
        boolean getNext = false;
        final int count = mMediaItems.size();
        for (int i = 0; i < count; i++) {
            final MovieMediaItem mi = mMediaItems.get(i);
            if (getNext) {
                return mi;
            } else {
                if (mediaItemId.equals(mi.getId())) {
                    getNext = true;
                }
            }
        }

        return null;
    }

    /**
     * Get the previous media item
     *
     * @param positionMs The current position in ms
     * @return The previous media item
     */
    public MovieMediaItem getPreviousMediaItem(long positionMs) {
        long startTimeMs = 0;
        MovieMediaItem prevMediaItem = null;
        for (MovieMediaItem mediaItem : mMediaItems) {
            if (positionMs == startTimeMs) {
                break;
            } else if (positionMs > startTimeMs
                    && positionMs < startTimeMs + mediaItem.getAppTimelineDuration()) {
                return mediaItem;
            } else {
                prevMediaItem = mediaItem;
            }

            startTimeMs += mediaItem.getAppTimelineDuration();
            if (mediaItem.getEndTransition() != null) {
                startTimeMs -= mediaItem.getEndTransition().getAppDuration();
            }
        }

        return prevMediaItem;
    }

    /**
     * Get the next media item
     *
     * @param positionMs The current position in ms
     * @return The next media item
     */
    public MovieMediaItem getNextMediaItem(long positionMs) {
        long startTimeMs = 0;
        final int count = mMediaItems.size();
        for (int i = 0; i < count; i++) {
            final MovieMediaItem mediaItem = mMediaItems.get(i);
            if (positionMs >= startTimeMs
                    && positionMs < startTimeMs + mediaItem.getAppTimelineDuration() -
                    getEndTransitionDuration(mediaItem)) {
                if (i < count - 1) {
                    return mMediaItems.get(i + 1);
                } else {
                    return null;
                }
            } else if (positionMs >= startTimeMs
                    && positionMs < startTimeMs + mediaItem.getAppTimelineDuration()) {
                if (i < count - 2) {
                    return mMediaItems.get(i + 2);
                } else {
                    return null;
                }
            } else {
                startTimeMs += mediaItem.getAppTimelineDuration();
                startTimeMs -= getEndTransitionDuration(mediaItem);
            }
        }

        return null;
    }

    /**
     * Get the beginning media item of the specified transition
     *
     * @param transition The transition
     *
     * @return The media item
     */
    public MovieMediaItem getPreviousMediaItem(MovieTransition transition) {
        final int count = mMediaItems.size();
        for (int i = 0; i < count; i++) {
            final MovieMediaItem mediaItem = mMediaItems.get(i);
            if (i == 0) {
                if (mediaItem.getBeginTransition() == transition) {
                    return null;
                }
            }

            if (mediaItem.getEndTransition() == transition) {
                return mediaItem;
            }
        }

        return null;
    }

    /**
     * Return the end transition duration
     *
     * @param mediaItem The media item
     * @return the end transition duration
     */
    private static long getEndTransitionDuration(MovieMediaItem mediaItem) {
        if (mediaItem.getEndTransition() != null) {
            return mediaItem.getEndTransition().getAppDuration();
        } else {
            return 0;
        }
    }

    /**
     * Determine the media item after which a new media item will be inserted.
     *
     * @param timeMs The inquiry position
     *
     * @return The media item after which the insertion will be performed
     */
    public MovieMediaItem getInsertAfterMediaItem(long timeMs) {
        long beginMs = 0;
        long endMs = 0;
        MovieMediaItem prevMediaItem = null;
        final int mediaItemsCount = mMediaItems.size();
        for (int i = 0; i < mediaItemsCount; i++) {
            final MovieMediaItem mediaItem = mMediaItems.get(i);

            endMs = beginMs + mediaItem.getAppTimelineDuration();

            if (mediaItem.getEndTransition() != null) {
                if (i < mediaItemsCount - 1) {
                    endMs -= mediaItem.getEndTransition().getAppDuration();
                }
            }

            if (timeMs >= beginMs && timeMs <= endMs) {
                if (timeMs - beginMs < endMs - timeMs) { // Closer to the beginning
                    return prevMediaItem;
                } else { // Closer to the end
                    return mediaItem; // Insert after this item
                }
            }

            beginMs = endMs;
            prevMediaItem = mediaItem;
        }

        return null;
    }

    /**
     * @return true if media items have different aspect ratios
     */
    public boolean hasMultipleAspectRatios() {
        int aspectRatio = MediaProperties.ASPECT_RATIO_UNDEFINED;
        for (MovieMediaItem mediaItem : mMediaItems) {
            if (aspectRatio == MediaProperties.ASPECT_RATIO_UNDEFINED) {
                aspectRatio = mediaItem.getAspectRatio();
            } else if (mediaItem.getAspectRatio() != aspectRatio) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return The list of unique aspect ratios
     */
    public ArrayList<Integer> getUniqueAspectRatiosList() {
        final ArrayList<Integer> aspectRatiosList = new ArrayList<Integer>();
        for (MovieMediaItem mediaItem : mMediaItems) {
            int aspectRatio = mediaItem.getAspectRatio();
            if (!aspectRatiosList.contains(aspectRatio)) {
                aspectRatiosList.add(aspectRatio);
            }
        }

        return aspectRatiosList;
    }

    /**
     * Add a new transition
     *
     * @param transition The transition
     * @param afterMediaItemId Add the transition after this media item
     */
    void addTransition(MovieTransition transition, String afterMediaItemId) {
        final int count = mMediaItems.size();
        if (afterMediaItemId != null) {
            MovieMediaItem afterMediaItem = null;
            int afterMediaItemIndex = -1;
            for (int i = 0; i < count; i++) {
                final MovieMediaItem mediaItem = mMediaItems.get(i);
                if (mediaItem.getId().equals(afterMediaItemId)) {
                    afterMediaItem = mediaItem;
                    afterMediaItemIndex = i;
                    break;
                }
            }

            // Link the transition to the next and previous media items
            if (afterMediaItem == null) {
                throw new IllegalArgumentException("Media item not found: " + afterMediaItemId);
            }

            afterMediaItem.setEndTransition(transition);

            if (afterMediaItemIndex < count - 1) {
                final MovieMediaItem beforeMediaItem = mMediaItems.get(afterMediaItemIndex + 1);
                beforeMediaItem.setBeginTransition(transition);
            }
        } else {
            if (count == 0) {
                throw new IllegalArgumentException("Media item not found at the beginning");
            }

            final MovieMediaItem beforeMediaItem = mMediaItems.get(0);
            beforeMediaItem.setBeginTransition(transition);
        }
    }

    /**
     * Remove the specified transition
     *
     * @param transitionId The transition id
     */
    void removeTransition(String transitionId) {
        final int count = mMediaItems.size();
        for (int i = 0; i < count; i++) {
            final MovieMediaItem mediaItem = mMediaItems.get(i);
            final MovieTransition beginTransition = mediaItem.getBeginTransition();
            if (beginTransition != null && beginTransition.getId().equals(transitionId)) {
                mediaItem.setBeginTransition(null);
                break;
            }

            final MovieTransition endTransition = mediaItem.getEndTransition();
            if (endTransition != null && endTransition.getId().equals(transitionId)) {
                mediaItem.setEndTransition(null);
            }
        }
    }

    /**
     * Find the transition with the specified id
     *
     * @param transitionId The transition id
     * @return The transition
     */
    public MovieTransition getTransition(String transitionId) {
        final MovieMediaItem firstMediaItem = getFirstMediaItem();
        if (firstMediaItem == null) {
            return null;
        }

        final MovieTransition beginTransition = firstMediaItem.getBeginTransition();
        if (beginTransition != null && beginTransition.getId().equals(transitionId)) {
            return beginTransition;
        }

        for (MovieMediaItem mediaItem : mMediaItems) {
            final MovieTransition endTransition = mediaItem.getEndTransition();
            if (endTransition != null && endTransition.getId().equals(transitionId)) {
                return endTransition;
            }
        }

        return null;
    }

    /**
     * Add the overlay
     *
     * @param mediaItemId The media item id
     * @param overlay The overlay
     */
    void addOverlay(String mediaItemId, MovieOverlay overlay) {
        final MovieMediaItem mediaItem = getMediaItem(mediaItemId);
        mediaItem.addOverlay(overlay);
    }

    /**
     * Remove the specified overlay
     *
     * @param mediaItemId The media item id
     * @param overlayId The overlay id
     */
    void removeOverlay(String mediaItemId, String overlayId) {
        final MovieMediaItem mediaItem = getMediaItem(mediaItemId);
        mediaItem.removeOverlay(overlayId);
    }

    /**
     * Get the specified overlay
     *
     * @param mediaItemId The media item id
     * @param overlayId The overlay id
     * @return The movie overlay
     */
    public MovieOverlay getOverlay(String mediaItemId, String overlayId) {
        final MovieMediaItem mediaItem = getMediaItem(mediaItemId);
        return mediaItem.getOverlay();
    }

    /**
     * Add the effect
     *
     * @param mediaItemId The media item id
     * @param effect The effect
     */
    void addEffect(String mediaItemId, MovieEffect effect) {
        final MovieMediaItem mediaItem = getMediaItem(mediaItemId);
        mediaItem.addEffect(effect);
    }

    /**
     * Remove the specified effect
     *
     * @param mediaItemId The media item id
     * @param effectId The effect id
     */
    void removeEffect(String mediaItemId, String effectId) {
        final MovieMediaItem mediaItem = getMediaItem(mediaItemId);
        mediaItem.removeEffect(effectId);
    }

    /**
     * Get the specified effect
     *
     * @param mediaItemId The media item id
     * @param effectId The effect id
     * @return The movie effect
     */
    public MovieEffect getEffect(String mediaItemId, String effectId) {
        final MovieMediaItem mediaItem = getMediaItem(mediaItemId);
        return mediaItem.getEffect();
    }

    /**
     * Set the audio tracks
     *
     * @param audioTracks The audio tracks
     */
    void setAudioTracks(List<MovieAudioTrack> audioTracks) {
        mAudioTracks = audioTracks;
    }

    /**
     * Add an audio track
     *
     * @param audioTrack The audio track
     */
    void addAudioTrack(MovieAudioTrack audioTrack) {
        mAudioTracks.add(audioTrack);
    }

    /**
     * Remove the specified audio track
     *
     * @param audioTrackId The audio track id
     */
    void removeAudioTrack(String audioTrackId) {
        final int count = mAudioTracks.size();
        for (int i = 0; i < count; i++) {
            final MovieAudioTrack audioTrack = mAudioTracks.get(i);
            if (audioTrack.getId().equals(audioTrackId)) {
                mAudioTracks.remove(i);
                break;
            }
        }
    }

    /**
     * @return The audio tracks
     */
    public List<MovieAudioTrack> getAudioTracks() {
        return mAudioTracks;
    }

    /**
     * @param audioTrackId The audio track id
     * @return The audio track
     */
    public MovieAudioTrack getAudioTrack(String audioTrackId) {
        for (MovieAudioTrack audioTrack : mAudioTracks) {
            if (audioTrack.getId().equals(audioTrackId)) {
                return audioTrack;
            }
        }

        return null;
    }

    /**
     * Compute the begin time for this media item
     *
     * @param mediaItemId The media item id for which we compute the begin time
     *
     * @return The begin time for this media item
     */
    public long getMediaItemBeginTime(String mediaItemId) {
        long beginMs = 0;
        final int mediaItemsCount = mMediaItems.size();
        for (int i = 0; i < mediaItemsCount; i++) {
            final MovieMediaItem mi = mMediaItems.get(i);
            if (mi.getId().equals(mediaItemId)) {
                break;
            }

            beginMs += mi.getAppTimelineDuration();

            if (mi.getEndTransition() != null) {
                if (i < mediaItemsCount - 1) {
                    beginMs -= mi.getEndTransition().getAppDuration();
                }
            }
        }

        return beginMs;
    }

    /**
     * @return The total duration
     */
    public long computeDuration() {
        long totalDurationMs = 0;
        final int mediaItemsCount = mMediaItems.size();
        for (int i = 0; i < mediaItemsCount; i++) {
            final MovieMediaItem mediaItem = mMediaItems.get(i);
            totalDurationMs += mediaItem.getAppTimelineDuration();

            if (mediaItem.getEndTransition() != null) {
                if (i < mediaItemsCount - 1) {
                    totalDurationMs -= mediaItem.getEndTransition().getAppDuration();
                }
            }
        }

        return totalDurationMs;
    }

    /**
     * Render a frame according to the preview aspect ratio and activating all
     * storyboard items relative to the specified time.
     *
     * @param surfaceHolder SurfaceHolder used by the application
     * @param timeMs time corresponding to the frame to display
     * @param overlayData The overlay data
     *
     * @return The accurate time stamp of the frame that is rendered.
     * @throws IllegalStateException if a preview or an export is already in
     *             progress
     * @throws IllegalArgumentException if time is negative or beyond the
     *             preview duration
     */
    public long renderPreviewFrame(SurfaceHolder surfaceHolder, long timeMs,
            VideoEditor.OverlayData overlayData) {
        if (mVideoEditor != null) {
            return mVideoEditor.renderPreviewFrame(surfaceHolder, timeMs, overlayData);
        } else {
            return 0;
        }
    }

    /**
     * Render a frame of a media item.
     *
     * @param surfaceHolder SurfaceHolder used by the application
     * @param mediaItemId The media item id
     * @param timeMs time corresponding to the frame to display
     *
     * @return The accurate time stamp of the frame that is rendered .
     * @throws IllegalStateException if a preview or an export is already in
     *             progress
     * @throws IllegalArgumentException if time is negative or beyond the
     *             preview duration
     */
    public long renderMediaItemFrame(SurfaceHolder surfaceHolder, String mediaItemId,
            long timeMs) {
        if (mVideoEditor != null) {
            final MediaVideoItem mediaItem =
                (MediaVideoItem)mVideoEditor.getMediaItem(mediaItemId);
            if (mediaItem != null) {
                return mediaItem.renderFrame(surfaceHolder, timeMs);
            } else {
                return -1;
            }
        } else {
            return 0;
        }
    }

    /**
     * Start the preview of all the storyboard items applied on all MediaItems
     * This method does not block (does not wait for the preview to complete).
     * The PreviewProgressListener allows to track the progress at the time
     * interval determined by the callbackAfterFrameCount parameter. The
     * SurfaceHolder has to be created and ready for use before calling this
     * method. The method is a no-op if there are no MediaItems in the
     * storyboard.
     *
     * @param surfaceHolder SurfaceHolder where the preview is rendered.
     * @param fromMs The time (relative to the timeline) at which the preview
     *            will start
     * @param toMs The time (relative to the timeline) at which the preview will
     *            stop. Use -1 to play to the end of the timeline
     * @param loop true if the preview should be looped once it reaches the end
     * @param callbackAfterFrameCount The listener interface should be invoked
     *            after the number of frames specified by this parameter.
     * @param listener The listener which will be notified of the preview
     *            progress
     *
     * @throws IllegalArgumentException if fromMs is beyond the preview duration
     * @throws IllegalStateException if a preview or an export is already in
     *             progress
     */
    public void startPreview(SurfaceHolder surfaceHolder, long fromMs, long toMs, boolean loop,
            int callbackAfterFrameCount, PreviewProgressListener listener) {
        if (mVideoEditor != null) {
            mVideoEditor.startPreview(surfaceHolder, fromMs, toMs, loop, callbackAfterFrameCount,
                    listener);
        }
    }

    /**
     * Stop the current preview. This method blocks until ongoing preview is
     * stopped. Ignored if there is no preview running.
     *
     * @return The accurate current time when stop is effective expressed in
     *         milliseconds
     */
    public long stopPreview() {
        if (mVideoEditor != null) {
            return mVideoEditor.stopPreview();
        } else {
            return 0;
        }
    }

    /**
     * Clear the surface
     *
     * @param surfaceHolder SurfaceHolder where the preview is rendered.
     */
    public void clearSurface(SurfaceHolder surfaceHolder) {
        if (mVideoEditor != null) {
            mVideoEditor.clearSurface(surfaceHolder);
        }
    }

    /**
     * Release the project
     */
    public void release() {
    }

    /**
     * Load metadata from file
     *
     * @param videoEditor The video editor
     * @param projectPath The project path
     *
     * @return A new instance of the VideoEditorProject
     */
    public static VideoEditorProject fromXml(VideoEditor videoEditor, String projectPath)
            throws XmlPullParserException, FileNotFoundException, IOException {
        final File file = new File(projectPath, PROJECT_METADATA_FILENAME);
        final FileInputStream fis = new FileInputStream(file);
        try {
            // Load the metadata
            final XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, "UTF-8");
            int eventType = parser.getEventType();

            String projectName = null;
            String themeId = null;
            Uri exportedMovieUri = null;
            long lastSaved = 0;
            long playheadPosMs = 0;
            long durationMs = 0;
            int zoomLevel = DEFAULT_ZOOM_LEVEL;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String name = null;
                switch (eventType) {
                    case XmlPullParser.START_TAG: {
                        name = parser.getName();
                        if (name.equalsIgnoreCase(TAG_PROJECT)) {
                            projectName = parser.getAttributeValue("", ATTR_NAME);
                            themeId = parser.getAttributeValue("", ATTR_THEME);
                            lastSaved = Long.parseLong(parser.getAttributeValue("", ATTR_SAVED));
                            playheadPosMs = Long.parseLong(parser.getAttributeValue("",
                                    ATTR_PLAYHEAD_POSITION));
                            durationMs = Long.parseLong(parser.getAttributeValue("",
                                    ATTR_DURATION));
                            zoomLevel = Integer.parseInt(parser.getAttributeValue("",
                                    ATTR_ZOOM_LEVEL));
                        } else if (name.equalsIgnoreCase(TAG_MOVIE)) {
                            exportedMovieUri = Uri.parse(parser.getAttributeValue("", ATTR_URI));
                        }
                        break;
                    }

                    default: {
                        break;
                    }
                }
                eventType = parser.next();
            }

            return new VideoEditorProject(videoEditor, projectPath, projectName, lastSaved,
                    playheadPosMs, durationMs, zoomLevel, exportedMovieUri, themeId);
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    /**
     * Save the content to XML
     */
    public void saveToXml() throws IOException {
        // Save the project metadata
        final XmlSerializer serializer = Xml.newSerializer();
        final StringWriter writer = new StringWriter();
        serializer.setOutput(writer);
        serializer.startDocument("UTF-8", true);
        serializer.startTag("", TAG_PROJECT);
        if (mProjectName != null) {
            serializer.attribute("", ATTR_NAME, mProjectName);
        }
        if (mTheme != null) {
            serializer.attribute("", ATTR_THEME, mTheme);
        }

        serializer.attribute("", ATTR_PLAYHEAD_POSITION, Long.toString(mPlayheadPosMs));
        serializer.attribute("", ATTR_DURATION, Long.toString(computeDuration()));
        serializer.attribute("", ATTR_ZOOM_LEVEL, Integer.toString(mZoomLevel));

        mLastSaved = System.currentTimeMillis();
        serializer.attribute("", ATTR_SAVED, Long.toString(mLastSaved));
        if (mExportedMovieUri != null) {
            serializer.startTag("", TAG_MOVIE);
            serializer.attribute("", ATTR_URI, mExportedMovieUri.toString());
            serializer.endTag("", TAG_MOVIE);
        }

        serializer.endTag("", TAG_PROJECT);
        serializer.endDocument();

        // Save the metadata XML file
        final FileOutputStream out = new FileOutputStream(new File(mVideoEditor.getPath(),
                PROJECT_METADATA_FILENAME));
        out.write(writer.toString().getBytes("UTF-8"));
        out.flush();
        out.close();
    }
}
