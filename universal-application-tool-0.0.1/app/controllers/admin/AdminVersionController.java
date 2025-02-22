package controllers.admin;

import auth.Authorizers;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import views.admin.versions.VersionListView;

public class AdminVersionController extends Controller {
  private final VersionRepository versionRepository;
  private final VersionListView versionListView;

  @Inject
  public AdminVersionController(
      VersionRepository versionRepository, VersionListView versionListView) {
    this.versionRepository = versionRepository;
    this.versionListView = versionListView;
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result index(Http.Request request) {
    return ok(versionListView.render(versionRepository.listAllVersions(), request));
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result setVersionLive(long versionId, Http.Request request) {
    versionRepository.setLive(versionId);
    return redirect(routes.AdminVersionController.index());
  }
}
