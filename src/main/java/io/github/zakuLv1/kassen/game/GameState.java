package io.github.zakuLv1.kassen.game;

/**
 * States of the KASSEN game lifecycle.
 */
public enum GameState {

    /** Waiting for players in the lobby. */
    LOBBY,

    /** Countdown before game starts. */
    STARTING,

    /** Game is actively being played. */
    PLAYING,

    /** A round has ended, showing results. */
    ROUND_END,

    /** The entire game has ended. */
    GAME_END
}
