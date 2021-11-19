package net.minecraft.sounds;

public class SoundTracks {
    private static final int ONE_SECOND = 20;
    private static final int THIRTY_SECONDS = 600;
    private static final int TEN_MINUTES = 12000;
    private static final int TWENTY_MINUTES = 24000;
    private static final int FIVE_MINUTES = 6000;
    public static final SoundTrack MENU = new SoundTrack(SoundEffects.MUSIC_MENU, 20, 600, true);
    public static final SoundTrack CREATIVE = new SoundTrack(SoundEffects.MUSIC_CREATIVE, 12000, 24000, false);
    public static final SoundTrack CREDITS = new SoundTrack(SoundEffects.MUSIC_CREDITS, 0, 0, true);
    public static final SoundTrack END_BOSS = new SoundTrack(SoundEffects.MUSIC_DRAGON, 0, 0, true);
    public static final SoundTrack END = new SoundTrack(SoundEffects.MUSIC_END, 6000, 24000, true);
    public static final SoundTrack UNDER_WATER = createGameMusic(SoundEffects.MUSIC_UNDER_WATER);
    public static final SoundTrack GAME = createGameMusic(SoundEffects.MUSIC_GAME);

    public static SoundTrack createGameMusic(SoundEffect event) {
        return new SoundTrack(event, 12000, 24000, false);
    }
}
