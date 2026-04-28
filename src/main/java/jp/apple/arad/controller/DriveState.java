package jp.apple.arad.controller;

public enum DriveState {
    IDLE,
    /** ドア開までの待機 */
    STOP_WAIT_OPEN,
    /** ドア開 */
    DOOR_OPEN,
    /** 発車待ち */
    DOOR_CLOSE_WAIT,
    /** 走行中 */
    EN_ROUTE,

    BRAKING,

    TERMINAL
}
