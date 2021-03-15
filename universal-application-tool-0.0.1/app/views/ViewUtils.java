package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.link;
import static j2html.TagCreator.script;

import controllers.AssetsFinder;
import j2html.tags.Tag;
import javax.inject.Inject;

/** Utility class for accessing stateful view dependencies. */
public final class ViewUtils {
  private final AssetsFinder assetsFinder;

  public static final String POST = "post";

  @Inject
  ViewUtils(AssetsFinder assetsFinder) {
    this.assetsFinder = checkNotNull(assetsFinder);
  }

  /**
   * Generates an HTML script tag for loading the javascript file found at
   * public/javascripts/[filename].js.
   */
  Tag makeLocalJsTag(String filename) {
    return script()
        .withSrc(assetsFinder.path("javascripts/" + filename + ".js"))
        .withType("text/javascript");
  }

  /**
   * Generates an HTML link tag for loading the CSS file found at public/stylesheets/[filename].css.
   */
  Tag makeLocalCssTag(String filename) {
    return link()
        .withHref(assetsFinder.path("stylesheets/" + filename + ".css"))
        .withRel("stylesheet");
  }
}
