package views.admin;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.head;
import static j2html.TagCreator.header;
import static j2html.TagCreator.main;
import static views.StyleUtils.hover;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import javax.inject.Inject;
import play.twirl.api.Content;
import views.BaseHtmlLayout;
import views.Styles;
import views.ViewUtils;

public class AdminLayout extends BaseHtmlLayout {

  @Inject
  public AdminLayout(ViewUtils viewUtils) {
    super(viewUtils);
  }

  /** Renders mainDomContents within the main tag, in the context of the admin layout. */
  public Content render(DomContent... mainDomContents) {
    String questionLink = controllers.admin.routes.QuestionController.index("table").url();
    String programLink = controllers.admin.routes.AdminProgramController.index().url();
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();
    ContainerTag adminHeader =
        header()
            .with(navLink("Questions",questionLink))
            .with(navLink("Programs", programLink))
            .with(navLink("Logout", logoutLink))
            .withClasses(Styles.FLEX, Styles.INSET_X_0, Styles.TOP_0, Styles.ABSOLUTE, Styles.PY_4, Styles.BG_GRAY_700);
    return htmlContent(
            head(tailwindStyles()),
            body(adminHeader, main(mainDomContents).withClasses(Styles.PT_12, Styles.W_SCREEN))
              .withClasses(Styles.BG_GRAY_800, Styles.W_SCREEN, Styles.H_SCREEN, Styles.TEXT_GRAY_300));
  }

  private ContainerTag navLink(String a, String b) {
    return a(a).withHref(b).withClasses(Styles.ML_4, Styles.TRANSITION_ALL, Styles.TRANSFORM, hover(Styles.SCALE_105), hover(Styles.TEXT_GRAY_50));
  }
 }
