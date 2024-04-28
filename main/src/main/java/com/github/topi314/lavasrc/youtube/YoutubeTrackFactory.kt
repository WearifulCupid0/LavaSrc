package com.github.topi314.lavasrc.youtube

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import dev.lavalink.youtube.YoutubeAudioSourceManager as LavaYoutubeAudioSourceManager
import dev.lavalink.youtube.track.YoutubeAudioTrack as LavaYoutubeAudioTrack


interface YoutubeTrackFactory {
    fun createTrack(info: AudioTrackInfo): AudioTrack

    class LavaplayerYoutubeTrackFactory(private val youtubeAudioSourceManager: YoutubeAudioSourceManager) : YoutubeTrackFactory {
        override fun createTrack(info: AudioTrackInfo): AudioTrack {
            return YoutubeAudioTrack(info, this.youtubeAudioSourceManager)
        }
    }

    class LavaYoutubeSourceTrackFactory(private val youtubeAudioSourceManager: LavaYoutubeAudioSourceManager) : YoutubeTrackFactory {
        override fun createTrack(info: AudioTrackInfo): AudioTrack {
            return LavaYoutubeAudioTrack(info, this.youtubeAudioSourceManager)
        }
    }
}
