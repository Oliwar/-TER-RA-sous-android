package com.example.ter_ra_android;

import com.google.ar.core.Camera;
import com.google.ar.core.TrackingFailureReason;

/** Gets human readibly tracking failure reasons and suggested actions. */
final class TrackingStateHelper {
  private static final String INSUFFICIENT_FEATURES_MESSAGE =
      "Je ne trouve rien. Visez le périphérique sur une surface avec plus de texture ou de couleur.";
  private static final String EXCESSIVE_MOTION_MESSAGE = "Ça bouge trop vite. Ralentissez.";
  private static final String INSUFFICIENT_LIGHT_MESSAGE =
      "Trop sombre. Essayez de vous déplacer dans un endroit bien éclairé.";
  private static final String BAD_STATE_MESSAGE =
      "Suivi perdu en raison d'un mauvais état interne. S'il vous plaît essayez de redémarrer l'expérience AR.";

  public static String getTrackingFailureReasonString(Camera camera) {
    TrackingFailureReason reason = camera.getTrackingFailureReason();
    switch (reason) {
      case NONE:
        return "";
      case BAD_STATE:
        return BAD_STATE_MESSAGE;
      case INSUFFICIENT_LIGHT:
        return INSUFFICIENT_LIGHT_MESSAGE;
      case EXCESSIVE_MOTION:
        return EXCESSIVE_MOTION_MESSAGE;
      case INSUFFICIENT_FEATURES:
        return INSUFFICIENT_FEATURES_MESSAGE;
    }
    return "Unknown tracking failure reason: " + reason;
  }
}
